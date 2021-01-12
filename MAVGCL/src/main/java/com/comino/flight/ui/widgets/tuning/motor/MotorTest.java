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

package com.comino.flight.ui.widgets.tuning.motor;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.MOTOR_TEST_THROTTLE_TYPE;
import org.mavlink.messages.lquac.msg_param_set;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavcom.param.ParameterAttributes;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;


public class MotorTest extends VBox  {


	private static final int INITIAL_DELAY_MS = 6000;


	@FXML
	private HBox hbox;


	@FXML
	private Slider m1;

	@FXML
	private Slider m2;

	@FXML
	private Slider m3;

	@FXML
	private Slider m4;

	@FXML
	private Slider all;

	@FXML
	private CheckBox enable;
	
	@FXML
	private CheckBox record;


	private Timeline timeline;
	private Timeline delay;
	
	private AnalysisModelService modelService;
	
	private int motor;


	public MotorTest() {

		FXMLLoadHelper.load(this, "MotorTest.fxml");
		timeline = new Timeline(new KeyFrame(Duration.millis(3000), ae -> {
			m1.setValue(0); m2.setValue(0); m3.setValue(0); m4.setValue(0); 
			all.setValue(0);
			if(record.isSelected() && modelService.isCollecting())
			  modelService.stop(3);
			motor = 0;
		}));
		timeline.setCycleCount(1);
		timeline.setDelay(Duration.ZERO);
		

	}


	@FXML
	private void initialize() {

		m1.setOrientation(Orientation.VERTICAL);
		m2.setOrientation(Orientation.VERTICAL);
		m3.setOrientation(Orientation.VERTICAL);
		m4.setOrientation(Orientation.VERTICAL);
		all.setOrientation(Orientation.VERTICAL);

	}

	public void setup(IMAVController control) {
		
		this.modelService =  AnalysisModelService.getInstance();
		
		this.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());
		
		delay = new Timeline(new KeyFrame(Duration.millis(INITIAL_DELAY_MS), ae -> {
			switch(motor) {
			case 1:
				control.writeLogMessage(new LogMessage("[mgc] Motor 1 switched on",MAV_SEVERITY.MAV_SEVERITY_INFO));
				m1.setValue(500); break;
			case 2:
				control.writeLogMessage(new LogMessage("[mgc] Motor 2 switched on",MAV_SEVERITY.MAV_SEVERITY_INFO));
				m2.setValue(500); break;
			case 3:
				control.writeLogMessage(new LogMessage("[mgc] Motor 3 switched on",MAV_SEVERITY.MAV_SEVERITY_INFO));
				m3.setValue(500); break;
			case 4:
				control.writeLogMessage(new LogMessage("[mgc] Motor 4 switched on",MAV_SEVERITY.MAV_SEVERITY_INFO));
				m4.setValue(500); break;
			}
			
		}));
		delay.setCycleCount(1);
		delay.setDelay(Duration.ZERO);

		BooleanProperty armed   = StateProperties.getInstance().getArmedProperty();

		m1.disableProperty().bind (armed.or(enable.selectedProperty().not()));
		m2.disableProperty().bind( armed.or(enable.selectedProperty().not()));
		m3.disableProperty().bind( armed.or(enable.selectedProperty().not()));
		m4.disableProperty().bind( armed.or(enable.selectedProperty().not()));
		all.disableProperty().bind(armed.or(enable.selectedProperty().not()));

		m1.setOnMouseClicked((e) -> {
			if(e.getClickCount() == 2 && motor == 0) { 
				motor = 1; delay.play();
				if(record.isSelected())
					modelService.start();
			}
		});

		m2.setOnMouseClicked((e) -> {
			if(e.getClickCount() == 2 && motor == 0) {
				motor = 2; delay.play();
				if(record.isSelected())
					 modelService.start();
			}
		});

		m3.setOnMouseClicked((e) -> {
			if(e.getClickCount() == 2 && motor == 0) {
				motor = 3; delay.play();
				if(record.isSelected())
					modelService.start();
			}
		});

		m4.setOnMouseClicked((e) -> {
			if(e.getClickCount() == 2 && motor == 0) {
				motor = 4; delay.play();
				if(record.isSelected())
					modelService.start();
			}
		});

		armed.addListener((a,o,n) -> {
			if(n.booleanValue())
				enable.setSelected(false);		
		});


		m1.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST,1, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			timeline.play();
		});

		m2.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST, 2, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			timeline.play();
		});

		m3.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST, 3, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			timeline.play();
		});

		m4.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST, 4, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			timeline.play();
		});

		all.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST,1, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST,2, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST,3, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_MOTOR_TEST,4, MOTOR_TEST_THROTTLE_TYPE.MOTOR_TEST_THROTTLE_PERCENT,newvalue.floatValue()/10f,3,1);
			timeline.play();
		});

	}



}
