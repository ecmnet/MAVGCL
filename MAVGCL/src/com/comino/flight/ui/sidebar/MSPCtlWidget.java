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

import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.StateButton;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.execution.control.StatusManager;
import com.comino.msp.model.segment.Status;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class MSPCtlWidget extends WidgetPane   {


	@FXML
	private VBox     box;

	@FXML
	private VBox     settings;

	@FXML
	private VBox     modes;

	@FXML
	private Button   reset_odometry;

	@FXML
	private Button   reset_microslam;

	@FXML
	private CheckBox enable_vision;

	@FXML
	private StateButton enable_jumpback;

	@FXML
	private StateButton enable_circle;

	@FXML
	private Button   execute_waypoints;

	@FXML
	private Button   execute_mission;


	@FXML
	private Button   abort;


	private IMAVController control=null;

	public MSPCtlWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MSPCtlWidget.fxml"));
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

		modes.disableProperty().bind(StateProperties.getInstance().getLandedProperty());


		enable_vision.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_jumpback.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.JUMPBACK;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.JUMPBACK))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_circle.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.CIRCLE_MODE;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.CIRCLE_MODE))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		execute_waypoints.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.WAYPOINT_MODE;
		    msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			control.sendMAVLinkMessage(msp);
		});

		execute_mission.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.AUTO_MISSION;
		    msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			control.sendMAVLinkMessage(msp);
		});

		abort.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.ABORT;
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

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.CIRCLE_MODE,(o,n) -> {
			enable_circle.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.CIRCLE_MODE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.JUMPBACK,(o,n) -> {
			enable_jumpback.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.JUMPBACK));
		});

		Platform.runLater(() -> {
			enable_vision.setSelected(control.getCurrentModel().sys.isSensorAvailable(Status.MSP_OPCV_AVAILABILITY));
		});
	}

}
