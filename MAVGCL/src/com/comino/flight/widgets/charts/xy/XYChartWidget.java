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
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.flight.widgets.charts.line.XYDataPool;
import com.comino.mav.control.IMAVController;
import com.comino.msp.utils.MSPMathUtils;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
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

	private static String[][] PRESETS = {
			{  null		, null      },
			{ "LPOSX"	, "LPOSY" 	},
			{ "LPOSVX"	, "LPOSVY" 	},
			{ "VISIONX"	, "VISIONY" }
	};

	private final static String[] PRESET_NAMES = {
			"None",
			"Loc.Position",
			"Loc.Speed",
			"Vision Position",
	};

	private final static String[] SCALES = {
			"Auto", "0.2", "0.5","1", "2", "5", "10", "20","50", "100", "200"
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
	private ChoiceBox<KeyFigureMetaData> cseries1_x;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries1_y;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries2_x;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries2_y;

	@FXML
	private ChoiceBox<String> scale_select;

	@FXML
	private Slider rotation;

	@FXML
	private Label rot_label;

	@FXML
	private CheckBox normalize;

	@FXML
	private CheckBox auto_rotate;

	@FXML
	private Button export;

	@FXML
	private CheckBox force_zero;



	private volatile XYChart.Series<Number,Number> series1;
	private volatile XYChart.Series<Number,Number> series2;

	private AnimationTimer task;


	private IMAVController control;


	private KeyFigureMetaData type1_x=null;
	private KeyFigureMetaData type1_y=null;

	private KeyFigureMetaData type2_x=null;
	private KeyFigureMetaData type2_y=null;

	private StateProperties state = null;
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty   scroll       = new SimpleFloatProperty(0);

	private int resolution_ms 	= 50;
	private float scale = 0;


	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;

	private int frame_secs =30;

	private float rotation_rad = 0;

	private XYDataPool pool = null;

	private boolean refreshRequest = false;

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();
	private AnalysisModelService  dataService = AnalysisModelService.getInstance();

	public XYChartWidget() {

		FXMLLoadHelper.load(this, "XYChartWidget.fxml");

		this.state = StateProperties.getInstance();

		pool = new XYDataPool();

		task = new AnimationTimer() {
			@Override public void handle(long now) {
				updateGraph(refreshRequest);
			}
		};
	}

	@FXML
	private void initialize() {

		linechart.lookup(".chart-plot-background").setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					System.out.println(xAxis.getValueForDisplay(click.getX())+":"
							+yAxis.getValueForDisplay(click.getY()));
				}
			}
		});

		xAxis.setAutoRanging(true);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(true);
		yAxis.setForceZeroInRange(false);

		cseries1.getItems().addAll(PRESET_NAMES);
		cseries2.getItems().addAll(PRESET_NAMES);

		linechart.setLegendVisible(false);
		linechart.prefWidthProperty().bind(heightProperty().subtract(20).multiply(1.05f));
		linechart.prefHeightProperty().bind(heightProperty().subtract(20));

		initKeyFigureSelection(meta.getKeyFigures());

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

				if(PRESETS[newValue.intValue()][0]!=null)
					cseries1_x.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][0]));
				else
					cseries1_x.getSelectionModel().select(0);

				if(PRESETS[newValue.intValue()][1]!=null)
					cseries1_y.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][1]));
				else
					cseries1_y.getSelectionModel().select(0);

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				if(PRESETS[newValue.intValue()][0]!=null)
					cseries2_x.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][0]));
				else
					cseries2_x.getSelectionModel().select(0);

				if(PRESETS[newValue.intValue()][1]!=null)
					cseries2_y.getSelectionModel().select(meta.getMetaData(PRESETS[newValue.intValue()][1]));
				else
					cseries2_y.getSelectionModel().select(0);

			}

		});

		cseries1_x.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String x_desc = "";
			if(nv!=null) {
				type1_x = nv;

				if(type1_x.hash!=0)
					x_desc = x_desc + type1_x.desc1+" ["+type1_x.uom+"] ";
				if(type2_x.hash!=0)
					x_desc = x_desc + type2_x.desc1+" ["+type2_x.uom+"] ";
				xAxis.setLabel(x_desc);

				updateRequest();
			}
		});


		cseries1_y.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String y_desc = "";
			if(nv!=null) {
				type1_y = nv;

				if(type1_y.hash!=0)
					y_desc = y_desc + type1_y.desc1+" ["+type1_y.uom+"] ";
				if(type2_y.hash!=0)
					y_desc = y_desc + type2_y.desc1+" ["+type2_y.uom+"] ";
				yAxis.setLabel(y_desc);

				updateRequest();
			}
		});

		cseries2_x.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String x_desc = "";
			if(nv!=null) {
				type2_x = nv;
				if(type1_x.hash!=0)
					x_desc = x_desc + type1_x.desc1+" ["+type1_x.uom+"] ";
				if(type2_x.hash!=0)
					x_desc = x_desc + type2_x.desc1+" ["+type2_x.uom+"] ";
				xAxis.setLabel(x_desc);

				updateRequest();
			}
		});


		cseries2_y.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			String y_desc = "";
			if(nv!=null) {
				type2_y = nv;
				if(type1_y.hash!=0)
					y_desc = y_desc + type1_y.desc1+" ["+type1_y.uom+"] ";
				if(type2_y.hash!=0)
					y_desc = y_desc + type2_y.desc1+" ["+type2_y.uom+"] ";
				yAxis.setLabel(y_desc);

				updateRequest();
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
				updateRequest();
			}

		});

		rotation.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				auto_rotate.setSelected(false);
				rotation_rad = MSPMathUtils.toRad(new_val.intValue());
				rot_label.setText("Rotation: ["+new_val.intValue()+"°]");
				updateRequest();

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
			updateRequest();
		});


		scroll.addListener((v, ov, nv) -> {
			current_x0_pt =  dataService.calculateX0Index(nv.floatValue());
			updateRequest();
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
		if(frame < 240) {
			this.frame_secs = frame;
			resolution_ms = 50;
			scroll.setValue(1);
			refreshChart();
		}
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

		AnalysisDataModel m =null;

		if(disabledProperty().get())
			return;

		if(refresh) {
			synchronized(this) {
				refreshRequest = false;
				series1.getData().clear();
				series2.getData().clear();
				pool.invalidateAll();
			}

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

			if(current_x_pt < 0) current_x_pt = 0;
		}

		List<AnalysisDataModel> mList = dataService.getModelList();

		if(force_zero.isSelected() && scale > 0 && current_x_pt > 0) {
			float x = 0; float y = 0;

			if(type1_x.hash!=0 && type2_x.hash==0)	{
				x = getAverage(mList,type1_x);
				y = getAverage(mList,type1_y);
			}

			if(type2_x.hash!=0 && type1_x.hash==0)	{
				x = getAverage(mList,type2_x);
				y = getAverage(mList,type2_y);
			}

			if(type2_x.hash!=0 && type1_x.hash!=0)	{
				x = ( getAverage(mList,type2_x) + getAverage(mList,type1_x) ) / 2f;
				y = ( getAverage(mList,type2_y) + getAverage(mList,type1_y) ) / 2f;
			}
				x = (int)(x *  scale) / (scale); y = (int)(y *  scale) / (scale);
				xAxis.setLowerBound(x-scale);
				xAxis.setUpperBound(x+scale);
				yAxis.setLowerBound(y-scale);
				yAxis.setUpperBound(y+scale);

		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!state.getRecordingProperty().get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x) {

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					m = mList.get(current_x_pt);

					if(current_x_pt > current_x1_pt) {

						current_x0_pt += resolution_ms / COLLECTOR_CYCLE;
						current_x1_pt += resolution_ms / COLLECTOR_CYCLE;

						if(series1.getData().size()>0) {
							pool.invalidate(series1.getData().get(0));
							series1.getData().remove(0);
						}
						if(series2.getData().size()>0) {
							pool.invalidate(series2.getData().get(0));
							series2.getData().remove(0);
						}
					}

					if(auto_rotate.isSelected()) {
						rotation_rad= -MSPMathUtils.toRad(m.getValue("HEAD"));
					}


					if(type1_x.hash!=0 && type1_y.hash!=0) {
						if(rotation_rad==0) {
							series1.getData().add(pool.checkOut(
									m.getValue(type1_x), m.getValue(type1_y))
									);
						} else {
							float[] r = rotateRad(m.getValue(type1_x), m.getValue(type1_y),
									rotation_rad);
							series1.getData().add(pool.checkOut(r[0],r[1]));
						}
					}

					if(type2_x.hash!=0 && type2_y.hash!=0) {
						if(rotation_rad==0) {
							series2.getData().add(pool.checkOut(
									m.getValue(type2_x), m.getValue(type2_y))
									);
						} else {
							float[] r = rotateRad(m.getValue(type2_x), m.getValue(type2_y),
									rotation_rad);
							series2.getData().add(pool.checkOut(r[0],r[1]));
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

		state.getRecordingProperty().addListener((o,ov,nv) -> {
			if(nv.booleanValue()) {
				current_x0_pt = 0;
				setXResolution(timeFrame.get());
				scroll.setValue(0);
				task.start();
			} else
				task.stop();
		});

		current_x1_pt =  timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

		return this;
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

		updateRequest();
	}

	private void updateRequest() {
		if(!isDisabled()) {
			if(dataService.isCollecting())
				refreshRequest = true;
			else {
				Platform.runLater(() -> {
					updateGraph(true);
				});
			}
		}
	}



	private void setScaling(float scale) {
		if(scale>0) {

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

	private void initKeyFigureSelection(List<KeyFigureMetaData> kfl) {

		cseries1_x.getItems().clear();
		cseries2_x.getItems().clear();
		cseries1_y.getItems().clear();
		cseries2_y.getItems().clear();

		type1_x = new KeyFigureMetaData();
		type2_x = new KeyFigureMetaData();
		type1_y = new KeyFigureMetaData();
		type2_y = new KeyFigureMetaData();

		cseries1_x.getItems().add((type1_x));
		cseries1_x.getItems().addAll(kfl);
		cseries1_y.getItems().add((type1_y));
		cseries1_y.getItems().addAll(kfl);
		cseries1_x.getSelectionModel().select(0);
		cseries1_y.getSelectionModel().select(0);

		cseries2_x.getItems().add((type2_x));
		cseries2_x.getItems().addAll(kfl);
		cseries2_y.getItems().add((type1_y));
		cseries2_y.getItems().addAll(kfl);
		cseries2_x.getSelectionModel().select(0);
		cseries2_y.getSelectionModel().select(0);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);
	}

	private float getAverage(List<AnalysisDataModel> list, KeyFigureMetaData f) {
		float average = 0;
		if(list.size() < 10)
			return 0;
		for(int i = current_x0_pt; i< current_x1_pt && i < list.size();i++)
			average +=list.get(i).getValue(f);
		average = average / (current_x1_pt - current_x0_pt);
		return average;
	}

}
