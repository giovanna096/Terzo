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
package io.openems.impl.controller.symmetric.refuworkstate;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.refu.RefuEss;

@IsThingMap(type = RefuEss.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setActivePower;
	public final WriteChannel<Long> setReactivePower;
	public final WriteChannel<Long> setWorkState;
	public final WriteChannel<Long> setSystemErrorReset;
	public final ReadChannel<Long> systemState;

	public Ess(RefuEss ess) {
		super(ess);
		setActivePower = ess.setActivePower().required();
		setReactivePower = ess.setReactivePower().required();
		setWorkState = ess.setWorkState().required();
		systemState = ess.systemState().required();
		setSystemErrorReset = ess.setSystemErrorReset.required();
	}

}
