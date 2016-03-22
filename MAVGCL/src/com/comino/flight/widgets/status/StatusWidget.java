/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.widgets.status;

import java.io.IOException;

import com.comino.flight.control.FlightModeProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
