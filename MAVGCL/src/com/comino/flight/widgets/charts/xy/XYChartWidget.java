/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.charts.xy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
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
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;


public class XYChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,			MSTYPE.MSP_NONE							},
			{ MSTYPE.MSP_RNEDX, 		MSTYPE.MSP_RNEDY,		},
			{ MSTYPE.MSP_NEDVX, 		MSTYPE.MSP_NEDVY,		},
			{ MSTYPE.MSP_LERRX, 		MSTYPE.MSP_LERRY,		},
			{ MSTYPE.MSP_GLOBRELX,      MSTYPE.MSP_GLOBRELY, },
			{ MSTYPE.MSP_GLOBRELVX,     MSTYPE.MSP_GLOBRELVY, },
			{ MSTYPE.MSP_ANGLEX, 		MSTYPE.MSP_ANGLEY,	},
			{ MSTYPE.MSP_DEBUGX,        MSTYPE.MSP_DEBUGY, },
			{ MSTYPE.MSP_ACCX, 			MSTYPE.MSP_ACCY, 		},
			{ MSTYPE.MSP_GYROX, 		MSTYPE.MSP_GYROY, 	},
			{ MSTYPE.MSP_RAW_FLOWX, 	MSTYPE.MSP_RAW_FLOWY, },
			{ MSTYPE.MSP_MAGX, 			MSTYPE.MSP_MAGY, },
	};

	private final static String[] PRESET_NAMES = {
			"None",
			"Loc.Pos.rel.",
			"Loc.Speed",
			"Loc.Pos.Error",
			"Glob.Pos.rel",
			"Glob.Speed",
			"Angle",
			"Debug XY",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Flow",
			"Magnetic Field XY"
	};

	private final static String[] SCALES = {
			"Auto", "0.2", "0.5","1", "2", "5", "10", "50", "100", "200"
	};


	private static int COLLECTOR_CYCLE = 50;

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
	private ChoiceBox<String> cseries1_x;

	@FXML
	private ChoiceBox<String> cseries1_y;

	@FXML
	private ChoiceBox<String> cseries2_x;

	@FXML
	private ChoiceBox<String> cseries2_y;

	@FXML
	private ChoiceBox<String> scale_select;

	@FXML
	private Slider rotation;

	@FXML
	private Label rot_label;

	@FXML
	private CheckBox normalize;

	@FXML
	private Button export;

	@FXML
	private CheckBox force_zero;



	private volatile XYChart.Series<Number,Number> series1;
	private volatile XYChart.Series<Number,Number> series2;

	private Task<Integer> task;


	private IMAVController control;


	private MSTYPE type1_x=MSTYPE.MSP_NONE;
	private MSTYPE type1_y=MSTYPE.MSP_NONE;

	private MSTYPE type2_x=MSTYPE.MSP_NONE;
	private MSTYPE type2_y=MSTYPE.MSP_NONE;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty   scroll       = new SimpleFloatProperty(0);

	private int resolution_ms 	= 50;
	private float scale = 0;


	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;

	private int frame_secs =30;

	private float rotation_rad = 0;

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
						Thread.sleep(50);
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
						synchronized(this) {
							series1.getData().clear();
							series2.getData().clear();
						}
						current_x_pt = 0;
						scroll.setValue(0);
						updateGraph(true);
					}

					isCollecting.set(control.getCollector().isCollecting());

					if(isCollecting.get() && control.isConnected())
						Platform.runLater(() -> {
							updateGraph(false);
						});
				}
				return control.getCollector().getModelList().size();
			}
		};

		xAxis.forceZeroInRangeProperty().bind(force_zero.selectedProperty());
		yAxis.forceZeroInRangeProperty().bind(force_zero.selectedProperty());

	}

	@FXML
	private void initialize() {


		xAxis.setAutoRanging(true);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(true);
		yAxis.setForceZeroInRange(false);

		cseries1.getItems().addAll(PRESET_NAMES);
		cseries2.getItems().addAll(PRESET_NAMES);

		linechart.setLegendVisible(false);
		linechart.prefWidthProperty().bind(heightProperty().multiply(1.05f));
		linechart.prefHeightProperty().bind(heightProperty());

		cseries1_x.getItems().addAll(MSTYPE.getList());
		cseries1_y.getItems().addAll(MSTYPE.getList());
		cseries1_x.getSelectionModel().select(0);
		cseries1_y.getSelectionModel().select(0);

		cseries2_x.getItems().addAll(MSTYPE.getList());
		cseries2_y.getItems().addAll(MSTYPE.getList());
		cseries2_x.getSelectionModel().select(0);
		cseries2_y.getSelectionModel().select(0);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);

		scale_select.getItems().addAll(SCALES);
		scale_select.getSelectionModel().select(0);

		xAxis.setLowerBound(-5);
		xAxis.setUpperBound(5);
		yAxis.setLowerBound(-5);
		yAxis.setUpperBound(5);

		xAxis.setTickUnit(1); yAxis.setTickUnit(1);

		//		center_origin.setDisable(true);

		linechart.prefHeightProperty().bind(heightProperty().subtract(10));

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				cseries1_x.getSelectionModel().select(PRESETS[newValue.intValue()][0].getDescription());
				cseries1_y.getSelectionModel().select(PRESETS[newValue.intValue()][1].getDescription());


			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				cseries2_x.getSelectionModel().select(PRESETS[newValue.intValue()][0].getDescription());
				cseries2_y.getSelectionModel().select(PRESETS[newValue.intValue()][1].getDescription());


			}

		});

		cseries1_x.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1_x = MSTYPE.values()[newValue.intValue()];

				String x_desc = "";
				if(type1_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type1_x.getDescription()+" ["+type1_x.getUnit()+"]  ";


				if(type2_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type2_x.getDescription()+" ["+type2_x.getUnit()+"]  ";

				xAxis.setLabel(x_desc);
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}

		});

		cseries1_y.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1_y = MSTYPE.values()[newValue.intValue()];

				String y_desc = "";
				if(type1_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type1_y.getDescription()+" ["+type1_y.getUnit()+"]  ";

				if(type2_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type2_y.getDescription()+" ["+type2_y.getUnit()+"]  ";

				yAxis.setLabel(y_desc);

				Platform.runLater(() -> {
					updateGraph(true);
				});

			}

		});

		cseries2_x.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2_x = MSTYPE.values()[newValue.intValue()];

				String x_desc = "";
				if(type1_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type1_x.getDescription()+" ["+type1_x.getUnit()+"]  ";

				if(type2_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type2_x.getDescription()+" ["+type2_x.getUnit()+"]  ";

				xAxis.setLabel(x_desc);
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}

		});

		cseries2_y.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2_y = MSTYPE.values()[newValue.intValue()];

				String y_desc = "";
				if(type1_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type1_y.getDescription()+" ["+type1_y.getUnit()+"]  ";

				if(type2_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type2_y.getDescription()+" ["+type2_y.getUnit()+"]  ";

				yAxis.setLabel(y_desc);
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}

		});

		scale_select.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if(newValue.intValue()>0)
					scale = Float.parseFloat(SCALES[newValue.intValue()]);
				else
					scale = 0;
				setScaling(scale);
				Platform.runLater(() -> {
					updateGraph(true);
				});
			}

		});

		rotation.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				rotation_rad = Utils.toRad(new_val.intValue());
				rot_label.setText("Rotation: ["+new_val.intValue()+"°]");
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}
		});

		rotation.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					rotation_rad = 0;
					rotation.setValue(0);
					rot_label.setText("Rotation: [ 0°]");
				}
			}
		});

		rotation.setTooltip(new Tooltip("Double-click to set to 0°"));


		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});

		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});

		force_zero.setOnAction((ActionEvent event)-> {
			refreshChart();
		});


		scroll.addListener((v, ov, nv) -> {

			current_x0_pt = control.getCollector().calculateX0Index(nv.floatValue());

			if(!disabledProperty().get())
				Platform.runLater(() -> {
					updateGraph(true);
				});
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x_pt = 0;
				scroll.setValue(1);
				refreshChart();
			}
		});
	}

	private void setXResolution(int frame) {
		this.current_x_pt = 0;
		this.frame_secs = frame;
		resolution_ms = 50;
		scroll.setValue(1);
		refreshChart();
	}

	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = linechart.snapshot(param, null);
		File file = new File(path+"/xychart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}



	private void updateGraph(boolean refresh) {

		if(refresh) {
			synchronized(this) {
				series1.getData().clear();
				series2.getData().clear();

			}

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

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					if(current_x_pt > current_x1_pt) {

						current_x0_pt += resolution_ms / COLLECTOR_CYCLE;
						current_x1_pt += resolution_ms / COLLECTOR_CYCLE;

						if(series1.getData().size()>0)
							series1.getData().remove(0);
						if(series2.getData().size()>0)
							series2.getData().remove(0);
					}


					if(type1_x!=MSTYPE.MSP_NONE && type1_y!=MSTYPE.MSP_NONE) {
						if(rotation_rad==0) {
							series1.getData().add(new XYChart.Data<Number,Number>(
									MSTYPE.getValue(mList.get(current_x_pt),type1_x),
									MSTYPE.getValue(mList.get(current_x_pt),type1_y))
									);
						} else {
							float[] r = rotateRad(MSTYPE.getValue(mList.get(current_x_pt),type1_x),
									MSTYPE.getValue(mList.get(current_x_pt),type1_y),
									rotation_rad);
							series1.getData().add(new XYChart.Data<Number,Number>(r[0],r[1]));
						}
					}

					if(type2_x!=MSTYPE.MSP_NONE && type2_y!=MSTYPE.MSP_NONE) {
						if(rotation_rad==0) {
							series2.getData().add(new XYChart.Data<Number,Number>(
									MSTYPE.getValue(mList.get(current_x_pt),type2_x),
									MSTYPE.getValue(mList.get(current_x_pt),type2_y))
									);
						} else {
							float[] r = rotateRad(MSTYPE.getValue(mList.get(current_x_pt),type2_x),
									MSTYPE.getValue(mList.get(current_x_pt),type2_y),
									rotation_rad);
							series2.getData().add(new XYChart.Data<Number,Number>(r[0],r[1]));

						}

					}

					current_x_pt++;
				}
			}
		}
	}


	public XYChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();

		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);

		this.control = control;

		xAxis.setLowerBound(-1);
		xAxis.setUpperBound(+1);
		yAxis.setLowerBound(-1);
		yAxis.setUpperBound(+1);

		//ExecutorService.get().execute(task);
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public void refreshChart() {
		if(frame_secs > 60)
			frame_secs = 60;

		current_x0_pt = control.getCollector().calculateX0Index(1);
		setScaling(scale);

		if(!disabledProperty().get())
			Platform.runLater(() -> {
				updateGraph(true);
			});
	}


	private void setScaling(float scale) {
		if(scale>0) {
			force_zero.setDisable(true);
			xAxis.setAutoRanging(false);
			yAxis.setAutoRanging(false);

			xAxis.setLowerBound(-scale);
			xAxis.setUpperBound(+scale);
			yAxis.setLowerBound(-scale);
			yAxis.setUpperBound(+scale);

			if(scale>10) {
				xAxis.setTickUnit(10); yAxis.setTickUnit(10);
			} else if(scale>2) {
				xAxis.setTickUnit(1); yAxis.setTickUnit(1);
			} else if(scale>0.5f) {
				xAxis.setTickUnit(0.5); yAxis.setTickUnit(0.5);
			} else {
				xAxis.setTickUnit(0.1); yAxis.setTickUnit(0.1);
			}
		} else {
			force_zero.setDisable(false);
			xAxis.setAutoRanging(true);
			yAxis.setAutoRanging(true);
		}

	}

	private  float[] rotateRad(float posx, float posy, float heading_rad) {
		float[] rotated = new float[2];
		rotated[0] =  posx * (float)Math.cos(heading_rad) + posy * (float)Math.sin(heading_rad);
		rotated[1] = -posx * (float)Math.sin(heading_rad) + posy * (float)Math.cos(heading_rad);
		return rotated;
	}


}
