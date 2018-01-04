package io.openems.backend.openemswebsocket;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.browserwebsocket.BrowserWebsocket;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.backend.openemswebsocket.session.OpenemsSession;
import io.openems.backend.openemswebsocket.session.OpenemsSessionData;
import io.openems.backend.openemswebsocket.session.OpenemsSessionManager;
import io.openems.backend.timedata.Timedata;
import io.openems.backend.utilities.StringUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections to OpenEMS-Devices.
 *
 * @author stefan.feilmeier
 *
 */
public class OpenemsWebsocketSingleton
		extends AbstractWebsocketServer<OpenemsSession, OpenemsSessionData, OpenemsSessionManager> {
	private final Logger log = LoggerFactory.getLogger(OpenemsWebsocketSingleton.class);

	protected OpenemsWebsocketSingleton(int port) {
		super(port, new OpenemsSessionManager());
	}

	/**
	 * Open event of websocket. Parses the "apikey" and stores it in a new Session.
	 */
	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String apikey = "";
		Set<String> deviceNames = new HashSet<>();
		try {

			// get apikey from handshake
			Optional<String> apikeyOpt = parseApikeyFromHandshake(handshake);
			if (!apikeyOpt.isPresent()) {
				throw new OpenemsException("Apikey is missing in handshake");
			}
			apikey = apikeyOpt.get();

			// if existing: close existing websocket for this apikey
			Optional<OpenemsSession> oldSessionOpt = this.sessionManager.getSessionByToken(apikey);
			if (oldSessionOpt.isPresent()) {
				OpenemsSession oldSession = oldSessionOpt.get();
				WebSocket oldWebsocket = oldSession.getData().getWebsocket();
				oldWebsocket.closeConnection(CloseFrame.REFUSE,
						"Another device with this apikey [" + apikey + "] connected.");
				this.sessionManager.removeSession(oldSession);
			}

			// get device for apikey
			MetadataDevices devices = Metadata.instance().getDeviceModel().getDevicesForApikey(apikey);
			if (devices.isEmpty()) {
				throw new OpenemsException("Unable to find device for apikey [" + apikey + "]");
			}
			deviceNames = devices.getNames();

			// create new session
			OpenemsSessionData sessionData = new OpenemsSessionData(websocket, devices);
			OpenemsSession session = sessionManager.createNewSession(apikey, sessionData);

			// send successful reply to openems
			JsonObject jReply = DefaultMessages.openemsConnectionSuccessfulReply();
			WebSocketUtils.send(websocket, jReply);
			// add websocket to local cache
			this.addWebsocket(websocket, session);

			log.info("Device [" + String.join(",", deviceNames) + "] connected.");

			try {
				// set device active (in Odoo)
				for (MetadataDevice device : devices) {
					if (device.getState().equals("inactive")) {
						device.setState("active");
					}
					device.setLastMessage();
					device.writeObject();
				}
			} catch (OpenemsException e) {
				// this error does not stop the connection
				log.error("Device [" + String.join(",", deviceNames) + "] error: " + e.getMessage());
			}

			// announce browserWebsocket that this OpenEMS Edge was connected
			BrowserWebsocket.instance().openemsConnectionOpened(deviceNames);

		} catch (OpenemsException e) {
			// send connection failed to OpenEMS
			JsonObject jReply = DefaultMessages.openemsConnectionFailedReply(e.getMessage());
			WebSocketUtils.send(websocket, jReply);
			// close websocket
			websocket.closeConnection(CloseFrame.REFUSE, "OpenEMS connection failed. Device ["
					+ String.join(",", deviceNames) + "] Apikey [" + apikey + "]");
		}
	}

	/**
	 * Close event of websocket. Removes the session.
	 */
	@Override
	public void _onClose(WebSocket websocket, Optional<OpenemsSession> sessionOpt) {
		if (sessionOpt.isPresent()) {
			// log.info("Would remove the session... " + sessionOpt.get());
			sessionManager.removeSession(sessionOpt.get());
		}
	}

	/**
	 * Message event of websocket. Handles a new message. At this point the device is already authenticated.
	 */
	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		OpenemsSessionData sessionData = this.getSessionFromWebsocket(websocket).get().getData();
		MetadataDevices devices = sessionData.getDevices();

		// if (!jMessage.has("timedata") && !jMessage.has("currentData") && !jMessage.has("log")
		// && !jMessage.has("config")) {
		// log.info("Received from " + device.getName() + ": " + jMessage.toString());
		// }

		/*
		 * Config? -> store in Metadata
		 */
		if (jMessage.has("config")) {
			try {
				JsonObject jConfig = JsonUtils.getAsJsonObject(jMessage, "config");
				for (MetadataDevice device : devices) {
					device.setOpenemsConfig(jConfig);
				}
				sessionData.setOpenemsConfig(jConfig);
				log.info("Device [" + devices.getNamesString() + "] sent config.");
			} catch (OpenemsException e) {
				log.error(e.getMessage());
			}
		}

		/*
		 * Is this a reply? -> forward to Browser
		 */
		if (jMessage.has("id")) {
			for (String deviceName : devices.getNames()) {
				forwardReplyToBrowser(websocket, deviceName, jMessage);
			}
		}

		/*
		 * New timestamped data
		 */
		if (jMessage.has("timedata")) {
			timedata(devices, jMessage.get("timedata"));
		}

		// Save data to Odoo
		try {
			for (MetadataDevice device : devices) {
				device.writeObject();
			}
		} catch (OpenemsException e) {
			log.error("Device [" + devices.getNamesString() + "] error: " + e.getMessage());
		}
	}

	private void forwardReplyToBrowser(WebSocket openemsWebsocket, String deviceName, JsonObject jMessage) {
		try {
			// get browser websocket
			JsonArray jId = JsonUtils.getAsJsonArray(jMessage, "id");
			String token = JsonUtils.getAsString(jId.get(jId.size() - 1));
			Optional<WebSocket> browserWebsocketOpt = BrowserWebsocket.instance().getWebsocketByToken(token);
			if (!browserWebsocketOpt.isPresent()) {
				log.warn("Device [" + deviceName + "] Browser websocket is not connected. Message ["
						+ StringUtils.toShortString(jMessage, 100) + "]");
				if (jMessage.has("currentData")) {
					// unsubscribe obsolete browser websocket
					WebSocketUtils.send(openemsWebsocket, DefaultMessages.currentDataSubscribe(jId, new JsonObject()));
				}
				if (jMessage.has("log")) {
					// unsubscribe obsolete browser websocket
					WebSocketUtils.send(openemsWebsocket, DefaultMessages.logUnsubscribe(jId));
				}
				return;
			}
			WebSocket browserWebsocket = browserWebsocketOpt.get();

			// remove token from message id
			jId.remove(jId.size() - 1);
			jMessage.add("id", jId);
			// always add device name
			jMessage.addProperty("device", deviceName);

			// send
			WebSocketUtils.send(browserWebsocket, jMessage);
		} catch (OpenemsException e) {
			log.error("Device [" + deviceName + "] error: " + e.getMessage());
		}
	}

	private void timedata(MetadataDevices devices, JsonElement jTimedataElement) {
		try {
			JsonObject jTimedata = JsonUtils.getAsJsonObject(jTimedataElement);
			// Write to InfluxDB
			try {
				Timedata.instance().write(devices, jTimedata);
				log.debug(devices.getNamesString() + ": wrote " + jTimedata.entrySet().size() + " timestamps "
						+ StringUtils.toShortString(jTimedata, 120));
			} catch (Exception e) {
				log.error("Unable to write Timedata: ", e);
			}
			// Set Odoo last message timestamp
			for (MetadataDevice device : devices) {
				device.setLastMessage();
			}

			for (Entry<String, JsonElement> jTimedataEntry : jTimedata.entrySet()) {
				try {
					JsonObject jChannels = JsonUtils.getAsJsonObject(jTimedataEntry.getValue());

					// set Odoo last update timestamp only for those channels
					for (String channel : jChannels.keySet()) {
						if (channel.endsWith("ActivePower")
								|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
										| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
							for (MetadataDevice device : devices) {
								device.setLastUpdate();
							}
						}
					}

					// set specific Odoo values
					if (jChannels.has("ess0/Soc")) {
						int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
						for (MetadataDevice device : devices) {
							device.setSoc(soc);
						}
					}
					if (jChannels.has("system0/PrimaryIpAddress")) {
						String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
						for (MetadataDevice device : devices) {
							device.setIpV4(ipv4);
						}
					}
				} catch (OpenemsException e) {
					log.error("Device [" + String.join(",", devices.getNames()) + "] error: " + e.getMessage());
				}
			}

		} catch (OpenemsException e) {
			log.error("Device [" + devices.getNamesString() + "] error: " + e.getMessage());
		}
	}

	/**
	 * Parses the apikey from websocket onOpen handshake
	 *
	 * @param handshake
	 * @return
	 */
	private Optional<String> parseApikeyFromHandshake(ClientHandshake handshake) {
		if (handshake.hasFieldValue("apikey")) {
			String apikey = handshake.getFieldValue("apikey");
			return Optional.ofNullable(apikey);
		}
		return Optional.empty();
	}

	/**
	 * Returns true if this device is currently connected
	 *
	 * @param name
	 * @return
	 */
	public Optional<OpenemsSession> getOpenemsSession(String deviceName) {
		return this.sessionManager.getSessionByDeviceName(deviceName);
	}

	/**
	 * Returns true if this device is currently connected
	 *
	 * @param name
	 * @return
	 */
	public boolean isOpenemsWebsocketConnected(String deviceName) {
		Optional<OpenemsSession> sessionOpt = getOpenemsSession(deviceName);
		if (sessionOpt.isPresent()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the OpenemsWebsocket for the given device
	 *
	 * @param name
	 * @return
	 */
	public Optional<WebSocket> getOpenemsWebsocket(String deviceName) {
		Optional<OpenemsSession> sessionOpt = this.sessionManager.getSessionByDeviceName(deviceName);
		if (!sessionOpt.isPresent()) {
			return Optional.empty();
		}
		OpenemsSession session = sessionOpt.get();
		return this.getWebsocketFromSession(session);
	}

	public Collection<OpenemsSession> getSessions() {
		return Collections.synchronizedCollection(this.sessionManager.getSessions());
	}
}