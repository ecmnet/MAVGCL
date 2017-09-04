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

package com.comino.flight.ui.sidebar;

import java.io.IOException;

import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.jfx.extensions.DashLabelLED;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.segment.Status;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class VehicleCtlWidget extends WidgetPane   {


	@FXML
	private VBox     box;

	@FXML
	private Button   reset_odometry;

	@FXML
	private Button   reset_microslam;

	@FXML
	private CheckBox enable_vision;

	@FXML
	private CheckBox enable_jumpback;

	@FXML
	private CheckBox enable_circle;

	@FXML
	private DashLabelLED jumpback;

	@FXML
	private DashLabelLED circlemode;



	private IMAVController control=null;

	public VehicleCtlWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("VehicleCtlWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

	}

	@FXML
	private void initialize() {

		box.prefHeightProperty().bind(this.heightProperty());


		enable_vision.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_jumpback.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.JUMPBACK;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_circle.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.CIRCLE_MODE;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		reset_odometry.setOnAction((ActionEvent event)-> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			msp.param1  = MSP_COMPONENT_CTRL.RESET;
			control.sendMAVLinkMessage(msp);
		});

		reset_microslam.setOnAction((ActionEvent event)-> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_MICROSLAM;
			msp.param1  = MSP_COMPONENT_CTRL.RESET;
			control.sendMAVLinkMessage(msp);
		});
	}




	public void setup(IMAVController control) {
		this.control = control;

		control.addStatusChangeListener((o,n) -> {
			Platform.runLater(() -> {
				if(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.JUMPBACK))
					jumpback.setMode(DashLabelLED.MODE_ON);
				else {
					jumpback.setMode(DashLabelLED.MODE_OFF);
				}
				if(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.CIRCLE_MODE))
					circlemode.setMode(DashLabelLED.MODE_ON);
				else
					circlemode.setMode(DashLabelLED.MODE_OFF);
			});
		});

		Platform.runLater(() -> {
			enable_vision.setSelected(control.getCurrentModel().sys.isSensorAvailable(Status.MSP_OPCV_AVAILABILITY));
		});
	}

}
