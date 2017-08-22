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

package com.comino.flight.ui.widgets.experimental;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.experimental.VisionSimulationUpdater;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.offboard.OffboardPositionUpdater;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class ExperimentalWidget extends WidgetPane   {


	@FXML
	private GridPane grid;

	@FXML
	private CheckBox offboard_enabled;

	@FXML
	private CheckBox vision_enabled;

	@FXML
	private Button althold_command;

	@FXML
	private Button file_command;

	@FXML
	private Button vis_reset;

	@FXML
	private Slider alt_control;

	@FXML
	private Slider x_control;

	@FXML
	private Slider y_control;

	private DataModel model;
	private VisionSimulationUpdater vision = null;
	private OffboardPositionUpdater offboard = null;
	private IMAVController control;

	private boolean file_enabled = false;

	public ExperimentalWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ExperimentalWidget.fxml"));
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

		vision_enabled.selectedProperty().addListener((v,ov,nv) -> {
			if(nv.booleanValue())  {
				vision.start();
			} else {
				vision.stop();
			}
		});

		vis_reset.setOnAction((ActionEvent event)-> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			msp.param1 = MSP_COMPONENT_CTRL.RESET;
			control.sendMAVLinkMessage(msp);
		});


		althold_command.setOnAction((ActionEvent event)-> {

			//			msg_msp_command msp = new msg_msp_command(255,1);
			//			msp.command = MSP_CMD.MSP_CMD_RESTART;
			//			control.sendMAVLinkMessage(msp);

			if(!model.sys.isStatus(Status.MSP_ARMED))
				return;

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

		file_command.setOnAction((ActionEvent event)-> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_COMBINEDFILESTREAM;
			if(file_enabled) {
				System.out.println("Stop stream recording");
				msp.param1 = 0;
			} else {
				System.out.println("Start stream recording");
				msp.param1 = 1;
			}
			control.sendMAVLinkMessage(msp);
			file_enabled = !file_enabled;

		});

		//		poshold_command.setOnAction((ActionEvent event)-> {
		//
		//			if(!model.sys.isStatus(Status.MSP_ARMED))
		//				return;
		//
		//			if(!model.sys.isStatus(Status.MSP_MODE_POSITION)) {
		//
		//
		//				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
		//						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
		//						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
		//			}
		//			else
		//				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
		//						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
		//						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );
		//		});

		alt_control.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			if(offboard_enabled.isSelected())
				offboard.setNEDZ(-newvalue.intValue()/100f);
		});

		x_control.setValue(0);
		x_control.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			if(offboard_enabled.isSelected())
				offboard.setNEDX(newvalue.intValue()/100f);
		});

		x_control.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					x_control.setValue(0);
				}
			}
		});


		y_control.setValue(0);
		y_control.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			if(offboard_enabled.isSelected())
				offboard.setNEDY(newvalue.intValue()/100f);
		});

		y_control.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					y_control.setValue(0);
				}
			}
		});

	}




	public void setup(IMAVController control) {

		this.control = control;
		this.model   = control.getCurrentModel();
		offboard = new OffboardPositionUpdater(control);
		vision = new VisionSimulationUpdater(control);

		offboard.enableProperty().addListener((e,o,n) -> {
			if(!n.booleanValue())
				Platform.runLater(() -> { offboard_enabled.setSelected(false);
				});
		});

		offboard_enabled.selectedProperty().addListener((e,o,n) -> {
			offboard.setNEDZ(1);
			offboard.enableProperty().set(n.booleanValue());
		});

	}

}
