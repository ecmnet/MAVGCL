/****************************************************************************
 *
 *   Copyright (c) 2017,2020 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.widgets.panel;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_RESULT;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MSP_AUTOCONTROL_ACTION;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_manual_control;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.jfx.extensions.StateButton;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.mavlink.MAV_CUST_MODE;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.status.StatusManager;
import com.comino.mavutils.MSPMathUtils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class CommanderWidget extends ChartControlPane  {


	@FXML
	private Button arm_command;

	@FXML
	private Button land_command;

	@FXML
	private StateButton takeoff_command;

	@FXML
	private Button rtl_command;

	@FXML
	private Button hold_command;

	@FXML
	private Button emergency;

	private IMAVController control;
	private DataModel model;


	public CommanderWidget() {
		super(300, true);
		FXMLLoadHelper.load(this, "CommanderWidget.fxml");
	}

	@FXML
	private void initialize() {

		this.disableProperty().bind(state.getConnectedProperty().not());

		//fadeProperty().bind(state.getConnectedProperty());

		state.getArmedProperty().addListener((observable, oldvalue, newvalue) -> {
			Platform.runLater(() -> {
				if(newvalue.booleanValue())
					arm_command.setText("Disarm motors");
				else
					arm_command.setText("Arm motors");
			});
		});

		state.getLandedProperty().addListener((observable, oldvalue, newvalue) -> {
			if(control!=null && control.isSimulation()) {
				msg_manual_control rc = new msg_manual_control(255,1);
				if(newvalue.booleanValue())
					rc.z = 0;
				else
					rc.z = 500;
				control.sendMAVLinkMessage(rc);
			}
		});


		rtl_command.disableProperty().bind(state.getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty()));
		rtl_command.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
					MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
					MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_AUTO, MAV_CUST_MODE.PX4_CUSTOM_SUB_MODE_AUTO_RTL);


		});

		arm_command.disableProperty().bind(state.getLandedProperty().not()
				.and(state.getSimulationProperty().not()));

		arm_command.setOnAction((ActionEvent event)-> {

			if(!model.sys.isStatus(Status.MSP_ARMED)) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,1 );
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
			} else {
				if(model.sys.isStatus(Status.MSP_LANDED)) {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
				}
			}

		});

		land_command.disableProperty().bind(state.getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty()));
		land_command.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_LAND, ( cmd,result) -> {
				if(result != MAV_RESULT.MAV_RESULT_ACCEPTED)
					logger.writeLocalMsg("[mgc] PX4 landing rejected ("+result+")",MAV_SEVERITY.MAV_SEVERITY_WARNING);
			}, 0, 0, 0, model.attitude.sy );
		});

		hold_command.disableProperty().bind(state.getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty()));
		hold_command.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE, (cmd, result) -> {
				if(result != MAV_RESULT.MAV_RESULT_ACCEPTED)
					logger.writeLocalMsg("[mgc] Switching to hold rejected ("+result+")",MAV_SEVERITY.MAV_SEVERITY_WARNING);
			},	MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
					MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_AUTO, MAV_CUST_MODE.PX4_CUSTOM_SUB_MODE_AUTO_LOITER );
		});


		takeoff_command.disableProperty().bind(state.getArmedProperty().not()
				//	.or(state.getRCProperty())
				.or(state.getLandedProperty().not()));

		takeoff_command.setOnAction((ActionEvent event)-> {
			if(model.hud.ag!=Float.NaN && model.sys.isStatus(Status.MSP_LPOS_VALID) ) {
				if(state.getMSPProperty().get()) {
					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.MSP_CMD_AUTOMODE;
					msp.param2 =  MSP_AUTOCONTROL_ACTION.TAKEOFF;

					if(!control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_ACTION.TAKEOFF))
						msp.param1  = MSP_COMPONENT_CTRL.ENABLE;
					else
						msp.param1  = MSP_COMPONENT_CTRL.DISABLE;
					control.sendMAVLinkMessage(msp);

				}
				else {
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_TAKEOFF, -1, 0, 0, Float.NaN, Float.NaN, Float.NaN,Float.NaN);
					takeoff_command.setState(false);
				}
			}
			else {
				if(model.sys.isStatus(Status.MSP_LANDED))
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
				logger.writeLocalMsg("[mgc] Takoff rejected: LPOS not available",
						MAV_SEVERITY.MAV_SEVERITY_WARNING);
			}

		});

		emergency.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, (cmd,result) -> {
				if(result==0) {
					logger.writeLocalMsg("[mgc] User requested to switch off motors",
							MAV_SEVERITY.MAV_SEVERITY_EMERGENCY);
				}
			},0, 21196 );
		});
	}

	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_AUTOPILOT, MSP_AUTOCONTROL_ACTION.TAKEOFF,(n) -> {
			if(state.getMSPProperty().get())
				takeoff_command.setState(n.isAutopilotMode(MSP_AUTOCONTROL_ACTION.TAKEOFF));
		});
	}

}
