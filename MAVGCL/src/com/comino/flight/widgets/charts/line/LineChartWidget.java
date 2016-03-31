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

package com.comino.flight.widgets.charts.line;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.MovingAxis;
import com.comino.flight.widgets.SectionLineChart;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;


public class LineChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,		MSTYPE.MSP_NONE,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDX, 		MSTYPE.MSP_NEDY,		MSTYPE.MSP_NEDZ		},
			{ MSTYPE.MSP_NEDX, 		MSTYPE.MSP_SPNEDX,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDY, 		MSTYPE.MSP_SPNEDY,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDZ, 		MSTYPE.MSP_SPNEDZ,		MSTYPE.MSP_NONE 	},
			{ MSTYPE.MSP_NEDVX, 	MSTYPE.MSP_NEDVY,		MSTYPE.MSP_NEDVZ	},
			{ MSTYPE.MSP_GLOBRELX,  MSTYPE.MSP_GLOBRELY,	MSTYPE.MSP_GLOBRELZ  },
			{ MSTYPE.MSP_GLOBRELVX, MSTYPE.MSP_GLOBRELVY,	MSTYPE.MSP_GLOBRELVZ },
			{ MSTYPE.MSP_LERRX, 	MSTYPE.MSP_LERRY,		MSTYPE.MSP_LERRZ	 },
			{ MSTYPE.MSP_ANGLEX, 	MSTYPE.MSP_ANGLEY,		MSTYPE.MSP_NONE		 },
			{ MSTYPE.MSP_DEBUGX,    MSTYPE.MSP_DEBUGY,      MSTYPE.MSP_DEBUGZ    },
			{ MSTYPE.MSP_ACCX, 		MSTYPE.MSP_ACCY, 		MSTYPE.MSP_ACCZ 	},
			{ MSTYPE.MSP_GYROX, 	MSTYPE.MSP_GYROY, 		MSTYPE.MSP_GYROZ 	},
			{ MSTYPE.MSP_MAGX,	    MSTYPE.MSP_MAGY, 		MSTYPE.MSP_MAGZ		},
			{ MSTYPE.MSP_RAW_FLOWX, MSTYPE.MSP_RAW_FLOWY, 	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_VOLTAGE, 	MSTYPE.MSP_CURRENT, 	MSTYPE.MSP_NONE		},
	};

	private static String[] PRESET_NAMES = {
			"None",
			"Loc.Pos.NED",
			"Loc.Pos.NED X",
			"Loc.Pos.NED Y",
			"Loc.Pos.NED Z",
			"Loc. Speed",
			"Rel.GPS.Position",
			"Rel.GPS.Speed",
			"Loc.Pos.Error",
			"Angle",
			"Debug Values",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Magnetometer",
			"Raw Flow",
			"Battery",

	};

	private static int COLLECTOR_CYCLE = 50;

	@FXML
	private SectionLineChart<Number, Number> linechart;

	@FXML
	private MovingAxis xAxis;

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

	private ModeAnnotation posHoldAnnotation;
	private ModeAnnotation altHoldAnnotation;


	private XYChart.Series<Number,Number> series1;
	private XYChart.Series<Number,Number> series2;
	private XYChart.Series<Number,Number> series3;

	private Task<Integer> task;

	private IMAVController control;

	private MSTYPE type1=MSTYPE.MSP_NONE;
	private MSTYPE type2=MSTYPE.MSP_NONE;
	private MSTYPE type3=MSTYPE.MSP_NONE;


	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private DoubleProperty  scroll       = new SimpleDoubleProperty(0);


	private int resolution_ms 	= 50;

	private int current_x_pt=0;

	private int current_x0_pt = 0;
	private int current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

	private int frame_secs = 30;



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
						Thread.sleep(100);
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
							series3.getData().clear();
						}
						current_x_pt = 0; current_x0_pt=0;
						setXAxisBounds(current_x0_pt,timeFrame.intValue() * 1000 / COLLECTOR_CYCLE);
						scroll.setValue(0);
						updateGraph(true);
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

	}

	@FXML
	private void initialize() {

		xAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setLabel("Seconds");
		xAxis.setUpperBound(timeFrame.intValue());


		//		linechart.getAnnotations().add(posHoldAnnotation,Layer.FOREGROUND);
		//		linechart.getAnnotations().add(altHoldAnnotation,Layer.FOREGROUND);
		//
		//		posHoldAnnotation = new ModeAnnotation(0, 0, Orientation.VERTICAL, 0, null, new Color(0, 1, 0, 0.1));
		//		altHoldAnnotation = new ModeAnnotation(0, 3, Orientation.VERTICAL, 0, null, new Color(1, 0, 0, 0.1));


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
				series1.setName(type1.getDescription()+" ["+type1.getUnit()+"]   ");
				linechart.setLegendVisible(true);
				updateGraph(true);

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2 = MSTYPE.values()[newValue.intValue()];
				series2.setName(type2.getDescription()+" ["+type2.getUnit()+"]   ");
				linechart.setLegendVisible(true);
				updateGraph(true);

			}

		});

		cseries3.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type3 = MSTYPE.values()[newValue.intValue()];
				series3.setName(type3.getDescription()+" ["+type3.getUnit()+"]   ");
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

				series1.setName(type1.getDescription()+" ["+type1.getUnit()+"]   ");
				series2.setName(type2.getDescription()+" ["+type2.getUnit()+"]   ");
				series3.setName(type3.getDescription()+" ["+type3.getUnit()+"]   ");


				linechart.setLegendVisible(true);

				updateGraph(true);
			}

		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});


		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});


		scroll.addListener((v, ov, nv) -> {
			if(!isCollecting.get()) {

				current_x0_pt = (int)(
						( control.getCollector().getModelList().size()  - timeFrame.get() *  1000f / COLLECTOR_CYCLE)
						* nv.doubleValue())	;

				if(current_x0_pt<0)
					current_x0_pt = 0;

				if(!disabledProperty().get())
					updateGraph(true);

			}
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x_pt = 0;
				scroll.setValue(0);
				refreshChart();
			}
		});




	}


	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = linechart.snapshot(param, null);
		File file = new File(path+"/chart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}

	private void setXResolution(int frame) {
		this.current_x_pt = 0;
		this.frame_secs = frame;

		if(frame > 600)
			resolution_ms = 500;
		else if(frame > 200)
			resolution_ms = 250;
		else if(frame > 30)
			resolution_ms = 200;
		else if(frame > 20)
			resolution_ms = 100;
		else
			resolution_ms = 50;

		xAxis.setTickUnit(resolution_ms/20);
		xAxis.setMinorTickCount(10);

		scroll.setValue(0);
		xAxis.setLabel("Seconds ("+resolution_ms+"ms)");
		refreshChart();
	}



	private void updateGraph(boolean refresh) {
		float dt_sec = 0;

		List<DataModel> mList = control.getCollector().getModelList();


		if(refresh) {
			synchronized(this) {
				series1.getData().clear();
				series2.getData().clear();
				series3.getData().clear();

			}
			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x ) {

				dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					if(current_x_pt > current_x1_pt) {

						current_x0_pt += resolution_ms / COLLECTOR_CYCLE;
						current_x1_pt += resolution_ms / COLLECTOR_CYCLE;

						if(series1.getData().size()>0)
							series1.getData().remove(0);
						if(series2.getData().size()>0)
							series2.getData().remove(0);
						if(series3.getData().size()>0)
							series3.getData().remove(0);

						setXAxisBounds(current_x0_pt,current_x1_pt);
					}

					if(type1!=MSTYPE.MSP_NONE)
						series1.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type1)));
					if(type2!=MSTYPE.MSP_NONE)
						series2.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type2)));
					if(type3!=MSTYPE.MSP_NONE)
						series3.getData().add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(mList.get(current_x_pt),type3)));
				}


				current_x_pt++;
			}
		}
	}

	private synchronized void setXAxisBounds(int lower_pt, int upper_pt) {
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

		setXResolution(30);

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
		current_x0_pt = control.getCollector().getModelList().size() - frame_secs * 1000 / COLLECTOR_CYCLE;
		if(current_x0_pt < 0)
			current_x0_pt = 0;
		if(!disabledProperty().get())
		  updateGraph(true);
	}




}
