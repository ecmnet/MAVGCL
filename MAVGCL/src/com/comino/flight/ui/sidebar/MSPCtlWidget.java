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

package com.comino.flight.ui.sidebar;

import java.io.IOException;

import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;
import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.StateButton;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.status.StatusManager;

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
	private VBox     msp_control;

	@FXML
	private Button   reset_odometry;

	@FXML
	private Button   reset_microslam;

	@FXML
	private CheckBox enable_vision;

	@FXML
	private StateButton enable_offboard;

	@FXML
	private StateButton enable_stop;

	@FXML
	private StateButton enable_avoidance;

	@FXML
	private StateButton enable_interactive;

	@FXML
	private StateButton enable_rtl;

	@FXML
	private Button debug_mode1;

	@FXML
	private Button debug_mode2;

	@FXML
	private Button restart;

	@FXML
	private Button  filter;

	@FXML
	private Button save_map;

	@FXML
	private Button load_map;

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

		this.disableProperty().bind(state.getConnectedProperty().not().or(state.getMSPProperty().not()));

		box.prefHeightProperty().bind(this.heightProperty());

		modes.disableProperty().bind(StateProperties.getInstance().getLandedProperty()
				.or(StateProperties.getInstance().getOffboardProperty().not()));


		enable_vision.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_stop.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_STOP;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.OBSTACLE_STOP))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_AVOIDANCE;
			msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);


		});

		enable_avoidance.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_AVOIDANCE;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.OBSTACLE_AVOIDANCE))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_STOP;
			msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_interactive.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.INTERACTIVE;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.INTERACTIVE))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});


		enable_rtl.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.RTL;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_ACTION.RTL))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		debug_mode1.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.DEBUG_MODE1;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_ACTION.DEBUG_MODE1))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		debug_mode2.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.DEBUG_MODE2;
			control.sendMAVLinkMessage(msp);

		});

		restart.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_RESTART;
			control.sendMAVLinkMessage(msp);

		});

		enable_offboard.setOnAction((event) ->{

			if(control.getCurrentModel().sys.isStatus(Status.MSP_LANDED))
				return;

			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});


		save_map.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.SAVE_MAP2D;
			msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			control.sendMAVLinkMessage(msp);
		});

		load_map.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.LOAD_MAP2D;
			msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			control.sendMAVLinkMessage(msp);
		});

		filter.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.APPLY_MAP_FILTER;
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

		msp_control.disableProperty().bind(state.getArmedProperty());

	}




	public void setup(IMAVController control) {
		this.control = control;

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_ACTION.RTL,(n) -> {
			enable_rtl.setState(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.RTL));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.OBSTACLE_STOP,(n) -> {
			enable_stop.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.OBSTACLE_STOP));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.OBSTACLE_AVOIDANCE,(n) -> {
			enable_avoidance.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.OBSTACLE_AVOIDANCE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.INTERACTIVE,(n) -> {
			enable_interactive.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.INTERACTIVE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER,(n) -> {
			enable_offboard.setState(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER));
		});

		Platform.runLater(() -> {
			enable_vision.setSelected(true);
		});

	}

}
