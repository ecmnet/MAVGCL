/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.ui.widgets.panel;

import com.comino.flight.FXMLLoadHelper;
import com.comino.jfx.extensions.DashLabelLED;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPStatusChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class StatusWidget extends WidgetPane implements IMSPStatusChangedListener {

	@FXML
	private DashLabelLED armed;

	@FXML
	private DashLabelLED althold;

	@FXML
	private DashLabelLED poshold;

	@FXML
	private DashLabelLED mission;

	@FXML
	private DashLabelLED offboard;

	@FXML
	private DashLabelLED landed;

	private DataModel model;

	public StatusWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "StatusWidget.fxml");
	}

	public void setup(IMAVController control) {
		super.setup(control);
		this.model = control.getCurrentModel();
		this.control.addStatusChangeListener(this);
		update(model.sys,model.sys);
	}

	@Override
	public void update(Status arg0, Status newStat) {

		Platform.runLater(() -> {

			if(!newStat.isStatus(Status.MSP_CONNECTED)) {
				armed.setMode(DashLabelLED.MODE_OFF);
				althold.setMode(DashLabelLED.MODE_OFF);
				poshold.setMode(DashLabelLED.MODE_OFF);
				mission.setMode(DashLabelLED.MODE_OFF);
				offboard.setMode(DashLabelLED.MODE_OFF);
				landed.setMode(DashLabelLED.MODE_OFF);
				return;
			}

			if(newStat.isStatus(Status.MSP_ARMED))
				armed.setMode(DashLabelLED.MODE_ON);
			else
				armed.setMode(DashLabelLED.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_ALTITUDE))
				althold.setMode(DashLabelLED.MODE_ON);
			else
				althold.setMode(DashLabelLED.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_POSITION))
				poshold.setMode(DashLabelLED.MODE_ON);
			else
				poshold.setMode(DashLabelLED.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_MISSION))
				mission.setMode(DashLabelLED.MODE_ON);
			else
				if(newStat.isStatus(Status.MSP_MODE_RTL))
					mission.setMode(DashLabelLED.MODE_BLINK);
				else
					mission.setMode(DashLabelLED.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_OFFBOARD))
				offboard.setMode(DashLabelLED.MODE_ON);
			else
				offboard.setMode(DashLabelLED.MODE_OFF);

			if(newStat.isStatus(Status.MSP_LANDED))
				landed.setMode(DashLabelLED.MODE_ON);
			else {
				if(newStat.isStatus(Status.MSP_MODE_LANDING) || newStat.isStatus(Status.MSP_MODE_TAKEOFF))
					landed.setMode(DashLabelLED.MODE_BLINK);
				else
					landed.setMode(DashLabelLED.MODE_OFF);
			}
		});
	}

}
