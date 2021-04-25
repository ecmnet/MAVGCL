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
import java.util.prefs.Preferences;

import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;
import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.jfx.extensions.StateButton;
import com.comino.flight.model.map.MAVGCLMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.model.segment.Vision;
import com.comino.mavcom.status.StatusManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;

public class MSPCtlWidget extends ChartControlPane   {

	private static final String[]  STREAMS = { "Foreward", "Downward" };


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
	private CheckBox enable_takeoff_proc;

	@FXML
	private CheckBox enable_precision_lock;

	@FXML
	private ChoiceBox<String> stream;

	@FXML
	private StateButton enable_offboard;

	@FXML
	private StateButton enable_stop;

	@FXML
	private StateButton enable_avoidance;

	@FXML
	private StateButton enable_interactive;

	@FXML
	private StateButton enable_follow;

	@FXML
	private StateButton enable_planner;

	@FXML
	private StateButton enable_rtl;

	@FXML
	private Button debug_mode1;

	@FXML
	private Button debug_mode2;

	@FXML
	private Button rotate_north;

	@FXML
	private Button exec_land;

	@FXML
	private Button test_seq1;

	@FXML
	private Button save_map;

	@FXML
	private Button load_map;

	@FXML
	private Button   abort;

	private IMAVController  control = null;
	private Preferences     prefs   = null;

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

		modes.disableProperty().bind(state.getOffboardProperty().not());

		enable_takeoff_proc.disableProperty().bind(state.getLandedProperty().not());

		stream.getItems().addAll(STREAMS);

		state.getFiducialLockedProperty().addListener((v,o,n) -> {
			if(n.booleanValue())
				state.getStreamProperty().set(1);
			else
				if(state.getSLAMAvailableProperty().get())
					state.getStreamProperty().set(0);
		});

		stream.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
			state.getStreamProperty().set(newvalue.intValue());
		});


		state.getStreamProperty().addListener((o,ov,nv) -> {
			stream.getSelectionModel().select(nv.intValue());		
		});

		enable_vision.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_takeoff_proc.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.TAKEOFF_PROCEDURE;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_precision_lock.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.PRECISION_LOCK;
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

		enable_follow.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT))
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

		rotate_north.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.ROTATE;
			msp.param3 = 0f;
			control.sendMAVLinkMessage(msp);

		});

		exec_land.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.LAND;
			control.sendMAVLinkMessage(msp);

		});

		test_seq1.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.TEST_SEQ1;
			control.sendMAVLinkMessage(msp);

		});

		enable_planner.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.PX4_PLANNER;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.PX4_PLANNER))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
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
		this.prefs   = MAVPreferences.getInstance();

		state.getConnectedProperty().addListener((c,o,n) -> {
			if(n.booleanValue()) {
				Platform.runLater(() -> {
					stream.getSelectionModel().select(0);
					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.SELECT_VIDEO_STREAM;
					msp.param1  = stream.getSelectionModel().getSelectedIndex();
					control.sendMAVLinkMessage(msp);	
				}); 
			}
		});



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

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT,(n) -> {
			enable_follow.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.PX4_PLANNER,(n) -> {
			enable_planner.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.PX4_PLANNER));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER,(n) -> {
			enable_offboard.setState(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.OFFBOARD_UPDATER));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.TAKEOFF_PROCEDURE,(n) -> {
			enable_takeoff_proc.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.TAKEOFF_PROCEDURE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.PRECISION_LOCK,(n) -> {
			enable_precision_lock.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.PRECISION_LOCK));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_SERVICES, Status.MSP_OPCV_AVAILABILITY,(n) -> {
			if(n.isSensorAvailable(Status.MSP_OPCV_AVAILABILITY))
				enable_vision.setSelected(true);
		});
	}

}
