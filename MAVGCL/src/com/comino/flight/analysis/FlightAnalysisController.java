package com.comino.flight.analysis;

import java.text.DecimalFormat;

import com.comino.flight.MainApp;
import com.comino.flight.widgets.analysiscontrol.AnalysisControlWidget;
import com.comino.flight.widgets.battery.BatteryWidget;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.linechart.LineChartWidget;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSPTypes;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class FlightAnalysisController {



	@FXML
	private StatusWidget status;

	@FXML
	private LineChartWidget chart1;

	@FXML
	private LineChartWidget chart2;

	@FXML
	private AnalysisControlWidget analysiscontrol;

	@FXML
	private BatteryWidget battery;

	@FXML
	private DetailsWidget details;

	private MainApp mainApp;
	private DataModel model;


	//	private XYChart.Series<Number,Number> series;

	private int time=0;

	private Task<Long> task;

	private IMAVController control;

	private static final DecimalFormat volt_f = new DecimalFormat("#0.0V");



	public FlightAnalysisController() {

		//		series = new XYChart.Series<Number,Number>();
		//
		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if (isCancelled()) {
						break;
					}
					updateValue(control.getCurrentModel().sys.tms);
				}
				return control.getCurrentModel().sys.tms;
			}
		};
		//
		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {



			}
		});




	}




	public void start(MainApp mainApp,IMAVController control) {
		this.mainApp = mainApp;
		this.control = control;
		this.model = control.getCurrentModel();
		analysiscontrol.addChart(chart1.setup(control));
		analysiscontrol.addChart(chart2.setup(control));
		analysiscontrol.setup(control);
		status.setup(control);
		battery.setup(control);
		details.setup(control);
		ExecutorService.get().execute(task);


	}



}
