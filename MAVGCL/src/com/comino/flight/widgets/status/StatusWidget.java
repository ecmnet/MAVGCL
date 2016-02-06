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

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
        this.control.addModeChangeListener(this);
        update(model.sys,model.sys);
	}

	@Override
	public void update(Status arg0, Status newStat) {
		if(newStat.isStatus(Status.MSP_CONNECTED))
			connected.setFill(Color.LIGHTGREEN);
		else
			connected.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_ARMED) && newStat.isStatus(Status.MSP_CONNECTED))
			armed.setFill(Color.LIGHTGREEN);
		else
			armed.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_RC_ATTACHED) && newStat.isStatus(Status.MSP_CONNECTED))
			rcavailable.setFill(Color.LIGHTGREEN);
		else
			rcavailable.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_ALTITUDE) && newStat.isStatus(Status.MSP_CONNECTED))
			althold.setFill(Color.LIGHTGREEN);
		else
			althold.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_POSITION) && newStat.isStatus(Status.MSP_CONNECTED))
			poshold.setFill(Color.LIGHTGREEN);
		else
			poshold.setFill(Color.LIGHTGRAY);

	}

}
