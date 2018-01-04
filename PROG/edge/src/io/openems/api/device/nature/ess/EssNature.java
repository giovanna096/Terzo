/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems.api.device.nature.ess;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;

public interface EssNature extends DeviceNature {
	/*
	 * Constants
	 */
	public final int DEFAULT_MINSOC = 10;

	public final String OFF = "Off";
	public final String ON = "On";

	public final String OFF_GRID = "Off-Grid";
	public final String ON_GRID = "On-Grid";

	public final String STANDBY = "Standby";
	public final String START = "Start";
	public final String STOP = "Stop";
	public final String FAULT = "Fault";

	/*
	 * Config
	 */
	@ChannelInfo(title = "Min-SOC", description = "Sets the minimal SOC.", type = Integer.class)
	public ConfigChannel<Integer> minSoc();

	@ChannelInfo(title = "Charge-SOC", description = "Sets the force charge SOC.", type = Integer.class, isOptional = true)
	public ConfigChannel<Integer> chargeSoc();

	/*
	 * Read Channels
	 */
	public ReadChannel<Long> gridMode();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> soc();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> systemState();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> allowedCharge();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> allowedDischarge();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> allowedApparent();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> capacity();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> maxNominalPower();

	public StatusBitChannels warning();

	/*
	 * Write Channels
	 */
	public WriteChannel<Long> setWorkState();
}
