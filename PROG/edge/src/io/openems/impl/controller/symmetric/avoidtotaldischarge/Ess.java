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
package io.openems.impl.controller.symmetric.avoidtotaldischarge;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.core.utilities.hysteresis.Hysteresis;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final ReadChannel<Integer> minSoc;
	public final WriteChannel<Long> setActivePower;
	public final ReadChannel<Long> soc;
	public final ReadChannel<Long> systemState;
	public int maxPowerPercent = 100;
	public final ReadChannel<Long> allowedDischarge;
	public final ReadChannel<Long> allowedCharge;
	public final ReadChannel<Integer> chargeSoc;
	public Hysteresis socMinHysteresis;
	public State currentState = State.NORMAL;

	public enum State {
		NORMAL, MINSOC, CHARGESOC, FULL;
	}

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower().required();
		systemState = ess.systemState().required();
		soc = ess.soc().required();
		minSoc = ess.minSoc().required();
		allowedDischarge = ess.allowedDischarge().required();
		allowedCharge = ess.allowedCharge().required();
		chargeSoc = ess.chargeSoc().required();
		ChannelChangeListener hysteresisCreator = new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				if (minSoc.valueOptional().isPresent() && chargeSoc.valueOptional().isPresent()) {
					socMinHysteresis = new Hysteresis(chargeSoc.valueOptional().get(), minSoc.valueOptional().get());
				} else if (minSoc.valueOptional().isPresent()) {
					socMinHysteresis = new Hysteresis(minSoc.valueOptional().get() - 3, minSoc.valueOptional().get());
				}
			}
		};
		minSoc.addChangeListener(hysteresisCreator);
		chargeSoc.addChangeListener(hysteresisCreator);

		hysteresisCreator.channelChanged(null, null, null);
	}
}
