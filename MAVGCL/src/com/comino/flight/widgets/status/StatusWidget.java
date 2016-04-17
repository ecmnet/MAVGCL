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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class StatusWidget extends Pane implements IMSPModeChangedListener {

	@FXML
	private Circle armed;

	@FXML
	private Circle connected;

	@FXML
	private Circle rcavailable;

	@FXML
	private Circle althold;

	@FXML
	private Circle poshold;

	@FXML
	private Circle mission;

	@FXML
	private Circle offboard;

	@FXML
	private Circle landed;

	@FXML
	private CheckBox details;

	@FXML
	private CheckBox video;

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

	public BooleanProperty getDetailsProperty() {
		return details.selectedProperty();
	}

	public BooleanProperty getVideoProperty() {
		return video.selectedProperty();
	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		this.control.addModeChangeListener(this);
		this.details.selectedProperty().set(true);
		update(model.sys,model.sys);
	}

	@Override
	public void update(Status arg0, Status newStat) {


		if(newStat.isStatus(Status.MSP_CONNECTED))
			connected.setFill(Color.DARKORANGE);
		else {
			connected.setFill(Color.LIGHTGRAY);
		}

		if(newStat.isStatus(Status.MSP_ARMED) && newStat.isStatus(Status.MSP_CONNECTED))
			armed.setFill(Color.DARKORANGE);
		else
			armed.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_RC_ATTACHED) && newStat.isStatus(Status.MSP_CONNECTED))
			rcavailable.setFill(Color.DARKORANGE);
		else
			rcavailable.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_ALTITUDE) && newStat.isStatus(Status.MSP_CONNECTED))
			althold.setFill(Color.GREEN);
		else
			althold.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_POSITION) && newStat.isStatus(Status.MSP_CONNECTED))
			poshold.setFill(Color.GREEN);
		else
			poshold.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_MISSION) && newStat.isStatus(Status.MSP_CONNECTED))
			mission.setFill(Color.GREEN);
		else
			mission.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_OFFBOARD) && newStat.isStatus(Status.MSP_CONNECTED))
			offboard.setFill(Color.GREEN);
		else
			offboard.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_LANDED) && newStat.isStatus(Status.MSP_CONNECTED))
			landed.setFill(Color.GREEN);
		else
			landed.setFill(Color.LIGHTGRAY);

	}

}
