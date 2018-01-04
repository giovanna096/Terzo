/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.asymmetric.avoidtotaldischarge;

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.asymmetric.avoidtotaldischarge.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Asymmetric)", description = "Makes sure the battery is not going into critically low state of charge. For asymmetric Ess.")
public class AvoidTotalDischargeController extends Controller {

	/*
	 * Constructors
	 */
	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String id) {
		super(id);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);
	@ChannelInfo(title = "Max Soc", description = "If the System is full the charge is blocked untill the soc decrease below the maxSoc.", type = Long.class, defaultValue = "95")
	public final ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				switch (ess.currentState) {
				case CHARGESOC:
					if (ess.soc.value() > ess.minSoc.value()) {
						ess.currentState = State.MINSOC;
					} else {
						try {
							Optional<Long> currentMinValueL1 = ess.setActivePowerL1.writeMin();
							if (currentMinValueL1.isPresent() && currentMinValueL1.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL1=Max[" + currentMinValueL1.get() / 5 + "]");
								ess.setActivePowerL1.pushWriteMax(currentMinValueL1.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL1=Max[-1000 W]");
								ess.setActivePowerL1.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL1: " + e.getMessage());
						}
						try {
							Optional<Long> currentMinValueL2 = ess.setActivePowerL2.writeMin();
							if (currentMinValueL2.isPresent() && currentMinValueL2.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL2=Max[" + currentMinValueL2.get() / 5 + "]");
								ess.setActivePowerL2.pushWriteMax(currentMinValueL2.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL2=Max[-1000 W]");
								ess.setActivePowerL2.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL2: " + e.getMessage());
						}
						try {
							Optional<Long> currentMinValueL3 = ess.setActivePowerL3.writeMin();
							if (currentMinValueL3.isPresent() && currentMinValueL3.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL3=Max[" + currentMinValueL3.get() / 5 + "]");
								ess.setActivePowerL3.pushWriteMax(currentMinValueL3.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL3=Max[-1000 W]");
								ess.setActivePowerL3.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL3: " + e.getMessage());
						}
					}
					break;
				case MINSOC:
					if (ess.soc.value() < ess.chargeSoc.value()) {
						ess.currentState = State.CHARGESOC;
					} else if (ess.soc.value() >= ess.minSoc.value() + 5) {
						ess.currentState = State.NORMAL;
					} else {
						try {
							long maxPower = 0;
							if (!ess.setActivePowerL1.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL1.writeMax().get()) {
								ess.setActivePowerL1.pushWriteMax(maxPower);
							}
							if (!ess.setActivePowerL2.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL2.writeMax().get()) {
								ess.setActivePowerL2.pushWriteMax(maxPower);
							}
							if (!ess.setActivePowerL3.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL3.writeMax().get()) {
								ess.setActivePowerL3.pushWriteMax(maxPower);
							}
						} catch (WriteChannelException e) {
							log.error(ess.id() + "Failed to set Max allowed power.", e);
						}
					}
					break;
				case NORMAL:
					if (ess.soc.value() <= ess.minSoc.value()) {
						ess.currentState = State.MINSOC;
					} else if (ess.soc.value() >= 99 && ess.allowedCharge.value() == 0
							&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
						ess.currentState = State.FULL;
					}
					break;
				case FULL:
					try {
						ess.setActivePowerL1.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL1: " + e.getMessage());
					}
					try {
						ess.setActivePowerL2.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL2: " + e.getMessage());
					}
					try {
						ess.setActivePowerL3.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL3: " + e.getMessage());
					}
					if (ess.soc.value() < maxSoc.value()) {
						ess.currentState = State.NORMAL;
					}
					break;
				}
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
