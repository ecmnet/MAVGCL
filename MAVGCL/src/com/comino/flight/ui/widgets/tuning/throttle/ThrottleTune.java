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

package com.comino.flight.ui.widgets.tuning.throttle;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_param_set;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.MAVGCLPX4Parameters;
import com.comino.flight.parameter.ParamUtils;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;


public class ThrottleTune extends VBox  {


	@FXML
	private HBox hbox;


	@FXML
	private Slider hover;

	@FXML
	private Slider minimal;

	private StateProperties state = null;
	private MAVGCLPX4Parameters parameters = null;


	public ThrottleTune() {

		FXMLLoadHelper.load(this, "ThrottleTune.fxml");

	}


	@FXML
	private void initialize() {

		state = StateProperties.getInstance();
		parameters = MAVGCLPX4Parameters.getInstance();

		hover.prefWidthProperty().bind(widthProperty().subtract(200));
		minimal.prefWidthProperty().bind(widthProperty().subtract(200));

		hover.disableProperty().bind(state.getParamLoadedProperty().not().or(state.getConnectedProperty().not()));
		//	minimal.disableProperty().bind(state.getParamLoadedProperty().not().or(state.getConnectedProperty().not()));
		minimal.setDisable(true);

	}

	public void setup(IMAVController control) {

		state.getParamLoadedProperty().addListener((a,o,n) -> {
			if(n.booleanValue()) {
				hover.setValue(parameters.get("MPC_THR_HOVER").value * 1000);
			}
		});

		hover.valueProperty().addListener((observable, oldvalue, newvalue) -> {

			float val = newvalue.intValue() / 1000f;

			if( Math.abs(parameters.get("MPC_THR_HOVER").value - val) > 0.005f) {

				parameters.get("MPC_THR_HOVER").value = val;

				final msg_param_set msg = new msg_param_set(255,1);
				msg.target_component = 1;
				msg.target_system = 1;
				msg.setParam_id("MPC_THR_HOVER");
				msg.param_value = val;
				control.sendMAVLinkMessage(msg);

			}

		});

		hover.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					hover.setValue(500);
				}
			}
		});

	}


}
