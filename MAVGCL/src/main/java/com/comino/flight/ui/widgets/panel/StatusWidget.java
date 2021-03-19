/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;

import com.comino.flight.FXMLLoadHelper;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.jfx.extensions.DashLabelLED;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.status.StatusManager;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class StatusWidget extends ChartControlPane  {

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

	public StatusWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "StatusWidget.fxml");
	}

	public void setup(IMAVController control) {
		super.setup(control);

		this.disableProperty().bind(state.getConnectedProperty().not());

		control.getStatusManager().addListener(Status.MSP_CONNECTED, (n) -> {
			if(!n.isStatus(Status.MSP_CONNECTED)) {
				Platform.runLater(() -> {
					armed.setMode(DashLabelLED.MODE_OFF);
					althold.setMode(DashLabelLED.MODE_OFF);
					poshold.setMode(DashLabelLED.MODE_OFF);
					mission.setMode(DashLabelLED.MODE_OFF);
					offboard.setMode(DashLabelLED.MODE_OFF);
					landed.setMode(DashLabelLED.MODE_OFF);
				});
			}
		});

		control.getStatusManager().addListener(Status.MSP_ARMED, (n) -> {
			Platform.runLater(() -> {
				armed.set(n.isStatus(Status.MSP_ARMED));
				if(!n.isStatus(Status.MSP_ARMED))
					mission.set(false);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_ALTCTL, (n) -> {
			Platform.runLater(() -> {
				althold.set(n.nav_state == Status.NAVIGATION_STATE_ALTCTL);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_POSCTL,  (n) -> {
			Platform.runLater(() -> {
				poshold.set(n.nav_state == Status.NAVIGATION_STATE_POSCTL);
			});
		});

		control.getStatusManager().addListener(Status.MSP_LANDED, (n) -> {
			Platform.runLater(() -> {
				landed.set(n.isStatus(Status.MSP_LANDED));
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_RTL, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_RTL && !n.isStatus(Status.MSP_LANDED))
					mission.setMode(DashLabelLED.MODE_BLINK);
				else
					mission.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_TAKEOFF, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					mission.setMode(DashLabelLED.MODE_ON);
				else
					mission.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_LOITER, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					mission.setMode(DashLabelLED.MODE_ON);
				else
					mission.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_LAND,  (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					mission.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					mission.setMode(DashLabelLED.MODE_ON);
				else
					mission.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});


		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_OFFBOARD, (n) -> {
			Platform.runLater(() -> {
				offboard.set(n.nav_state == Status.NAVIGATION_STATE_OFFBOARD);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT,MSP_AUTOCONTROL_ACTION.WAYPOINT_MODE, (n) -> {
			Platform.runLater(() -> {
				if(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.WAYPOINT_MODE))
					offboard.setMode(DashLabelLED.MODE_BLINK);
				else
					offboard.set(n.nav_state == Status.NAVIGATION_STATE_OFFBOARD);
			});
		});



	}

}
