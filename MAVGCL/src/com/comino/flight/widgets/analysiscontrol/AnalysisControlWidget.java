package com.comino.flight.widgets.analysiscontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class AnalysisControlWidget extends Pane implements IMSPModeChangedListener {

	private static final int TRIG_ARMED 		= 0;
	private static final int TRIG_ALTHOLD		= 1;
	private static final int TRIG_POSHOLD 		= 2;

	private static final String[]  TRIG_START_OPTIONS = { "armed", "altHold entered", "posHold entered" };
	private static final String[]  TRIG_STOP_OPTIONS = { "unarmed", "altHold left", "posHold left" };

	private static final Integer[] TRIG_DELAY_OPTIONS = { 0, 10, 60, 120 };
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
	private ChoiceBox<Integer> totaltime;


	@FXML
	private Circle isrecording;

	private IMAVController control;
	private List<IChartControl> charts = null;

	private int triggerStartMode =0;
	private int triggerStopMode  =0;
	private int triggerStopDelay =0;

	private boolean modetrigger  = false;


	public AnalysisControlWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AnalysisControlWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}
		charts = new ArrayList<IChartControl>();
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

		totaltime.getItems().addAll(TOTAL_TIME);
		totaltime.getSelectionModel().select(1);



		recording.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov,
					Boolean old_val, Boolean new_val) {
				recording(new_val);
			}
		});


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
				triggerStopDelay = newValue.intValue();
			}
		});

		totaltime.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				for(IChartControl chart : charts)
				  chart.setTotalTime(newValue.intValue());
			}
		});
	}

	public void setup(IMAVController control) {
		this.control = control;
		this.control.addModeChangeListener(this);

	}


	public void addChart(IChartControl chart) {
		charts.add(chart);
	}


	private void recording(boolean start) {
		if(start) {
			control.getMessageList().clear();
			control.start();
			isrecording.setFill(Color.LIGHTGREEN);
			recording.selectedProperty().set(true);
		}
		else {
			control.stop();
			isrecording.setFill(Color.LIGHTGRAY);
			recording.selectedProperty().set(false);
		}
	}

	@Override
	public void update(Status oldStat, Status newStat) {
		if(!modetrigger)
			return;

		if(!control.isCollecting()) {
			switch(triggerStartMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED)); break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE)); break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION)); break;
			}
		} else {
			if(triggerStopDelay>0)
			  isrecording.setFill(Color.LIGHTYELLOW);
			ExecutorService.get().schedule(new Runnable() {
				@Override
				public void run() {
					switch(triggerStopMode) {
					case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED)); break;
					case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE)); break;
					case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION)); break;
					}
				}
			}, triggerStopDelay, TimeUnit.SECONDS);
		}
	}

}
