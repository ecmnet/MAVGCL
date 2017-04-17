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

package com.comino.flight.widgets.control;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class ControlWidget extends WidgetPane  {

	@FXML
	private CheckBox details;

	@FXML
	private CheckBox tuning;

	@FXML
	private CheckBox video;

	@FXML
	private CheckBox vehiclectl;

	@FXML
	private CheckBox experimental;

	private StateProperties stateProperties = StateProperties.getInstance();

	public ControlWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "ControlWidget.fxml");
	}

	public BooleanProperty getDetailVisibility() {
		return details.selectedProperty();
	}

	public BooleanProperty getVideoVisibility() {
		return video.selectedProperty();
	}

	public BooleanProperty getVehicleCtlVisibility() {
		return vehiclectl.selectedProperty();
	}

	public BooleanProperty getExperimentalVisibility() {
		return experimental.selectedProperty();
	}

	public BooleanProperty getTuningVisibility() {
		return tuning.selectedProperty();
	}

	@FXML
	private void initialize() {
		tuning.setDisable(true);
		tuning.selectedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				video.setSelected(false);
				video.setDisable(true);
			}
			else
				video.setDisable(false);
		});

		vehiclectl.setDisable(true);
		stateProperties.getConnectedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				vehiclectl.setDisable(true);
				vehiclectl.setSelected(false);
				experimental.setDisable(true);
				experimental.setSelected(false);
			} else {
				vehiclectl.setDisable(false);
				experimental.setDisable(false);
			}
		});

		stateProperties.getLogLoadedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				experimental.setSelected(false);
				vehiclectl.setSelected(false);
				details.setSelected(false);
			} else {
				if(stateProperties.getConnectedProperty().get()) {
					details.setSelected(true);
				}
			}

		});

		stateProperties.getParamLoadedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				tuning.setDisable(true);
				tuning.setSelected(false);
			} else
				tuning.setDisable(false);
		});

		stateProperties.getConnectedProperty().addListener((e,o,n) -> {
			if(!n.booleanValue()) {
				video.setSelected(false);
			}
		});
	}

	public void setup(IMAVController control) {
		this.details.selectedProperty().set(false);
	}

}
