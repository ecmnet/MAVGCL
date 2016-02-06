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

package com.comino.flight.widgets.linechart;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.analysiscontrol.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSPTypes;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


public class LineChartWidget extends VBox implements IChartControl {


	private static int[][] PRESETS = {
			{ 0,						0,						0					},
			{ MSPTypes.MSP_NEDX, 		MSPTypes.MSP_NEDY,		MSPTypes.MSP_NEDZ	},
			{ MSPTypes.MSP_NEDVX, 		MSPTypes.MSP_NEDVY,		MSPTypes.MSP_NEDVZ	},
			{ MSPTypes.MSP_LERRX, 		MSPTypes.MSP_LERRY,		MSPTypes.MSP_LERRZ	},
			{ MSPTypes.MSP_ANGLEX, 		MSPTypes.MSP_ANGLEY,	0					},
			{ MSPTypes.MSP_ACCX, 		MSPTypes.MSP_ACCY, 		MSPTypes.MSP_ACCZ 	},
			{ MSPTypes.MSP_GYROX, 		MSPTypes.MSP_GYROY, 	MSPTypes.MSP_GYROZ 	},
			{ MSPTypes.MSP_RAW_FLOWX, 	MSPTypes.MSP_RAW_FLOWY, 0 					},
			{ MSPTypes.MSP_VOLTAGE, 	MSPTypes.MSP_CURRENT, 0 					},
	};

	private static String[] PRESET_NAMES = {
			"None",
			"Loc.Position",
			"Loc. Speed",
			"Loc. Pos.Error",
			"Angle",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Flow",
			"Battery",

	};

	private static int COLLETCOR_CYCLE = 50;
	private static int REFRESH_MS = 200;

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
	private ChoiceBox<String> cseries3;

	@FXML
	private ChoiceBox<String> preset;

	@FXML
	private CheckBox normalize;

	@FXML
	private Button export;



	private XYChart.Series<Number,Number> series1;
	private XYChart.Series<Number,Number> series2;
	private XYChart.Series<Number,Number> series3;

	private Task<Integer> task;
	private int time=0;

	private IMAVController control;

	private int type1;
	private int type2;
	private int type3;

	private boolean isCollecting = false;

	private int totalTime 	= 30;
	private int resolution 	= 50;
	private float time_max = totalTime * 1000 / COLLETCOR_CYCLE;
	private int    totalMax = 0;

	public LineChartWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LineChartWidget.fxml"));
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


					if (isCancelled()) {
						break;
					}

					if(!isCollecting && control.getCollector().isCollecting()) {
						series1.getData().clear();
						series2.getData().clear();
						series3.getData().clear();
						time = 0;
					}

					isCollecting = control.getCollector().isCollecting();

					if(isCollecting && control.isConnected())
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

		VBox.setVgrow(this, Priority.ALWAYS);
		this.setFillWidth(true);

		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(totalTime);

		linechart.setPrefWidth(this.getPrefWidth()-50);

		cseries1.getItems().addAll(MSPTypes.getNames());
		cseries2.getItems().addAll(MSPTypes.getNames());
		cseries3.getItems().addAll(MSPTypes.getNames());


		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);
		cseries3.getSelectionModel().select(0);


		preset.getItems().addAll(PRESET_NAMES);
		preset.getSelectionModel().select(0);

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = newValue.intValue();
				series1.setName(MSPTypes.getNames()[type1]);
				linechart.setLegendVisible(true);
				refreshGraph();

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2 = newValue.intValue();
				series2.setName(MSPTypes.getNames()[type2]);
				linechart.setLegendVisible(true);
				refreshGraph();

			}

		});

		cseries3.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type3 = newValue.intValue();
				series3.setName(MSPTypes.getNames()[type3]);
				linechart.setLegendVisible(true);
				refreshGraph();

			}

		});

		preset.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = PRESETS[newValue.intValue()][0];
				type2 = PRESETS[newValue.intValue()][1];
				type3 = PRESETS[newValue.intValue()][2];

				cseries1.getSelectionModel().select(type1);
				cseries2.getSelectionModel().select(type2);
				cseries3.getSelectionModel().select(type3);

				series1.setName(MSPTypes.getNames()[type1]);
				series2.setName(MSPTypes.getNames()[type2]);
				series3.setName(MSPTypes.getNames()[type3]);

				linechart.setLegendVisible(true);

				refreshGraph();
			}

		});

		export.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				saveAsPng(System.getProperty("user.home"));
			}

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
		series3.getData().clear();

		time = control.getMessageList().size() - totalTime * 1000 / COLLETCOR_CYCLE;
		if(time < 0) time = 0;
		updateGraph();
	}


	private void updateGraph() {
		float dt_sec = 0;


		List<DataModel> mList = control.getCollector().getModelList();

		if(time==0) {
			xAxis.setLowerBound(0);
			xAxis.setUpperBound(time_max * COLLETCOR_CYCLE / 1000f);
		}

		if(time<mList.size() && mList.size()>0 ) {


			while(time<mList.size() && time < totalMax) {


				if(((time * COLLETCOR_CYCLE) % resolution) == 0) {

					dt_sec = time *  COLLETCOR_CYCLE / 1000f;

					if(type1>0)
						series1.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSPTypes.getFloat(mList.get(time),type1)));
					if(type2>0)
						series2.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSPTypes.getFloat(mList.get(time),type2)));
					if(type3>0)
						series3.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSPTypes.getFloat(mList.get(time),type3)));


					if(time > time_max) {
						if(series1.getData().size()>0)
							series1.getData().remove(0);
						if(series2.getData().size()>0)
							series2.getData().remove(0);
						if(series3.getData().size()>0)
							series3.getData().remove(0);
						xAxis.setLowerBound((time-time_max) * COLLETCOR_CYCLE / 1000F);
						xAxis.setUpperBound(time * COLLETCOR_CYCLE / 1000f);
					}
				}

				time++;
			}
		}
	}


	public LineChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);
		series3 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series3);
		this.control = control;


		series1.setName(MSPTypes.getNames()[type1]);
		series2.setName(MSPTypes.getNames()[type2]);
		series3.setName(MSPTypes.getNames()[type3]);

		ExecutorService.get().execute(task);
		return this;
	}



	@Override
	public void setTotalTime(int time_window) {
		this.totalTime = time_window;
		this.time = 0;

		if(time_window > 600) {
			resolution = 500;
		}
		else if(time_window > 200) {
			resolution = 200;
		}
		else if(time_window > 20) {
			resolution = 100;
		}
		else
			resolution = 50;

		xAxis.setTickUnit(resolution/20);
		xAxis.setMinorTickCount(10);

		this.time_max = totalTime * 1000 / COLLETCOR_CYCLE;
		refreshGraph();

	}

}
