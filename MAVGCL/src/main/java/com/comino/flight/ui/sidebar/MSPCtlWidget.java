/****************************************************************************
 *
 *   Copyright (c) 2017,2023 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import java.util.Optional;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;
import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.jfx.extensions.StateButton;
import com.comino.flight.MainApp;
import com.comino.flight.file.FileHandler;
import com.comino.flight.file.MAVFTPClient;
import com.comino.flight.model.map.MAVGCLMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
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
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class MSPCtlWidget extends ChartControlPane   {

	private static final String[]  STREAMS = { "FPV+Down", "Down", "Depth", "FPV" };


	@FXML
	private VBox     box;

	@FXML
	private VBox     modes;

	@FXML
	private VBox     sitl;

	@FXML
	private VBox     scenario_group;

	@FXML
	private VBox     msp_control;

	@FXML
	private VBox     actions;

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
	private CheckBox enable_collision_avoidance;

	@FXML
	private CheckBox enable_turn_to_person;

	@FXML
	private CheckBox enable_obstacle_stop;

	@FXML
	private CheckBox enable_mode2;

	@FXML
	private CheckBox enable_mode3;

	@FXML
	private ChoiceBox<String> stream;

	@FXML
	private StateButton enable_interactive;

	@FXML
	private StateButton enable_rtl;

	@FXML
	private ComboBox<String> scenario_select;

	@FXML
	private Button scenario_execute;

	@FXML
	private Button debug_mode1;

	@FXML
	private Button debug_mode2;

	@FXML
	private Button rotate;

	@FXML
	private Button exec_land;

	@FXML
	private Button sitl_action1;

	@FXML
	private Button sitl_action2;

	@FXML
	private StateButton sitl_mode1;

	@FXML
	private Button test_seq1;

	@FXML
	private Button save_map;

	@FXML
	private Button load_map;

	@FXML
	private Button   abort;

	private IMAVController  control = null;

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

		enable_mode2.setDisable(true);
		enable_mode3.setDisable(true);
		enable_takeoff_proc.setDisable(true);
		exec_land.setDisable(true);

		actions.setDisable(true);

		//enable_takeoff_proc.disableProperty().bind(state.getLandedProperty().not());

		stream.getItems().addAll(STREAMS);
		stream.getSelectionModel().select(0);

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
		
		enable_collision_avoidance.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.COLLISION_PREVENTION;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_turn_to_person.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		enable_obstacle_stop.setDisable(true);
		enable_obstacle_stop.selectedProperty().addListener((v,o,n) -> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_STOP;
			if(n.booleanValue())
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
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

		scenario_select.disableProperty().bind(enable_interactive.selectedProperty().not());
		//	.or(StateProperties.getInstance().getSimulationProperty()));

		scenario_select.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
			if(nv==null || nv.contains("..."))
				return;
			String scenario = MAVPreferences.getInstance().get(MAVPreferences.SCENARIO_DIR,System.getProperty("user.home"))
					+"/" + nv + ".xml";
			MAVFTPClient ftp = MAVFTPClient.getInstance(control);
			if(!ftp.sendFileAs(scenario, "scenario.xml")) {
				scenario_select.getEditor().setText("Select scenario...");
				scenario_execute.setDisable(true);
			}
			ftp.close();
			scenario_execute.setDisable(false);
		});

		scenario_select.disabledProperty().addListener((c,o,n) -> {
			if(n.booleanValue()) {
				scenario_select.getSelectionModel().clearSelection();
				scenario_select.getEditor().setText("Select scenario...");
				scenario_execute.setDisable(true);
			}
		});

		scenario_execute.setDisable(true);
		scenario_execute.setOnAction((event) ->{
			if(scenario_select.getEditor().getText().contains("...")) {
				logger.writeLocalMsg("[mgc] Select scenario first.",MAV_SEVERITY.MAV_SEVERITY_WARNING);
				return;
			}

			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_EXECUTE_SCENARIO;
			control.sendMAVLinkMessage(msp);

		});

		debug_mode1.disableProperty().bind(enable_interactive.selectedProperty().not());
		debug_mode1.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.DEBUG_MODE1;
			control.sendMAVLinkMessage(msp);

		});

		debug_mode2.disableProperty().bind(enable_interactive.selectedProperty().not());
		debug_mode2.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.DEBUG_MODE2;
			control.sendMAVLinkMessage(msp);

		});

		rotate.disableProperty().bind(enable_interactive.selectedProperty().not());
		rotate.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.ROTATE;
			msp.param3 = 180f;
			control.sendMAVLinkMessage(msp);

		});


		exec_land.disableProperty().bind(enable_interactive.selectedProperty().not());
		exec_land.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.LAND;
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

		sitl_mode1.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_MODE.SITL_MODE1;

			if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.SITL_MODE1))
				msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
			else
				msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
			control.sendMAVLinkMessage(msp);

		});

		sitl_action1.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.SITL_ACTION1;
			control.sendMAVLinkMessage(msp);

		});

		sitl_action2.setOnAction((event) ->{
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
			msp.param2 =  MSP_AUTOCONTROL_ACTION.SITL_ACTION2;
			control.sendMAVLinkMessage(msp);

		});


	}


	public void setup(IMAVController control) {
		this.control = control;
		this.prefs   = MAVPreferences.getInstance();

		sitl.disableProperty().bind(state.getSimulationProperty().not());

		scenario_select.getEditor().setText("Select scenario...");
		scenario_select.getItems().addAll(FileHandler.getInstance().getScenarioList());
		scenario_select.setEditable(true);
		scenario_select.getEditor().setEditable(false);
		scenario_select.getEditor().setCursor(Cursor.DEFAULT);
		scenario_select.setVisibleRowCount(15);

		state.getCVAvailableProperty().addListener((c,o,n) -> {
			if(n.booleanValue()) {	
				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.SELECT_VIDEO_STREAM;
				msp.param1  = state.getStreamProperty().intValue();
				control.sendMAVLinkMessage(msp);	
			}
		});


		state.getMSPProperty().addListener((c,o,n) -> {

			if(n.booleanValue() ) {	
				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.SELECT_VIDEO_STREAM;
				msp.param1  = state.getStreamProperty().intValue();
				control.sendMAVLinkMessage(msp);	

				msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
				msp.param2 =  MSP_AUTOCONTROL_MODE.OBSTACLE_STOP;
				if(enable_obstacle_stop.isSelected())
					msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
				else
					msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
				control.sendMAVLinkMessage(msp);

				msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
				msp.param2 =  MSP_AUTOCONTROL_MODE.FOLLOW_OBJECT;
				if(enable_turn_to_person.isSelected())
					msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
				else
					msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
				control.sendMAVLinkMessage(msp);

				msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
				msp.param2 =  MSP_AUTOCONTROL_MODE.PRECISION_LOCK;
				if(enable_precision_lock.isSelected())
					msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
				else
					msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
				control.sendMAVLinkMessage(msp);


			}
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_ACTION.RTL,(n) -> {
			enable_rtl.setState(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.RTL));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.INTERACTIVE,(n) -> {
			enable_interactive.setState(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.INTERACTIVE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.TAKEOFF_PROCEDURE,(n) -> {
			enable_takeoff_proc.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.TAKEOFF_PROCEDURE));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.PRECISION_LOCK,(n) -> {
			enable_precision_lock.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.PRECISION_LOCK));
		});
		
		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.COLLISION_PREVENTION,(n) -> {
			enable_collision_avoidance.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.COLLISION_PREVENTION));
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_MODE.SITL_MODE1,(n) -> {
			sitl_mode1.setSelected(n.isAutopilotMode(MSP_AUTOCONTROL_MODE.SITL_MODE1));
		});



		enable_vision.setSelected(true);

	}

	public static boolean confirmationDialog(Alert.AlertType alertType, String statement) {
		Alert alert = new Alert(alertType, statement);
		alert.getDialogPane().getStylesheets().add(MainApp.class.getResource("application.css").toExternalForm());
		alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));
		Optional<ButtonType> choose = alert.showAndWait();
		return choose.get() == ButtonType.OK;
	}



}
