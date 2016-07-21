/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.status;

import java.io.IOException;

import com.comino.flight.widgets.fx.controls.LEDControl;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;

public class StatusWidget extends Pane implements IMSPModeChangedListener {

	@FXML
	private LEDControl armed;

	@FXML
	private LEDControl connected;

	@FXML
	private LEDControl rcavailable;

	@FXML
	private LEDControl althold;

	@FXML
	private LEDControl poshold;

	@FXML
	private LEDControl mission;

	@FXML
	private LEDControl offboard;

	@FXML
	private LEDControl landed;

	@FXML
	private CheckBox details;

	@FXML
	private CheckBox tuning;

	@FXML
	private CheckBox video;

	@FXML
	private CheckBox messages;

	@FXML
	private CheckBox experimental;

	private IMAVController control;

	private DataModel model;

	public StatusWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	public BooleanProperty getDetailVisibility() {
		return details.selectedProperty();
	}

	public BooleanProperty getVideoVisibility() {
		return video.selectedProperty();
	}

	public BooleanProperty getExperimentalVisibility() {
		return experimental.selectedProperty();
	}

	public BooleanProperty getTuningVisibility() {
		return tuning.selectedProperty();
	}

	public BooleanProperty getMessageVisibility() {
		return messages.selectedProperty();
	}


	public void setup(IMAVController control) {

		this.model = control.getCurrentModel();
		this.control = control;
		this.control.addModeChangeListener(this);
		this.details.selectedProperty().set(false);
		this.messages.selectedProperty().set(true);
		update(model.sys,model.sys);
	}

	@Override
	public void update(Status arg0, Status newStat) {

		Platform.runLater(() -> {
			if(newStat.isStatus(Status.MSP_CONNECTED)) {
				details.selectedProperty().set(true);
				connected.setMode(LEDControl.MODE_ON);
			}
			else {
				connected.setMode(LEDControl.MODE_OFF);
				details.selectedProperty().set(false);
				tuning.selectedProperty().set(false);
			}

			if(newStat.isStatus(Status.MSP_ARMED) && newStat.isStatus(Status.MSP_CONNECTED))
				armed.setMode(LEDControl.MODE_ON);
			else
				armed.setMode(LEDControl.MODE_OFF);

			if((newStat.isStatus(Status.MSP_RC_ATTACHED) || newStat.isStatus(Status.MSP_JOY_ATTACHED))
					&& newStat.isStatus(Status.MSP_CONNECTED))
				rcavailable.setMode(LEDControl.MODE_ON);
			else
				rcavailable.setMode(LEDControl.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_ALTITUDE) && newStat.isStatus(Status.MSP_CONNECTED))
				althold.setMode(LEDControl.MODE_ON);
			else
				althold.setMode(LEDControl.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_POSITION) && newStat.isStatus(Status.MSP_CONNECTED))
				poshold.setMode(LEDControl.MODE_ON);
			else
				poshold.setMode(LEDControl.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_MISSION) && newStat.isStatus(Status.MSP_CONNECTED))
				mission.setMode(LEDControl.MODE_ON);
			else
				if(newStat.isStatus(Status.MSP_MODE_RTL) && newStat.isStatus(Status.MSP_CONNECTED))
					mission.setMode(LEDControl.MODE_BLINK);
				else
					mission.setMode(LEDControl.MODE_OFF);

			if(newStat.isStatus(Status.MSP_MODE_OFFBOARD) && newStat.isStatus(Status.MSP_CONNECTED))
				offboard.setMode(LEDControl.MODE_ON);
			else
				offboard.setMode(LEDControl.MODE_OFF);

			if(newStat.isStatus(Status.MSP_LANDED) && newStat.isStatus(Status.MSP_CONNECTED))
				landed.setMode(LEDControl.MODE_ON);
			else {
				if((newStat.isStatus(Status.MSP_MODE_LANDING) || newStat.isStatus(Status.MSP_MODE_TAKEOFF) ) && newStat.isStatus(Status.MSP_CONNECTED))
					landed.setMode(LEDControl.MODE_BLINK);
				else
					landed.setMode(LEDControl.MODE_OFF);
			}
		});
	}

}
