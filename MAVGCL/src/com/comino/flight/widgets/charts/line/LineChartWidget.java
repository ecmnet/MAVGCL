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

package com.comino.flight.widgets.charts.line;

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
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;


public class LineChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,		MSTYPE.MSP_NONE,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDX, 		MSTYPE.MSP_NEDY,		MSTYPE.MSP_NEDZ		},
			{ MSTYPE.MSP_NEDX, 		MSTYPE.MSP_SPNEDX,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDY, 		MSTYPE.MSP_SPNEDY,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDVX, 	MSTYPE.MSP_NEDVY,		MSTYPE.MSP_NEDVZ	},
			{ MSTYPE.MSP_GLOBRELX,  MSTYPE.MSP_GLOBRELY,	MSTYPE.MSP_GLOBRELZ  },
			{ MSTYPE.MSP_GLOBRELVX, MSTYPE.MSP_GLOBRELVY,	MSTYPE.MSP_GLOBRELVZ },
			{ MSTYPE.MSP_LERRX, 	MSTYPE.MSP_LERRY,		MSTYPE.MSP_LERRZ	},
			{ MSTYPE.MSP_ANGLEX, 	MSTYPE.MSP_ANGLEY,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ACCX, 		MSTYPE.MSP_ACCY, 		MSTYPE.MSP_ACCZ 	},
			{ MSTYPE.MSP_GYROX, 	MSTYPE.MSP_GYROY, 		MSTYPE.MSP_GYROZ 	},
			{ MSTYPE.MSP_RAW_FLOWX, MSTYPE.MSP_RAW_FLOWY, 	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_VOLTAGE, 	MSTYPE.MSP_CURRENT, 	MSTYPE.MSP_NONE		},
	};

	private static String[] PRESET_NAMES = {
			"None",
			"Loc.Position",
			"Loc.PositionX",
			"Loc.PositionY",
			"Loc. Speed",
			"Loc.GPS.Position",
			"Loc.GPS.Speed",
			"Loc. Pos.Error",
			"Angle",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Flow",
			"Battery",

	};

	private static int COLLECTOR_CYCLE = 50;
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
	private ChoiceBox<String> cseries3;

	@FXML
	private ChoiceBox<String> preset;

	@FXML
	private Button export;


	private XYChart.Series<Number,Number> series1;
	private XYChart.Series<Number,Number> series2;
	private XYChart.Series<Number,Number> series3;

	private Task<Integer> task;

	private IMAVController control;

	private MSTYPE type1=MSTYPE.MSP_NONE;
	private MSTYPE type2=MSTYPE.MSP_NONE;
	private MSTYPE type3=MSTYPE.MSP_NONE;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private BooleanProperty isReplaying  = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);


	private int resolution_ms 	= 50;

	private int current_x_pt=0;
	private int replay_x_pt=0;
	private int current_x0_pt = 0;
	private int current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

	private double m_x0=0;

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

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					if(!isCollecting.get() && control.getCollector().isCollecting()) {
						series1.getData().clear();
						series2.getData().clear();
						series3.getData().clear();
						current_x_pt = 0; current_x0_pt=0;
					}

					isCollecting.set(control.getCollector().isCollecting());

					if((isCollecting.get() && control.isConnected()))
						updateValue(control.getCollector().getModelList().size());


					if(isReplaying.get()) {
						replay_x_pt += REFRESH_MS / COLLECTOR_CYCLE;
						if(replay_x_pt < control.getCollector().getModelList().size())
							updateValue(replay_x_pt);
						else
							isReplaying.set(false);;
					}

				}
				return control.getCollector().getModelList().size();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				updateGraph(false);
			}
		});

		setOnMousePressed(t -> {
			m_x0 = t.getSceneX();

		});

		setOnMouseDragged(t -> {
			int dx = (int)(m_x0 - t.getSceneX())/4;
			int old_x0 = current_x0_pt;
			current_x0_pt += dx;

			if(current_x0_pt<0)
				current_x0_pt=0;
			if(current_x0_pt >
			control.getCollector().getModelList().size()-timeFrame.intValue() * 1000 / COLLECTOR_CYCLE)
				current_x0_pt= old_x0;

			updateGraph(true);

			if(dx!=0)
				m_x0 = t.getSceneX();
		});

	}

	@FXML
	private void initialize() {

		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(timeFrame.intValue());

		xAxis.setTickLabelFormatter(new StringConverter<Number>() {

			@Override
			public String toString(Number o) {
				return Integer.toString((int)(Math.round(o.floatValue())));
			}

			@Override
			public Number fromString(String string) {
				// TODO Auto-generated method stub
				return null;
			}
		});


		linechart.prefWidthProperty().bind(widthProperty());
		linechart.prefHeightProperty().bind(heightProperty());

		cseries1.getItems().addAll(MSTYPE.getList());
		cseries2.getItems().addAll(MSTYPE.getList());
		cseries3.getItems().addAll(MSTYPE.getList());


		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);
		cseries3.getSelectionModel().select(0);


		preset.getItems().addAll(PRESET_NAMES);
		preset.getSelectionModel().select(0);

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = MSTYPE.values()[newValue.intValue()];
				series1.setName(type1.getDescription());
				linechart.setLegendVisible(true);
				updateGraph(true);

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2 = MSTYPE.values()[newValue.intValue()];
				series2.setName(type2.getDescription());
				linechart.setLegendVisible(true);
				updateGraph(true);

			}

		});

		cseries3.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type3 = MSTYPE.values()[newValue.intValue()];
				series3.setName(type3.getDescription());
				linechart.setLegendVisible(true);
				updateGraph(true);

			}

		});

		preset.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = PRESETS[newValue.intValue()][0];
				type2 = PRESETS[newValue.intValue()][1];
				type3 = PRESETS[newValue.intValue()][2];

				cseries1.getSelectionModel().select(type1.getDescription());
				cseries2.getSelectionModel().select(type2.getDescription());
				cseries3.getSelectionModel().select(type3.getDescription());

				series1.setName(type1.getDescription());
				series2.setName(type2.getDescription());
				series3.setName(type3.getDescription());

				linechart.setLegendVisible(true);

				updateGraph(true);
			}

		});

		export.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				saveAsPng(System.getProperty("user.home"));
			}

		});


		isReplaying.addListener((v, ov, nv) -> {
			if(isCollecting.get())
				return;

			if(nv.booleanValue() && !ov.booleanValue()) {
				series1.getData().clear();
				series2.getData().clear();
				series3.getData().clear();
				current_x_pt = 0; current_x0_pt=0;
				replay_x_pt = 0;
				setXAxisBounds(current_x0_pt,timeFrame.intValue() * 1000 / COLLECTOR_CYCLE);
			}

		});

		timeFrame.addListener((v, ov, nv) -> {

			this.current_x_pt = 0;

			if(nv.intValue() > 600) {
				resolution_ms = 500;
			}
			else if(nv.intValue() > 200) {
				resolution_ms = 200;
			}
			else if(nv.intValue() > 20) {
				resolution_ms = 100;
			}
			else
				resolution_ms = 50;

			xAxis.setTickUnit(resolution_ms/20);
			xAxis.setMinorTickCount(10);

			current_x0_pt = control.getCollector().getModelList().size() - nv.intValue() * 1000 / COLLECTOR_CYCLE;
			if(current_x0_pt < 0)
				current_x0_pt = 0;

			updateGraph(true);
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



	private void updateGraph(boolean refresh) {
		float dt_sec = 0;

		List<DataModel> mList = control.getCollector().getModelList();

		if(refresh) {
			series1.getData().clear();
			series2.getData().clear();
			series3.getData().clear();
			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;
			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			if(isReplaying.get())
				max_x = replay_x_pt;

			while(current_x_pt<max_x ) {

				if(current_x_pt > current_x1_pt)
					current_x0_pt++;


				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					if(current_x_pt > current_x1_pt) {
						current_x1_pt++;
						if(series1.getData().size()>0)
							series1.getData().remove(0);
						if(series2.getData().size()>0)
							series2.getData().remove(0);
						if(series3.getData().size()>0)
							series3.getData().remove(0);
						setXAxisBounds(current_x0_pt,current_x1_pt);

					}

					//System.out.println(current_x_pt+":"+current_x0_pt);

					dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

					if(dt_sec > xAxis.getLowerBound()) {

						if(type1!=MSTYPE.MSP_NONE)
							series1.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type1)));
						if(type2!=MSTYPE.MSP_NONE)
							series2.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type2)));
						if(type3!=MSTYPE.MSP_NONE)
							series3.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type3)));
					}

				}


				current_x_pt++;
			}
		}
	}

	private void setXAxisBounds(int lower_pt, int upper_pt) {
		xAxis.setLowerBound(lower_pt * COLLECTOR_CYCLE / 1000F);
		xAxis.setUpperBound(upper_pt * COLLECTOR_CYCLE / 1000f);
	}


	public LineChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);
		series3 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series3);
		this.control = control;


		series1.setName(type1.getDescription());
		series2.setName(type2.getDescription());
		series3.setName(type3.getDescription());

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
