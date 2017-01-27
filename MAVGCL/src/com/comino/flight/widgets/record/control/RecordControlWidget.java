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

package com.comino.flight.widgets.record.control;


import java.io.IOException;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.log.FileHandler;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.info.InfoWidget;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPStatusChangedListener;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.msp.model.segment.Status;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class RecordControlWidget extends WidgetPane implements IMSPStatusChangedListener {

	private static final int TRIG_ARMED 		= 0;
	private static final int TRIG_LANDED		= 1;
	private static final int TRIG_ALTHOLD		= 2;
	private static final int TRIG_POSHOLD 		= 3;

	private static final String[]  TRIG_START_OPTIONS = { "armed", "started", "altHold entered", "posHold entered" };
	private static final String[]  TRIG_STOP_OPTIONS = { "unarmed", "landed", "altHold left", "posHold left" };

	private static final Integer[] TRIG_DELAY_OPTIONS = { 0, 2, 5, 10, 30 };

	@FXML
	private ToggleButton recording;

	@FXML
	private CheckBox enablemodetrig;

	@FXML
	private ChoiceBox<String> trigstart;

	@FXML
	private ChoiceBox<String> trigstop;

	@FXML
	private ChoiceBox<Integer> trigdelay;

	@FXML
	private ChoiceBox<Integer> predelay;

	@FXML
	private Circle isrecording;

	@FXML
	private Button clear;

	private IMAVController control;

	private int triggerStartMode =0;
	private int triggerStopMode  =0;
	private int triggerDelay =0;

	private boolean modetrigger  = false;
	protected int totalTime_sec = 30;
	private AnalysisModelService modelService;

	private ChartControlWidget charts;
	private InfoWidget info;


	public RecordControlWidget() {
		super(300,true);
		FXMLLoadHelper.load(this, "RecordControlWidget.fxml");
	}

	@FXML
	private void initialize() {

		trigstart.getItems().addAll(TRIG_START_OPTIONS);
		trigstart.getSelectionModel().select(0);
		trigstart.setDisable(true);
		trigstop.getItems().addAll(TRIG_STOP_OPTIONS);
		trigstop.getSelectionModel().select(0);
		trigstop.setDisable(true);
		trigdelay.getItems().addAll(TRIG_DELAY_OPTIONS);
		trigdelay.getSelectionModel().select(0);
		trigdelay.setDisable(true);
		predelay.getItems().addAll(TRIG_DELAY_OPTIONS);
		predelay.getSelectionModel().select(0);
		predelay.setDisable(true);

		state.getConnectedProperty().addListener((l,o,n) -> {
			recording.setDisable(!n.booleanValue());
			if(!n.booleanValue())
				recording(false,0);
		});

		recording.setOnMousePressed(event -> {
			enablemodetrig.selectedProperty().set(false);
		});

		recording.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			recording(newvalue, 0);
		});

		clear.disableProperty().bind(state.getRecordingProperty());
		clear.setOnAction((ActionEvent event)-> {
			AnalysisModelService.getInstance().clearModelList();
			FileHandler.getInstance().clear();
			StateProperties.getInstance().getLogLoadedProperty().set(false);
			charts.refreshCharts();
			info.clear();
		});


		recording.setTooltip(new Tooltip("start/stop recording"));

		enablemodetrig.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			modetrigger = newvalue;
			trigdelay.setDisable(oldvalue);
			trigstop.setDisable(oldvalue);
			trigstart.setDisable(oldvalue);

			if(modelService!=null && newvalue.booleanValue() && modelService.isCollecting())
				recording(false,0);

		});

		trigstart.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerStartMode = newvalue.intValue();
			triggerStopMode  = newvalue.intValue();
			trigstop.getSelectionModel().select(triggerStopMode);
		});

		trigstop.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerStopMode = newvalue.intValue();
		});


		trigdelay.getSelectionModel().selectedItemProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerDelay = newvalue.intValue();
		});

		StateProperties.getInstance().getConnectedProperty().addListener((observable, oldvalue, newvalue) -> {
			if(!newvalue.booleanValue())
				state.getRecordingProperty().set(false);
		});

		this.disabledProperty().addListener((observable, oldvalue, newvalue) -> {
			if(newvalue.booleanValue())
				state.getRecordingProperty().set(false);
		});

		enablemodetrig.selectedProperty().set(true);

		state.getRecordingProperty().addListener((o,ov,nv) -> {
			switch(modelService.getMode()) {

			case ModelCollectorService.STOPPED:

				recording.selectedProperty().set(false);
				isrecording.setFill(Color.LIGHTGREY);

				if(MAVPreferences.getInstance().getBoolean(MAVPreferences.AUTOSAVE, false)) {
					try {
						FileHandler.getInstance().autoSave();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;

			case ModelCollectorService.PRE_COLLECTING:
				FileHandler.getInstance().clear();
				recording.selectedProperty().set(true);
				isrecording.setFill(Color.LIGHTBLUE); break;
			case ModelCollectorService.POST_COLLECTING:

				recording.selectedProperty().set(true);
				isrecording.setFill(Color.LIGHTYELLOW); break;
			case ModelCollectorService.COLLECTING:

				FileHandler.getInstance().clear();
				recording.selectedProperty().set(true);
				isrecording.setFill(Color.RED); break;
			}
		});

		state.getLogLoadedProperty().addListener((o,ov,nv) -> {
			if(nv.booleanValue() && modelService.isCollecting()) {
				modelService.stop(0);
			}
		});
	}


	public void setup(IMAVController control, ChartControlWidget charts, InfoWidget info, StatusWidget statuswidget) {
		this.charts = charts;
		this.info = info;
		this.control = control;
		this.modelService =  AnalysisModelService.getInstance();
		this.control.addStatusChangeListener(this);
		this.modelService.setTotalTimeSec(totalTime_sec);
		this.modelService.clearModelList();

	}

	@Override
	public void update(Status oldStat, Status newStat) {

		if(!modetrigger)
			return;

		if(!modelService.isCollecting()) {
			switch(triggerStartMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),0); break;
			case TRIG_LANDED:		recording(!newStat.isStatus(Status.MSP_LANDED),0); break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE) ||
					newStat.isStatus(Status.MSP_MODE_POSITION),0); break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION),0); break;
			}
		} else {
			switch(triggerStopMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),triggerDelay);
			break;
			case TRIG_LANDED:		recording(!newStat.isStatus(Status.MSP_LANDED),triggerDelay);
			break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE) ||
					         newStat.isStatus(Status.MSP_MODE_POSITION),triggerDelay);
			break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION),triggerDelay);
			break;
			}
		}
	}


	private void recording(boolean start, int delay) {
		if(start)
			modelService.start();
		else
			modelService.stop(delay);
	}
}
