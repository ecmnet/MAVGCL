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

package com.comino.flight.widgets.experimental;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.MSP_COMPONENT_CTRL;
import org.mavlink.messages.lquac.msg_logging_ack;
import org.mavlink.messages.lquac.msg_logging_data_acked;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.experimental.OffboardUpdater;
import com.comino.flight.experimental.VisionSpeedSimulationUpdater;
import com.comino.flight.widgets.fx.controls.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

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

public class ExperimentalWidget extends WidgetPane implements IMAVLinkListener  {


	@FXML
	private GridPane grid;

	@FXML
	private CheckBox offboard_enabled;

	@FXML
	private CheckBox mavlink_enabled;

	@FXML
	private Button althold_command;

	@FXML
	private Button poshold_command;

	@FXML
	private Button vis_reset;

	@FXML
	private Slider alt_control;

	@FXML
	private Slider x_control;

	@FXML
	private Slider y_control;

	private DataModel model;
	private VisionSpeedSimulationUpdater vision = null;
	private OffboardUpdater offboard = null;
	private IMAVController control;

	private Task<Long> task;


	public ExperimentalWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ExperimentalWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}



		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					LockSupport.parkNanos(250000000L);
					if(isDisabled() || !isVisible() || offboard_enabled.isSelected()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					Platform.runLater(() -> {
						alt_control.setValue(-model.state.l_z * 100f);
					});
				}
				return model.tms;
			}
		};
	}

	@FXML
	private void initialize() {

		mavlink_enabled.selectedProperty().addListener((v,ov,nv) -> {
			if(nv.booleanValue())  {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_START,0);
//				msg_logging_ack ack = new msg_logging_ack(255,1);
//				ack.target_component=1;
//				ack.target_system=1;
//				ack.sequence=1;
//				control.sendMAVLinkMessage(ack);
			} else {
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
			}
		});

		vis_reset.setOnAction((ActionEvent event)-> {
			msg_msp_command msp = new msg_msp_command(255,1);
			msp.command = MSP_CMD.MSP_CMD_VISION;
			msp.param1 = MSP_COMPONENT_CTRL.RESET;
			control.sendMAVLinkMessage(msp);
		});

		offboard_enabled.selectedProperty().addListener((v,ov,nv) -> {

			//
			//			if(!model.sys.isStatus(Status.MSP_ARMED))
			//				return;

			if(nv.booleanValue()) {
				if(!offboard.isRunning()) {
					offboard.start();

					msg_msp_command msp = new msg_msp_command(255,1);
					msp.command = MSP_CMD.MSP_CMD_OFFBOARD;
					msp.param1 = MSP_COMPONENT_CTRL.ENABLE;
					control.sendMAVLinkMessage(msp);

				}

				if(control.isSimulation()) {
					if(!control.getCurrentModel().sys.isStatus(Status.MSP_MODE_OFFBOARD))
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
								MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
								MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_OFFBOARD, 0 );
				}
			} else {
				offboard.stop();

				msg_msp_command msp = new msg_msp_command(255,1);
				msp.command = MSP_CMD.MSP_CMD_OFFBOARD;
				msp.param1 = MSP_COMPONENT_CTRL.ENABLE;
				control.sendMAVLinkMessage(msp);


				if(control.isSimulation()) {
					if(control.getCurrentModel().sys.isStatus(Status.MSP_MODE_OFFBOARD))
						control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
								MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
								MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
				}
			}

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

		poshold_command.setOnAction((ActionEvent event)-> {

			if(!model.sys.isStatus(Status.MSP_ARMED))
				return;

			if(!model.sys.isStatus(Status.MSP_MODE_POSITION)) {


				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_POSCTL, 0 );
			}
			else
				control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
						MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
						MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );
		});

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

	@Override
	public void received(Object o) {
		if(o instanceof msg_logging_data_acked) {
			msg_logging_data_acked log = (msg_logging_data_acked)o;
			msg_logging_ack ack = new msg_logging_ack(255,1);
			ack.target_component=1;
			ack.target_system=1;
			ack.sequence=log.sequence;
			control.sendMAVLinkMessage(ack);
		}

	}


	public void setup(IMAVController control) {

		this.control = control;
		this.control.addMAVLinkListener(this);
		this.model   = control.getCurrentModel();
		offboard = new OffboardUpdater(control);
		vision = new VisionSpeedSimulationUpdater(control);

		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
		th.setDaemon(true);
		th.start();

	}

}
