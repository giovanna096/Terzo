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
package io.openems.impl.controller.symmetric.powerbyfrequency;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo(title = "Power by frequency (Symmetric)", description = "Tries to keep the grid meter at a given frequency. For symmetric Ess.")
public class PowerByFrequencyController extends Controller {

	/*
	 * Constructors
	 */
	public PowerByFrequencyController() {
		super();
	}

	public PowerByFrequencyController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ChannelInfo(title = "Meter", description = "The meter for the frequency meassurement.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<>("meter", this);

	@ChannelInfo(title = "Low SOC-Limit", description = "The low soc limit. Below this limit the Ess will charge with more power by the same frequency.", type = Integer.class, defaultValue = "30")
	public final ConfigChannel<Integer> lowSocLimit = new ConfigChannel<Integer>("lowSocLimit", this);

	@ChannelInfo(title = "High SOC-Limit", description = "The upper soc limit. Above this limit the Ess will discharge with more power by the same frequency.", type = Integer.class, defaultValue = "70")
	public final ConfigChannel<Integer> highSocLimit = new ConfigChannel<Integer>("highSocLimit", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			// Calculate required sum values
			long activePower = 0L;
			if (meter.frequency.value() >= 49990 && meter.frequency.value() <= 50010) {
				// charge if SOC isn't in the expected range
				if ((ess.soc.value() > highSocLimit.value() && meter.frequency.value() < 50000)
						|| (ess.soc.value() < lowSocLimit.value() && meter.frequency.value() > 50000)) {
					activePower = (long) (ess.maxNominalPower.value() * (300.0 - 0.006 * meter.frequency.value()));
				}
			} else {
				// calculate minimal Power for Frequency
				activePower = (long) ((double) ess.maxNominalPower.value() * (250.0 - meter.frequency.value() / 200.0));
				if ((meter.frequency.value() < 50000 && ess.soc.value() > highSocLimit.value())
						|| (meter.frequency.value() > 50000 && ess.soc.value() < lowSocLimit.value())) {
					// calculate maximal Power for frequency
					activePower = (long) (ess.maxNominalPower.value() * (300 - 0.006 * meter.frequency.value()));
				}
			}
			ess.power.setActivePower(activePower);
			ess.power.writePower();
			log.info(ess.id() + " Set ActivePower [" + ess.power.getActivePower() + "]");
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
