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

package com.comino.flight.tabs.experimental;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_set_position_target_local_ned;

import com.comino.flight.experimental.OffboardSimulationUpdater;
import com.comino.flight.experimental.VisionPositionSimulationUpdater;
import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVExperimentalTab extends BorderPane  {


	@FXML
	private Button exp1;

	@FXML
	private Button exp2;

	@FXML
	private Button althold_command;

	@FXML
	private Button poshold_command;


	private VisionPositionSimulationUpdater vision = null;
	private OffboardSimulationUpdater offboard = null;

	private IMAVController control;

	private DataModel model;


	public MAVExperimentalTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVExperimentalTab.fxml"));
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

		exp1.setOnAction((ActionEvent event)-> {
			offboard.start();


		});

		exp2.setOnAction((ActionEvent event)-> {
			offboard.stop();



		});

		althold_command.setOnAction((ActionEvent event)-> {
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

			if(!model.sys.isStatus(Status.MSP_MODE_POSITION)) {

				msg_set_position_target_local_ned cmd = new msg_set_position_target_local_ned(255,1);
				cmd.target_component = 1;
				cmd.target_system = 1;
				cmd.x =  0;
				cmd.y =  0;
				cmd.z = -2;
				control.sendMAVLinkMessage(cmd);

				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
			}
			else
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );

		});

	}


	public MAVExperimentalTab setup(IMAVController control) {
	    this.control = control;
	    this.model   = control.getCurrentModel();
	//	vision = new VisionPositionSimulationUpdater(control);
	//	offboard = new OffboardSimulationUpdater(control);
		return this;
	}


}
