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
package io.openems.impl.controller.symmetric.balancingsurplus;

import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.AvgFiFoQueue;

@ThingInfo(title = "Self-consumption optimization with surplus feed-in (Symmetric)", description = "Tries to keep the grid meter on zero. For symmetric Ess. If ess is over the surplusMinSoc, the ess discharges with the power of the chargers. ")
public class BalancingSurplusController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingSurplusController() {
		super();
	}

	public BalancingSurplusController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Charger", description = "Sets the Chargers connected to the ess.", type = Charger.class, isArray = true)
	public final ConfigChannel<Set<Charger>> chargers = new ConfigChannel<Set<Charger>>("chargers", this);

	@ChannelInfo(title = "Surplus min soc", description = "The required Soc to start surplus feed-in.", type = Long.class)
	public final ConfigChannel<Long> surplusMinSoc = new ConfigChannel<Long>("surplusMinSoc", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	private AvgFiFoQueue meterActivePower = new AvgFiFoQueue(3, 1.5);
	private AvgFiFoQueue essActivePower = new AvgFiFoQueue(3, 1.5);

	private long surplus = 0L;
	private boolean surplusOn = false;

	/*
	 * Methods
	 */

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			meterActivePower.add(meter.value().activePower.value());
			essActivePower.add((ess.activePower.value() - surplus));
			// Calculate required sum values
			long calculatedPower = meterActivePower.avg() + essActivePower.avg();
			surplus = getSurplusPower();
			// in case the storage has surplus it isn't allowed to charge the storage ac
			if (calculatedPower < 0 && surplus > 0) {
				calculatedPower = 0;
			}
			if (getPvVoltage() < 200000 || surplus < 0) {
				surplus = 0l;
			}
			calculatedPower += surplus;
			ess.power.setActivePower(calculatedPower);
			ess.power.writePower();
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

	private long getSurplusPower() throws InvalidValueException {
		long power = 0l;
		if (ess.value().soc.value() >= surplusMinSoc.value() + 2) {
			surplusOn = true;
		} else if (ess.value().soc.value() < surplusMinSoc.value()) {
			surplusOn = false;
		}
		if (surplusOn) {
			for (Charger c : chargers.value()) {
				power += c.power.value();
			}
			long multiplier = ess.value().soc.value() - surplusMinSoc.value() - 2;
			if (multiplier > 0) {
				power += ess.value().nominalPower.value() * 0.25 / (100 - surplusMinSoc.value() - 2) * multiplier;
			}
		}
		return power;
	}

	private long getPvVoltage() throws InvalidValueException {
		long voltage = 0;
		for (Charger c : chargers.value()) {
			if (c.inputVoltage.value() > voltage) {
				voltage = c.inputVoltage.value();
			}
		}
		return voltage;
	}

}
