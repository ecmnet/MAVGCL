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

import org.mavlink.messages.lquac.msg_param_set;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.param.ParameterAttributes;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class ThrottleTune extends VBox  {


	@FXML
	private HBox hbox;


	@FXML
	private Slider hover;


	@FXML
	private Slider mdl;

	private StateProperties state = null;
	private MAVGCLPX4Parameters parameters = null;


	private IMAVController control;


	public ThrottleTune() {

		FXMLLoadHelper.load(this, "ThrottleTune.fxml");

	}


	@FXML
	private void initialize() {

		state = StateProperties.getInstance();
		parameters = MAVGCLPX4Parameters.getInstance();

		hover.prefWidthProperty().bind(widthProperty().subtract(450));
		mdl.prefWidthProperty().bind(widthProperty().subtract(450));

//		hover.disableProperty().bind(state.getParamLoadedProperty().not().or(state.getConnectedProperty().not()
//				.or(state.getLogLoadedProperty())));
//		mdl.disableProperty().bind(state.getParamLoadedProperty().not().or(state.getConnectedProperty().not()
//				.or(state.getLogLoadedProperty())));

		hover.setDisable(true);
		mdl.setDisable(true);


	}

	public void setup(IMAVController control) {

		this.control = control;

		state.getParamLoadedProperty().addListener((a,o,n) -> {
			if(n.booleanValue() && parameters!=null && parameters.isLoaded()) {
				hover.setValue(parameters.get("MPC_THR_HOVER").value * 1000f);
				mdl.setValue(parameters.get("THR_MDL_FAC").value * 100f);
			}
		});

		hover.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			setParameter("MPC_THR_HOVER",newvalue.intValue() / 1000f );
		});

		hover.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					hover.setValue(parameters.get("MPC_THR_HOVER").default_val * 1000);
				}
			}
		});

		mdl.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			setParameter("THR_MDL_FAC",newvalue.intValue() / 100f );
		});

		mdl.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					hover.setValue(parameters.get("THR_MDL_FAC").default_val * 100);
				}
			}
		});


	}

	private void setParameter(String name, float val) {

		ParameterAttributes param = parameters.get(name);

		if( Math.abs(param.value - val) > 0.05f &&
				state.getConnectedProperty().get() && state.getParamLoadedProperty().get()) {

			param.value = val;

			final msg_param_set msg = new msg_param_set(255,1);
			msg.target_component = 1;
			msg.target_system = 1;
			msg.setParam_id(param.name);
			msg.param_value = val;
			msg.param_type = param.vtype;
			control.sendMAVLinkMessage(msg);

		}

	}


}
