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

package com.comino.flight.widgets.charts.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ChartControlWidget extends Pane implements IMSPModeChangedListener {

	private static final int TRIG_ARMED 		= 0;
	private static final int TRIG_ALTHOLD		= 1;
	private static final int TRIG_POSHOLD 		= 2;

	private static final String[]  TRIG_START_OPTIONS = { "armed", "altHold entered", "posHold entered" };
	private static final String[]  TRIG_STOP_OPTIONS = { "unarmed", "altHold left", "posHold left" };

	private static final Integer[] TRIG_DELAY_OPTIONS = { 0, 2, 5, 10, 30 };
	private static final Integer[] TOTAL_TIME = { 10, 30, 60, 240, 1200 };

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
	private ChoiceBox<Integer> totaltime;

    @FXML Slider scroll;

	@FXML
	private Circle isrecording;

	private Task<Integer> task;

	private IMAVController control;
	private List<IChartControl> charts = null;

	private int triggerStartMode =0;
	private int triggerStopMode  =0;
	private int triggerDelay =0;

	private boolean modetrigger  = false;


	public ChartControlWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChartControlWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}
		charts = new ArrayList<IChartControl>();

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}


					updateValue(control.getCollector().getMode());
				}
				return control.getCollector().getMode();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {

			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				switch(newData) {
				case ModelCollectorService.STOPPED:
					recording.selectedProperty().set(false);
					isrecording.setFill(Color.LIGHTGREY); break;
				case ModelCollectorService.PRE_COLLECTING:
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.LIGHTBLUE); break;
				case ModelCollectorService.POST_COLLECTING:
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.LIGHTYELLOW); break;
				case ModelCollectorService.COLLECTING:
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.RED); break;
				}
			}
		});

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

		totaltime.getItems().addAll(TOTAL_TIME);
		totaltime.getSelectionModel().select(1);



		recording.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov,
					Boolean old_val, Boolean new_val) {
				recording(new_val, 0);
			}
		});

		recording.setTooltip(new Tooltip("start/stop recording"));


		enablemodetrig.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov,
					Boolean old_val, Boolean new_val) {
				modetrigger = new_val;
				trigdelay.setDisable(old_val);
				trigstop.setDisable(old_val);
				trigstart.setDisable(old_val);
				recording.setDisable(new_val);

			}
		});

		trigstart.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				triggerStartMode = newValue.intValue();
				triggerStopMode  = newValue.intValue();
				trigstop.getSelectionModel().select(triggerStopMode);
			}
		});

		trigstop.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				triggerStopMode = newValue.intValue();
			}
		});

		trigdelay.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				triggerDelay = newValue.intValue();
			}
		});

		totaltime.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				for(IChartControl chart : charts)
					chart.getTimeFrameProperty().set(newValue.intValue());
			}
		});

		scroll.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				for(IChartControl chart : charts) {
					if(chart.getScrollProperty()!=null)
					   chart.getScrollProperty().set(new_val.intValue());
				}


			}
		});

	}

	public void setup(IMAVController control) {
		this.control = control;
		this.control.addModeChangeListener(this);
		ExecutorService.get().execute(task);

	}


	public void addChart(IChartControl chart) {
		charts.add(chart);
	}


	private void recording(boolean start, int delay) {
			if(start) {
				control.getMessageList().clear();
				control.getCollector().start();
				scroll.setDisable(true);
			}
			else {
				control.getCollector().stop(delay);
				scroll.setDisable(false);
			}
	}

	@Override
	public void update(Status oldStat, Status newStat) {
		if(!modetrigger)
			return;

		if(!control.getCollector().isCollecting()) {
			switch(triggerStartMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),0); break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE),0); break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION),0); break;
			}
		} else {
			switch(triggerStopMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),triggerDelay);         break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE),triggerDelay); break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION),triggerDelay); break;
			}

		}
	}

}
