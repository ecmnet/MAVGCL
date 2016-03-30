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

package deprectated.com.comino.flight.widgets.charts.xy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.MSTYPE;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;


public class XYChartWidgetDnD extends BorderPane implements IChartControl {


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


	private static int COLLECTOR_CYCLE = 50;
	private static int REFRESH_MS = 100;

	@FXML
	private LineChart<Number,Number> linechart;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<Float> scale;

	@FXML
	private ListView<String> keyfigurelist;


	@FXML
	private CheckBox normalize;

	@FXML
	private Button export;



	private XYChart.Series<Number,Number> series1;

	private Task<Integer> task;


	private IMAVController control;

	private int type1;
	private int type2;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private DoubleProperty scroll        = new SimpleDoubleProperty(0);

	private int resolution 	= 50;


	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;

	public XYChartWidgetDnD() {

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
						current_x_pt = 0;
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
				updateGraph(false);

			}
		});


	}

	@FXML
	private void initialize() {


		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);

		keyfigurelist.getItems().addAll(PRESET_NAMES);

		scale.getItems().addAll(SCALES);

		scale.getSelectionModel().select(2);

		xAxis.setLowerBound(-5);
		xAxis.setUpperBound(5);
		yAxis.setLowerBound(-5);
		yAxis.setUpperBound(5);

		xAxis.setTickUnit(1); yAxis.setTickUnit(1);

		linechart.prefHeightProperty().bind(heightProperty().subtract(10));



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

				updateGraph(true);

			}

		});


		export.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				saveAsPng(System.getProperty("user.home"));
			}

		});

		timeFrame.addListener((v, ov, nv) -> {

			this.current_x_pt = 0;

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
                	int series = Integer.parseInt(db.getString());
                		type1 = series;
        				series1.setName(PRESET_NAMES[type1]);
        				xAxis.setLabel(PRESETS[type1][0].getUnit());
        				yAxis.setLabel(PRESETS[type1][0].getUnit());
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
		File file = new File(path+"/xychart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}



	private void updateGraph(boolean refresh) {

		if(refresh) {
			series1.getData().clear();

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

			if(current_x_pt < 0) current_x_pt = 0;
		}

		List<DataModel> mList = control.getCollector().getModelList();

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x) {

				if(current_x_pt > current_x1_pt)
					current_x0_pt++;

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution) == 0) {



					if(type1>0)
						series1.getData().add(new XYChart.Data<Number,Number>(
								MSTYPE.getValue(mList.get(current_x_pt),PRESETS[type1][0]),
								MSTYPE.getValue(mList.get(current_x_pt),PRESETS[type1][1]))
								);


					if(current_x_pt > current_x1_pt) {
						current_x1_pt++;
						if(series1.getData().size()>0)
							series1.getData().remove(0);
					}
				}

				current_x_pt++;
			}
		}
	}


	public XYChartWidgetDnD setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		series1.setName(PRESET_NAMES[type1]);
		linechart.getData().add(series1);

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

	@Override
	public void refreshChart() {
		updateGraph(true);
	}

}
