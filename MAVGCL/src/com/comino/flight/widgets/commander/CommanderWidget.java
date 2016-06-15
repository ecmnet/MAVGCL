/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.commander;

import java.io.IOException;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_manual_control;
import org.mavlink.messages.lquac.msg_msp_command;
import org.mavlink.messages.lquac.msg_rc_channels_override;
import org.mavlink.messages.lquac.msg_set_position_target_local_ned;
import org.mavlink.messages.lquac.msg_vision_position_estimate;
import org.mavlink.messages.lquac.msg_vision_speed_estimate;

import com.comino.flight.experimental.OffboardUpdater;
import com.comino.flight.experimental.VisionPositionSimulationUpdater;
import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class CommanderWidget extends Pane  {

	@FXML
	private Button arm_command;

	@FXML
	private Button land_command;

	@FXML
	private Button takeoff_command;

	@FXML
	private Button rtl_command;

	@FXML
	private Button emergency;

	private IMAVController control;
	private DataModel model;


	public CommanderWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CommanderWidget.fxml"));
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

		this.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());

		StateProperties.getInstance().getArmedProperty().addListener((observable, oldvalue, newvalue) -> {
			Platform.runLater(() -> {
				if(newvalue.booleanValue())
					arm_command.setText("Disarm motors");
				else
					arm_command.setText("Arm motors");
			});
		});

		StateProperties.getInstance().getLandedProperty().addListener((observable, oldvalue, newvalue) -> {
			if(control.isSimulation()) {
				msg_manual_control rc = new msg_manual_control(255,1);
				if(newvalue.booleanValue())
					rc.z = 0;
				else
					rc.z = 500;
				control.sendMAVLinkMessage(rc);
			}
		});


		rtl_command.disableProperty().bind(StateProperties.getInstance().getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty()));
		rtl_command.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
					MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
					MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_AUTO, MAV_CUST_MODE.PX4_CUSTOM_SUB_MODE_AUTO_RTL);


		});


		arm_command.disableProperty().bind(StateProperties.getInstance().getLandedProperty().not());
		arm_command.setOnAction((ActionEvent event)-> {
			if(!model.sys.isStatus(Status.MSP_ARMED)) {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,1 );
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
			} else {
				if(model.sys.isStatus(Status.MSP_LANDED))
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
			}

		});

		land_command.disableProperty().bind(StateProperties.getInstance().getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty()));
		land_command.setOnAction((ActionEvent event)-> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_LAND, 0, 2, 0.05f );
		});

		takeoff_command.disableProperty().bind(StateProperties.getInstance().getArmedProperty().not()
				.or(StateProperties.getInstance().getLandedProperty().not()));
		takeoff_command.setOnAction((ActionEvent event)-> {
			if(model.attitude.ag!=Float.NaN && model.sys.isStatus(Status.MSP_GPOS_AVAILABILITY))
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_TAKEOFF, -1, 0, 0, Float.NaN, Float.NaN, Float.NaN,
						model.attitude.ag+1);
			else {
				if(model.sys.isStatus(Status.MSP_LANDED))
					control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM,0 );
				MSPLogger.getInstance().writeLocalMsg("REJECTED: Global position not available");
			}

		});

		emergency.setOnAction((ActionEvent event)-> {
			if(control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, 0, 21196 ))
				MSPLogger.getInstance().writeLocalMsg("EMERGENCY: User requested to switch off motors");
		});
	}

	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
	}

}
