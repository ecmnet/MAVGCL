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

package com.comino.flight.widgets.charts.xy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;


public class XYChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,			MSTYPE.MSP_NONE							},
			{ MSTYPE.MSP_RNEDX, 		MSTYPE.MSP_RNEDY,		},
			{ MSTYPE.MSP_NEDVX, 		MSTYPE.MSP_NEDVY,		},
			{ MSTYPE.MSP_LERRX, 		MSTYPE.MSP_LERRY,		},
			{ MSTYPE.MSP_GLOBRELX,      MSTYPE.MSP_GLOBRELY, },
			{ MSTYPE.MSP_GLOBRELVX,     MSTYPE.MSP_GLOBRELVY, },
			{ MSTYPE.MSP_ANGLEX, 		MSTYPE.MSP_ANGLEY,	},
			{ MSTYPE.MSP_ACCX, 			MSTYPE.MSP_ACCY, 		},
			{ MSTYPE.MSP_GYROX, 		MSTYPE.MSP_GYROY, 	},
			{ MSTYPE.MSP_RAW_FLOWX, 	MSTYPE.MSP_RAW_FLOWY, },
	};

	private final static String[] PRESET_NAMES = {
			"None",
			"Loc.Pos.rel.",
			"Loc.Speed",
			"Loc.Pos.Error",
			"Loc.GPS.Pos",
			"Loc.GPS.Speed",
			"Angle",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Flow",
	};

	private final static Float[] SCALES = {
			1.0f, 2.0f, 5.0f, 10.0f, 50.0f, 100.0f
	};


	private static int COLLETCOR_CYCLE = 50;
	private static int REFRESH_MS = 100;

	@FXML
	private LineChart<Number,Number> linechart;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> cseries1;

	@FXML
	private ChoiceBox<String> cseries2;

	@FXML
	private ChoiceBox<Float> scale;


	@FXML
	private CheckBox normalize;

	@FXML
	private Button export;



	private XYChart.Series<Number,Number> series1;
	private XYChart.Series<Number,Number> series2;

	private Task<Integer> task;
	private int time=0;

	private IMAVController control;

	private int type1;
	private int type2;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private BooleanProperty isReplaying  = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);

	private int totalTime 	= 30;
	private int resolution 	= 50;
	private float time_max = totalTime * 1000 / COLLETCOR_CYCLE;
	private int   totalMax = 0;


	public XYChartWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("XYChartWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					try {
						Thread.sleep(REFRESH_MS);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					if(!isCollecting.get() && control.getCollector().isCollecting()) {
						series1.getData().clear();
						series2.getData().clear();
						time = 0;
					}

					isCollecting.set(control.getCollector().isCollecting());

					if(isCollecting.get() && control.isConnected())
						updateValue(control.getCollector().getModelList().size());


				}
				return control.getCollector().getModelList().size();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				totalMax = 999999;
				updateGraph();

			}
		});


	}

	@FXML
	private void initialize() {


		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);

		cseries1.getItems().addAll(PRESET_NAMES);
		cseries2.getItems().addAll(PRESET_NAMES);

		scale.getItems().addAll(SCALES);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);

		scale.getSelectionModel().select(2);

		xAxis.setLowerBound(-5);
		xAxis.setUpperBound(5);
		yAxis.setLowerBound(-5);
		yAxis.setUpperBound(5);

		xAxis.setTickUnit(1); yAxis.setTickUnit(1);

		linechart.prefHeightProperty().bind(heightProperty().subtract(10));

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = newValue.intValue();
				series1.setName(PRESET_NAMES[type1]);
				linechart.setLegendVisible(true);
				refreshGraph();

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2 = newValue.intValue();
				series2.setName(PRESET_NAMES[type2]);
				linechart.setLegendVisible(true);
				refreshGraph();

			}

		});

		scale.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				xAxis.setLowerBound(-SCALES[newValue.intValue()]);
				xAxis.setUpperBound(SCALES[newValue.intValue()]);
				yAxis.setLowerBound(-SCALES[newValue.intValue()]);
				yAxis.setUpperBound(SCALES[newValue.intValue()]);

				if(SCALES[newValue.intValue()]>10) {
					xAxis.setTickUnit(10); yAxis.setTickUnit(10);
				} else if(SCALES[newValue.intValue()]>1) {
					xAxis.setTickUnit(1); yAxis.setTickUnit(1);
				} else {
					xAxis.setTickUnit(0.5); yAxis.setTickUnit(0.5);
				}

				refreshGraph();

			}

		});


		export.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				saveAsPng(System.getProperty("user.home"));
			}

		});

		timeFrame.addListener((v, ov, nv) -> {

			this.totalTime = nv.intValue();
			this.time = 0;

			if(nv.intValue() > 600) {
				resolution = 500;
			}
			else if(nv.intValue() > 200) {
				resolution = 200;
			}
			else if(nv.intValue() > 20) {
				resolution = 100;
			}
			else
				resolution = 50;


			this.time_max = totalTime * 1000 / COLLETCOR_CYCLE;

			refreshGraph();
		});
	}

	public void saveAsPng(String path) {
		WritableImage image = linechart.snapshot(new SnapshotParameters(), null);
		File file = new File(path+"/chart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}

	private void refreshGraph() {
		series1.getData().clear();
		series2.getData().clear();

		time = control.getMessageList().size() - totalTime * 1000 / COLLETCOR_CYCLE;
		if(time < 0) time = 0;
		updateGraph();
	}


	private void updateGraph() {

		List<DataModel> mList = control.getCollector().getModelList();

		if(time<mList.size() && mList.size()>0 ) {


			while(time<mList.size() && time < totalMax) {



				if(((time * COLLETCOR_CYCLE) % resolution) == 0) {



					if(type1>0)
						series1.getData().add(new XYChart.Data<Number,Number>(
								MSTYPE.getValue(mList.get(time),PRESETS[type1][0]),
								MSTYPE.getValue(mList.get(time),PRESETS[type1][1]))
								);

					if(type2>0)
						series2.getData().add(new XYChart.Data<Number,Number>(
								MSTYPE.getValue(mList.get(time),PRESETS[type2][0]),
								MSTYPE.getValue(mList.get(time),PRESETS[type2][1]))
								);


					if(time > time_max) {
						if(series1.getData().size()>0)
							series1.getData().remove(0);
						if(series2.getData().size()>0)
							series2.getData().remove(0);

					}
				}

				time++;
			}
		}
	}


	public XYChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);


		this.control = control;

		series1.setName(PRESET_NAMES[type1]);
		series2.setName(PRESET_NAMES[type2]);


		ExecutorService.get().execute(task);
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}

	public BooleanProperty getReplayingProperty() {
		return isReplaying;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}


}
