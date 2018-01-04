package io.openems.backend.browserwebsocket;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.browserwebsocket.session.BackendCurrentDataWorker;
import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.browserwebsocket.session.BrowserSessionManager;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.openemswebsocket.OpenemsWebsocketSingleton;
import io.openems.backend.openemswebsocket.session.OpenemsSession;
import io.openems.backend.timedata.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.Device;
import io.openems.common.types.DeviceImpl;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections from a browser.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketSingleton
		extends AbstractWebsocketServer<BrowserSession, BrowserSessionData, BrowserSessionManager> {
	private final Logger log = LoggerFactory.getLogger(BrowserWebsocketSingleton.class);

	protected BrowserWebsocketSingleton(int port) throws OpenemsException {
		super(port, new BrowserSessionManager());
	}

	/**
	 * Open event of websocket. Parses the Odoo "session_id" and stores it in a new Session.
	 */
	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		// Prepare session information
		String error = "";
		BrowserSession session = null;
		Optional<String> sessionIdOpt = Optional.empty();

		try {
			// get cookie information
			JsonObject jCookie = parseCookieFromHandshake(handshake);
			sessionIdOpt = JsonUtils.getAsOptionalString(jCookie, "session_id");

			// try to get token of an existing, valid session from cookie
			if (jCookie.has("token")) {
				String token = JsonUtils.getAsString(jCookie, "token");
				Optional<BrowserSession> existingSessionOpt = sessionManager.getSessionByToken(token);
				if (existingSessionOpt.isPresent()) {
					BrowserSession existingSession = existingSessionOpt.get();
					// test if it is the same Odoo session_id
					if (sessionIdOpt.equals(existingSession.getData().getOdooSessionId())) {
						session = existingSession;
					}
				}
			}
		} catch (OpenemsException e) {
			error = e.getMessage();
		}

		if (session == null) {
			// create new session if no existing one was found
			BrowserSessionData sessionData = new BrowserSessionData();
			sessionData.setOdooSessionId(sessionIdOpt);
			session = sessionManager.createNewSession(sessionData);
		}

		// check Odoo session and refresh info from Odoo
		try {
			Metadata.instance().getInfoWithSession(session);
		} catch (OpenemsException e) {
			error = e.getMessage();
		}

		// check if the session is now valid and send reply to browser
		BrowserSessionData data = session.getData();
		if (error.isEmpty()) {
			// add isOnline information
			OpenemsWebsocketSingleton openemsWebsocket = OpenemsWebsocket.instance();
			for (DeviceImpl device : data.getDevices()) {
				device.setOnline(openemsWebsocket.isOpenemsWebsocketConnected(device.getName()));
			}

			// send connection successful to browser
			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(), Optional.empty(),
					data.getDevices());
			// TODO write user name to log output
			WebSocketUtils.send(websocket, jReply);

			// add websocket to local cache
			this.addWebsocket(websocket, session);

			log.info("User [" + data.getUserName() + "] connected with Session [" + data.getOdooSessionId().orElse("")
					+ "].");

		} else {
			// send connection failed to browser
			JsonObject jReply = DefaultMessages.browserConnectionFailedReply();
			WebSocketUtils.send(websocket, jReply);
			log.warn("User [" + data.getUserName() + "] connection failed. Session ["
					+ data.getOdooSessionId().orElse("") + "] Error [" + error + "].");

			websocket.closeConnection(CloseFrame.REFUSE, error);
		}
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		/*
		 * With existing device name
		 */
		if (deviceNameOpt.isPresent()) {
			String deviceName = deviceNameOpt.get();
			Optional<Integer> deviceIdOpt = Device.parseNumberFromName(deviceName);
			/*
			 * Query historic data
			 */
			if (jMessage.has("historicData")) {
				// parse deviceId
				JsonArray jMessageId = jMessageIdOpt.get();
				try {
					JsonObject jHistoricData = JsonUtils.getAsJsonObject(jMessage, "historicData");
					JsonObject jReply = WebSocketUtils.historicData(jMessageId, jHistoricData, deviceIdOpt,
							Timedata.instance(), Role.ADMIN);
					// TODO read role from device
					WebSocketUtils.send(websocket, jReply);
				} catch (OpenemsException e) {
					log.error(e.getMessage());
				}
			}

			// get session
			Optional<BrowserSession> sessionOpt = this.getSessionFromWebsocket(websocket);
			if (!sessionOpt.isPresent()) {
				log.warn("No BrowserSession available.");
				// throw new OpenemsException("No BrowserSession available.");
			}
			BrowserSession session = sessionOpt.get();

			/*
			 * Subscribe to currentData
			 */
			if (jMessage.has("currentData")) {
				JsonObject jCurrentData;
				try {
					jCurrentData = JsonUtils.getAsJsonObject(jMessage, "currentData");
					log.info("User [" + session.getData().getUserName() + "] subscribed to current data for device ["
							+ deviceName + "]: " + StringUtils.toShortString(jCurrentData, 50));
					JsonArray jMessageId = jMessageIdOpt.get();
					int deviceId = deviceIdOpt.get();
					this.currentData(session, websocket, jCurrentData, jMessageId, deviceName, deviceId);
				} catch (OpenemsException e) {
					log.error(e.getMessage());
				}
			}

			/*
			 * Serve "Config -> Query" from cache
			 */
			Optional<String> configModeOpt = Optional.empty();
			if (jMessage.has("config")) {
				Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
				if (jConfigOpt.isPresent()) {
					configModeOpt = JsonUtils.getAsOptionalString(jConfigOpt.get(), "mode");
					if (configModeOpt.isPresent() && configModeOpt.get().equals("query")) {
						/*
						 * Query current config
						 */
						Optional<OpenemsSession> openemsSessionOpt = OpenemsWebsocket.instance()
								.getOpenemsSession(deviceName);
						Optional<JsonObject> openemsConfig = Optional.empty();
						if (openemsSessionOpt.isPresent()) {
							openemsConfig = openemsSessionOpt.get().getData().getOpenemsConfigOpt();
						}
						if (!openemsConfig.isPresent()) {
							// set configMode to empty, so that the request is forwarded to Edge
							configModeOpt = Optional.empty();
						} else {
							log.info("User [" + session.getData().getUserName()
									+ "]: Sent OpenEMS-Config from cache for device [" + deviceName + "].");
							JsonObject jReply = DefaultMessages.configQueryReply(openemsConfig.get());
							if (deviceNameOpt.isPresent()) {
								jReply.addProperty("device", deviceNameOpt.get());
							}
							if (jMessageIdOpt.isPresent()) {
								jReply.add("id", jMessageIdOpt.get());
							}
							WebSocketUtils.send(websocket, jReply);
						}
					}
				}
			}

			/*
			 * Forward to OpenEMS Edge
			 */
			if ((jMessage.has("config") && !configModeOpt.orElse("").equals("query")) || jMessage.has("log")
					|| jMessage.has("system")) {
				try {
					forwardMessageToOpenems(session, websocket, jMessage, deviceName);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotification(websocket, new JsonArray(), LogBehaviour.WRITE_TO_LOG,
							Notification.EDGE_UNABLE_TO_FORWARD, deviceName, e.getMessage());
				}
			}
		}
	}

	@Override
	protected void _onClose(WebSocket websocket, Optional<BrowserSession> sessionOpt) {
		// nothing to do. Session is kept open.
	}

	/**
	 * Forward message to OpenEMS websocket.
	 *
	 * @throws OpenemsException
	 */
	private void forwardMessageToOpenems(BrowserSession session, WebSocket websocket, JsonObject jMessage,
			String deviceName) throws OpenemsException {
		// remove device from message
		if (jMessage.has("device")) {
			jMessage.remove("device");
		}

		// add session token to message id for identification
		JsonArray jId;
		if (jMessage.has("id")) {
			jId = JsonUtils.getAsJsonArray(jMessage, "id");
		} else {
			jId = new JsonArray();
		}
		jId.add(session.getToken());
		jMessage.add("id", jId);

		// add authentication role
		Role role = Role.GUEST;
		for (DeviceImpl device : session.getData().getDevices(deviceName)) {
			role = device.getRole();
		}
		jMessage.addProperty("role", role.name().toLowerCase());

		// get OpenEMS websocket and forward message
		Optional<WebSocket> openemsWebsocketOpt = OpenemsWebsocket.instance().getOpenemsWebsocket(deviceName);
		if (openemsWebsocketOpt.isPresent()) {
			WebSocket openemsWebsocket = openemsWebsocketOpt.get();
			if (WebSocketUtils.send(openemsWebsocket, jMessage)) {
				return;
			} else {
				throw new OpenemsException("Sending failed");
			}
		} else {
			throw new OpenemsException("Device is not connected.");
		}
	}

	/**
	 * Handle current data subscriptions
	 * (copied from EdgeWebsocketHandler. Try to keep synced...)
	 *
	 * @param j
	 */
	private synchronized void currentData(BrowserSession session, WebSocket websocket, JsonObject jCurrentData,
			JsonArray jId, String deviceName, int deviceId) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */

				// remove old worker if existed
				Optional<BackendCurrentDataWorker> workerOpt = session.getData().getCurrentDataWorkerOpt();
				if (workerOpt.isPresent()) {
					session.getData().setCurrentDataWorkerOpt(null);
					workerOpt.get().dispose();
				}

				// parse subscribed channels
				HashMultimap<String, String> channels = HashMultimap.create();
				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
				for (Entry<String, JsonElement> entry : jSubscribeChannels.entrySet()) {
					String thing = entry.getKey();
					JsonArray jChannels = JsonUtils.getAsJsonArray(entry.getValue());
					for (JsonElement jChannel : jChannels) {
						String channel = JsonUtils.getAsString(jChannel);
						channels.put(thing, channel);
					}
				}
				if (!channels.isEmpty()) {
					// create new worker
					BackendCurrentDataWorker worker = new BackendCurrentDataWorker(deviceId, deviceName, websocket, jId,
							channels);
					session.getData().setCurrentDataWorkerOpt(worker);
				}
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
	}

	// TODO notification handling
	// /**
	// * Generates a generic notification message
	// *
	// * @param message
	// * @return
	// */
	// private JsonObject generateNotification(String message) {
	// JsonObject j = new JsonObject();
	// JsonObject jNotification = new JsonObject();
	// jNotification.addProperty("message", message);
	// j.add("notification", jNotification);
	// return j;
	// }

	// TODO system command
	// /**
	// * System command
	// *
	// * @param j
	// */
	// private synchronized void system(String deviceName, JsonElement jSubscribeElement) {
	// JsonObject j = new JsonObject();
	// j.add("system", jSubscribeElement);
	// Optional<WebSocket> openemsWebsocketOpt = ConnectionManager.instance()
	// .getOpenemsWebsocketFromDeviceName(deviceName);
	// if (!openemsWebsocketOpt.isPresent()) {
	// log.warn("Trying to forward system call to [" + deviceName + "], but it is not online");
	// }
	// WebSocket openemsWebsocket = openemsWebsocketOpt.get();
	// log.info(deviceName + ": forward system call to OpenEMS " + StringUtils.toShortString(j, 100));
	// WebSocketUtils.send(openemsWebsocket, j);
	// }

	/**
	 * OpenEMS Websocket tells us, when the connection to an OpenEMS Edge is closed
	 *
	 * @param name
	 */
	public void openemsConnectionClosed(String name) {
		for (BrowserSession session : this.sessionManager.getSessions()) {
			for (DeviceImpl device : session.getData().getDevices()) {
				if (name.equals(device.getName())) {
					Optional<WebSocket> websocketOpt = this.getWebsocketFromSession(session);
					WebSocketUtils.sendNotification(websocketOpt, new JsonArray(), LogBehaviour.DO_NOT_WRITE_TO_LOG,
							Notification.EDGE_CONNECTION_ClOSED, name);
				}
			}
		}
	}

	/**
	 * OpenEMS Websocket tells us, when the connection to an OpenEMS Edge is openend
	 *
	 * @param name
	 */
	public void openemsConnectionOpened(Set<String> names) {
		for (BrowserSession session : this.sessionManager.getSessions()) {
			for (DeviceImpl device : session.getData().getDevices()) {
				for (String name : names) {
					if (name.equals(device.getName())) {
						// Optional<WebSocket> websocketOpt = this.getWebsocketFromSession(session);
						// TODO re-enable this once it is stable
						// WebSocketUtils.sendNotification(websocketOpt, new JsonArray(),
						// LogBehaviour.DO_NOT_WRITE_TO_LOG,
						// Notification.EDGE_CONNECTION_OPENED, name);
					}
				}
			}
		}
	}
}
