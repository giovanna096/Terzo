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
package io.openems.impl.controller.testwrite;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Test write")
public class TestWriteController extends Controller {

	/*
	 * Config
	 */
	@ChannelInfo(title = "Output", type = Output.class)
	public ConfigChannel<Output> out = new ConfigChannel<>("out", this);

	@ChannelInfo(title = "Input", type = Input.class)
	public ConfigChannel<Input> in = new ConfigChannel<>("in", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			out.value().output1.pushWrite(true);
			log.info(in.value().input1.value().toString());
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// for (Ess ess : esss) {
		// try {
		// ess.setActivePower.pushWrite(500L);
		// ess.setWorkState.pushWriteFromLabel(SymmetricEssNature.START);
		// } catch (WriteChannelException e) {
		// log.error("", e);
		// }
		// }
	}

}
