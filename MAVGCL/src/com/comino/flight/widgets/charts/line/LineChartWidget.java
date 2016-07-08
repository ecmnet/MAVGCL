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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.widgets.MovingAxis;
import com.comino.flight.widgets.SectionLineChart;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.emxsys.chart.extension.XYAnnotations.Layer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;


public class LineChartWidget extends BorderPane implements IChartControl {

	private static int MAXRECENT = 20;


	private static int COLLECTOR_CYCLE = 50;
	private static int REFRESH_RATE    = 50;

	@FXML
	private SectionLineChart<Number, Number> linechart;

	@FXML
	private MovingAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> group;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries1;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries2;

	@FXML
	private ChoiceBox<KeyFigureMetaData> cseries3;

	@FXML
	private Button export;

	@FXML
	private CheckBox annotations;


	private  XYChart.Series<Number,Number> series1;
	private  XYChart.Series<Number,Number> series2;
	private  XYChart.Series<Number,Number> series3;

	private Task<Integer> task;

	private IMAVController control;


	private KeyFigureMetaData type1 = null;
	private KeyFigureMetaData type2=  null;
	private KeyFigureMetaData type3=  null;


	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty  scroll        = new SimpleFloatProperty(0);


	private int resolution_ms 	= 50;

	private int current_x_pt=0;

	private int current_x0_pt = 0;
	private int current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();
	private AnalysisModelService  dataService = AnalysisModelService.getInstance();

	private ArrayList<KeyFigureMetaData> recent = null;

	private List<Data<Number,Number>> series1_list = new ArrayList<Data<Number,Number>>();
	private List<Data<Number,Number>> series2_list = new ArrayList<Data<Number,Number>>();
	private List<Data<Number,Number>> series3_list = new ArrayList<Data<Number,Number>>();

	private Gson gson = new GsonBuilder().create();
	private int   yoffset = 0;
	private int   last_annotation_pos = 0;

