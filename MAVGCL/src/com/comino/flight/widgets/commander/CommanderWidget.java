/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.widgets.commander;

import java.io.IOException;
import java.text.DecimalFormat;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_FRAME;
import org.mavlink.messages.MAV_MODE;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_command_long;
import org.mavlink.messages.lquac.msg_set_mode;
import org.mavlink.messages.lquac.msg_set_position_target_local_ned;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.model.utils.Utils;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class CommanderWidget extends Pane  {

	@FXML
	private Button land_command;

	@FXML
	private Button takeoff_command;

	@FXML
	private Button althold_command;

	@FXML
	private Button poshold_command;

	@FXML
	private Button left;

	@FXML
	private Button right;

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

		land_command.setOnAction((ActionEvent event)-> {

			if(!model.sys.isStatus(Status.MSP_ARMED)) {
				MSPLogger.getInstance().writeLocalMsg("Not armed: Changing mode rejected");
				return;
			}

			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_NAV_LAND, 0, 2, 0.05f );

		});

		takeoff_command.setDisable(true);
		takeoff_command.setOnAction((ActionEvent event)-> {


				System.out.println("Rejected");


		});


		althold_command.setOnAction((ActionEvent event)-> {

			if(!model.sys.isStatus(Status.MSP_ARMED)) {
				MSPLogger.getInstance().writeLocalMsg("Not armed: Changing mode rejected");
				return;
			}

			if(!model.sys.isStatus(Status.MSP_MODE_ALTITUDE))
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );
			else {
				if(!model.sys.isStatus(Status.MSP_LANDED))
					MSPLogger.getInstance().writeLocalMsg("AltHold mode cannot be reversed by GCL in flight");
				else
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
			}

		});

		poshold_command.setOnAction((ActionEvent event)-> {

			if(!model.sys.isStatus(Status.MSP_ARMED)) {
				MSPLogger.getInstance().writeLocalMsg("Not armed: Changing mode rejected");
				return;
			}

			if(!model.sys.isStatus(Status.MSP_MODE_POSITION))
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
			else
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );

		});

//		left.setOnAction((ActionEvent event)-> {
//
//			if(!model.sys.isStatus(Status.MSP_ARMED)) {
//				System.out.println("Not armed: Changing mode rejected");
//				return;
//			}
//
//			if(!model.sys.isStatus(Status.MSP_MODE_POSITION))
//				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
//						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
//						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_OFFBOARD, 0 );
//			else
//				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
//						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
//						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_MANUAL, 0 );
//
//		});

	}




	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
	}

}
