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
	private DashLabelLED automode;
	
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

		state.getConnectedProperty().addListener((v,o,n) -> refresh());

		control.getStatusManager().addListener(Status.MSP_ARMED, (n) -> {
			Platform.runLater(() -> {
				armed.set(n.isStatus(Status.MSP_ARMED));
				if(!n.isStatus(Status.MSP_ARMED)) {
					automode.set(false);
					landed.set(true);
				}

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
					automode.setMode(DashLabelLED.MODE_BLINK);
				else
					automode.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_TAKEOFF, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_PRECLAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					automode.setMode(DashLabelLED.MODE_ON);
				else
					automode.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_LOITER, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_PRECLAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					automode.setMode(DashLabelLED.MODE_ON);
				else
					automode.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_LAND,  (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_PRECLAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					automode.setMode(DashLabelLED.MODE_ON);
				else
					automode.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_PRECLAND,  (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_TAKEOFF)
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_PRECLAND && !n.isStatus(Status.MSP_LANDED))
					automode.setMode(DashLabelLED.MODE_BLINK);
				else if(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER)
					automode.setMode(DashLabelLED.MODE_ON);
				else
					automode.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});
		
		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_AUTO_MISSION, (n) -> {
			Platform.runLater(() -> {
				if(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION ) {
					mission.setMode(DashLabelLED.MODE_BLINK);
					automode.setMode(DashLabelLED.MODE_OFF);
				}
				else
					mission.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
			});
		});


		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE,Status.NAVIGATION_STATE_OFFBOARD, (n) -> {
			Platform.runLater(() -> {
				offboard.set(n.nav_state == Status.NAVIGATION_STATE_OFFBOARD);
			});
		});


		refresh();


	}

	private void refresh() {

		final Status status = control.getCurrentModel().sys;

		Platform.runLater(() ->  {
			armed.set(status.isStatus(Status.MSP_ARMED));

			landed.set(status.isStatus(Status.MSP_LANDED));
			althold.set(status.nav_state == Status.NAVIGATION_STATE_ALTCTL);
			poshold.set(status.nav_state == Status.NAVIGATION_STATE_POSCTL);
			offboard.set(status.nav_state == Status.NAVIGATION_STATE_OFFBOARD);

			if(status.nav_state == Status.NAVIGATION_STATE_AUTO_RTL && !status.isStatus(Status.MSP_LANDED))
				automode.setMode(DashLabelLED.MODE_BLINK);
			else
				automode.set(status.nav_state == Status.NAVIGATION_STATE_AUTO_MISSION);
		});	
	}

}