	public LineChartWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LineChartWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		readRecentList();
		if(recent==null)
			System.err.println("ERROR");

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {

				while(true) {

					LockSupport.parkNanos(REFRESH_RATE*1000000);

					if(isDisabled()) {
						LockSupport.parkNanos(500000000L);
						continue;
					}

					if (isCancelled())
						break;

					if(!isCollecting.get() && dataService.isCollecting()) {
						synchronized(this) {
							series1.getData().clear();
							series2.getData().clear();
							series3.getData().clear();
							current_x_pt = 0; current_x0_pt=0;
							setXAxisBounds(current_x0_pt,timeFrame.intValue() * 1000 / COLLECTOR_CYCLE);
							scroll.setValue(0);
						}
						updateGraph(false);
					}

					isCollecting.set(dataService.isCollecting());

					if(isCollecting.get() && control.isConnected())
						Platform.runLater(() -> {
							updateGraph(false);
						});
				}
				return 0;
			}
		};

	}

	@FXML
	private void initialize() {

		annotations.setSelected(true);

		annotations.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			Platform.runLater(() -> {
				updateGraph(true);
			});
		});

		xAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setLabel("Seconds");
		xAxis.setUpperBound(timeFrame.intValue());

		linechart.setLegendVisible(true);
		linechart.setLegendSide(Side.TOP);

		linechart.prefWidthProperty().bind(widthProperty());
		linechart.prefHeightProperty().bind(heightProperty());

		group.getItems().add("All");
		group.getItems().addAll(meta.getGroups());
		group.getItems().add("...used recently");
		group.getSelectionModel().select(0);

		initKeyFigureSelection(cseries1, type1, meta.getKeyFigures());
		initKeyFigureSelection(cseries2, type2, meta.getKeyFigures());
		initKeyFigureSelection(cseries3, type3, meta.getKeyFigures());

		type1 = type2 = type3 = new KeyFigureMetaData();

		group.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {

			if(nv.contains("All")) {
				initKeyFigureSelection(cseries1, type1, meta.getKeyFigures());
				initKeyFigureSelection(cseries2, type2, meta.getKeyFigures());
				initKeyFigureSelection(cseries3, type3, meta.getKeyFigures());
			} else if(nv.contains("recent")) {
				initKeyFigureSelection(cseries1, type1, recent);
				initKeyFigureSelection(cseries2, type2, recent);
				initKeyFigureSelection(cseries3, type3, recent);
			}
			else {
				initKeyFigureSelection(cseries1, type1, meta.getGroupMap().get(nv));
				initKeyFigureSelection(cseries2, type2, meta.getGroupMap().get(nv));
				initKeyFigureSelection(cseries3, type3, meta.getGroupMap().get(nv));
			}
		});


		cseries1.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			if(nv!=null && ov != nv) {
				if(nv.hash!=0) {
					addToRecent(nv);
					series1.setName(nv.desc1+" ["+nv.uom+"]   "); }
				else
					series1.setName(nv.desc1+"   ");
				type1 = nv;
				Platform.runLater(() -> {
					updateGraph(true);
				});
			}
		});

		cseries2.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			if(nv!=null && ov != nv) {
				if(nv.hash!=0) {
					addToRecent(nv);
					series2.setName(nv.desc1+" ["+nv.uom+"]   "); }
				else
					series2.setName(nv.desc1+"   ");
				type2 = nv;
				Platform.runLater(() -> {
					updateGraph(true);
				});
			}
		});

		cseries3.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			if(nv!=null && ov != nv) {
				if(nv.hash!=0) {
					addToRecent(nv);
					series3.setName(nv.desc1+" ["+nv.uom+"]   "); }
				else
					series3.setName(nv.desc1+"   ");
				type3 = nv;
				Platform.runLater(() -> {
					updateGraph(true);
				});
			}
		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});


		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});


		scroll.addListener((v, ov, nv) -> {

			current_x0_pt =  dataService.calculateX0Index(nv.floatValue());;

			if(!disabledProperty().get())
				Platform.runLater(() -> {
					updateGraph(true);
				});
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				scroll.setValue(0);
				refreshChart();
			}
		});

		annotations.setSelected(true);
	}


	private void addToRecent(KeyFigureMetaData nv) {
		if(recent.size()>MAXRECENT)
			recent.remove(0);
		if(!recent.contains(nv)) {
			recent.add(nv);
			storeRecentList();
		}
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

		if(frame > 600)
			resolution_ms = 2000;
		else if(frame > 200)
			resolution_ms = 500;
		else if(frame > 50)
			resolution_ms = 250;
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
		float dt_sec = 0; AnalysisDataModel m =null; int remove_count=0; boolean set_bounds = false;

		series1_list.clear();
		series2_list.clear();
		series3_list.clear();


		if(refresh) {
			series1.getData().clear();
			series2.getData().clear();
			series3.getData().clear();
			linechart.getAnnotations().clearAnnotations(Layer.FOREGROUND);
			last_annotation_pos = 0;
			yoffset = 0;

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;
			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt<dataService.getModelList().size() && dataService.getModelList().size()>0 ) {

			int max_x = dataService.getModelList().size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x ) {

				dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

				m = dataService.getModelList().get(current_x_pt);

				if(m.msg!=null && current_x_pt > 0 && m.msg.msg!=null && annotations.isSelected()) {
					if((current_x_pt - last_annotation_pos) > 150)
						yoffset=0;
					linechart.getAnnotations().add(new LineMessageAnnotation(dt_sec,yoffset++, m.msg, resolution_ms<200),
							    Layer.FOREGROUND);
					last_annotation_pos = current_x_pt;
				}

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0 && dt_sec > 0) {

					if(current_x_pt > current_x1_pt)
						remove_count++;

					if(type1.hash!=0)
						series1_list.add(new XYChart.Data<Number,Number>(dt_sec,m.getValue(type1)));
					if(type2.hash!=0)
						series2_list.add(new XYChart.Data<Number,Number>(dt_sec,m.getValue(type2)));
					if(type3.hash!=0)
						series3_list.add(new XYChart.Data<Number,Number>(dt_sec,m.getValue(type3)));
				}

				if(current_x_pt > current_x1_pt) {
					set_bounds = true;
					current_x0_pt += REFRESH_RATE/COLLECTOR_CYCLE;
					current_x1_pt += REFRESH_RATE/COLLECTOR_CYCLE;
				}
				current_x_pt++;
			}

			if(remove_count > 0) {
				if(series1.getData().size()>remove_count)
					series1.getData().remove(0, remove_count);
				if(series2.getData().size()>remove_count)
					series2.getData().remove(0, remove_count);
				if(series3.getData().size()>remove_count)
					series3.getData().remove(0, remove_count);
			}

			synchronized(this) {
				series1.getData().addAll(series1_list);
				series2.getData().addAll(series2_list);
				series3.getData().addAll(series3_list);
			}

			if(set_bounds)
				setXAxisBounds(current_x0_pt,current_x1_pt);
		}
	}

	private  void setXAxisBounds(int lower_pt, int upper_pt) {
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


		series1.setName(type1.desc1);
		series2.setName(type2.desc1);
		series3.setName(type3.desc1);

		setXResolution(30);

		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
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
		current_x0_pt = control.getCollector().calculateX0Index(1);
		if(!disabledProperty().get())
			Platform.runLater(() -> {
				updateGraph(true);
			});
	}

	private void initKeyFigureSelection(ChoiceBox<KeyFigureMetaData> series,KeyFigureMetaData type, List<KeyFigureMetaData> kfl) {

		KeyFigureMetaData none = new KeyFigureMetaData();

		Platform.runLater(() -> {

			series.getItems().clear();
			series.getItems().add(none);

			if(kfl.size()==0) {
				series.getSelectionModel().select(0);
				return;
			}

			if(type!=null && type.hash!=0) {
				if(!kfl.contains(type))
					series.getItems().add(type);
				series.getItems().addAll(kfl);
				series.getSelectionModel().select(type);
			} else {
				series.getItems().addAll(kfl);
				series.getSelectionModel().select(0);
			}

		});
	}

	private void storeRecentList() {
		Preferences prefs = MAVPreferences.getInstance();
		String rc = gson.toJson(recent);
		prefs.put(MAVPreferences.RECENT_FIGS, rc);
	}

	private void readRecentList() {
		Preferences prefs = MAVPreferences.getInstance();
		String rc = prefs.get(MAVPreferences.RECENT_FIGS, null);
		if(rc!=null)
			recent = gson.fromJson(rc, new TypeToken<ArrayList<KeyFigureMetaData>>() {}.getType());

		if(recent==null)
			recent = new ArrayList<KeyFigureMetaData>();

	}

}
