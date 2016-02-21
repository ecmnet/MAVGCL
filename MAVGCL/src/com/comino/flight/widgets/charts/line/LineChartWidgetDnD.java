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

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;


public class LineChartWidgetDnD extends BorderPane implements IChartControl {


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
	private ChoiceBox<String> preset;

	@FXML
	private Button export;

	@FXML
	private Button remove;

	@FXML
	private ListView<String> keyfigurelist;

	private Map<MSTYPE,XYChart.Series<Number,Number>> series;



	private Task<Integer> task;

	private IMAVController control;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private DoubleProperty  scroll       = new SimpleDoubleProperty(0);


	private int resolution_ms 	= 50;

	private int current_x_pt=0;

	private int current_x0_pt = 0;
	private int current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;



	public LineChartWidgetDnD() {

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
						for(XYChart.Series<Number,Number> serie : series.values())
							serie.getData().clear();
						current_x_pt = 0; current_x0_pt=0;
						setXAxisBounds(current_x0_pt,timeFrame.intValue() * 1000 / COLLECTOR_CYCLE);
						scroll.setValue(0);
					}

					isCollecting.set(control.getCollector().isCollecting());

					if((isCollecting.get() && control.isConnected()))
						updateValue(control.getCollector().getModelList().size());


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

		series = new HashMap<MSTYPE,XYChart.Series<Number,Number>>();

	}

	@FXML
	private void initialize() {

		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setLabel("Seconds");
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


		keyfigurelist.getItems().addAll(MSTYPE.getList());
		keyfigurelist.setTooltip(new Tooltip("Drag a keyfigure to graph"));

		preset.getItems().addAll(PRESET_NAMES);
		preset.getSelectionModel().select(0);




		preset.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				series.clear();
				linechart.getData().clear();

				MSTYPE type1 = PRESETS[newValue.intValue()][0];
				addSeriesToChart(type1);

				MSTYPE type2 = PRESETS[newValue.intValue()][1];
				addSeriesToChart(type2);

				MSTYPE type3 = PRESETS[newValue.intValue()][2];
				addSeriesToChart(type3);

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

		remove.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				series.clear();
				linechart.getData().clear();
				updateGraph(true);
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
			scroll.setValue(0);
			updateGraph(true);
		});


		scroll.addListener((v, ov, nv) -> {
			if(!isCollecting.get()) {
				current_x0_pt = (int)(
						( control.getCollector().getModelList().size()  - timeFrame.get() *  1000f / COLLECTOR_CYCLE)
						* (1 - nv.intValue() / 100f))	;
				if(current_x0_pt<0)
					current_x0_pt = 0;
				updateGraph(true);
			}
		});

		keyfigurelist.setOnDragDetected(new EventHandler <MouseEvent>() {
			public void handle(MouseEvent event) {
				Dragboard db = keyfigurelist.startDragAndDrop(TransferMode.ANY);
				ClipboardContent content = new ClipboardContent();
				content.putString(Integer.toString(keyfigurelist.getSelectionModel().getSelectedIndex()));
				db.setContent(content);
				event.consume();
			}
		});


		linechart.setOnDragOver(new EventHandler <DragEvent>() {
			public void handle(DragEvent event) {
				if (event.getGestureSource() != linechart &&
						event.getDragboard().hasString()) {
					/* allow for both copying and moving, whatever user chooses */
					event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				event.consume();
			}
		});

		linechart.setOnDragDropped(new EventHandler <DragEvent>() {
			public void handle(DragEvent event) {

				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasString()) {
					addSeriesToChart(MSTYPE.getTypeOf(Integer.parseInt(db.getString())));
					linechart.setLegendVisible(true);
					success = true;
					updateGraph(true);
				}
				event.setDropCompleted(success);
				event.consume();
			}
		});


		keyfigurelist.setOnDragDone(new EventHandler <DragEvent>() {
			public void handle(DragEvent event) {
				if (event.getTransferMode() == TransferMode.MOVE) {
					keyfigurelist.getSelectionModel().clearSelection();
				}
				event.consume();
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



	private void updateGraph(boolean refresh) {
		float dt_sec = 0;

		List<DataModel> mList = control.getCollector().getModelList();


		if(refresh) {
			for(XYChart.Series<Number,Number> serie : series.values())
				serie.getData().clear();
			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;
			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x ) {

				if(current_x_pt > current_x1_pt)
					current_x0_pt++;


				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					if(current_x_pt > current_x1_pt) {
						current_x1_pt++;
						for(XYChart.Series<Number,Number> serie : series.values()) {
							if(serie.getData().size()>0)
								serie.getData().remove(0);
						}
						setXAxisBounds(current_x0_pt,current_x1_pt);

					}

					//System.out.println(current_x_pt+":"+current_x0_pt);

					dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

					if(dt_sec > xAxis.getLowerBound()) {

						for (Entry<MSTYPE,XYChart.Series<Number,Number>> entry : series.entrySet()) {
							entry.getValue().getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),
									entry.getKey())));
						}
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


	public LineChartWidgetDnD setup(IMAVController control) {
        this.control = control;
		ExecutorService.get().execute(task);
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}


	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public DoubleProperty getScrollProperty() {
		return scroll;
	}


	private void addSeriesToChart(MSTYPE type) {
		if(series.size()<6) {
			XYChart.Series<Number,Number> s = new XYChart.Series<Number,Number>();
			linechart.getData().add(s);
			s.setName(type.getDescription()+" ["+type.getUnit()+"]   ");
			series.put(type, s);
		}
	}

}
