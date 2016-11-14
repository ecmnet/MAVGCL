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
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.flight.widgets.fx.controls.MovingAxis;
import com.comino.flight.widgets.fx.controls.SectionLineChart;
import com.comino.mav.control.IMAVController;
import com.emxsys.chart.extension.XYAnnotations.Layer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public class LineChartWidget extends BorderPane implements IChartControl {

	private static int MAXRECENT = 20;

	private final static int COLLECTOR_CYCLE = 50;
	private final static int REFRESH_RATE    = 50;
	private final static int REFRESH_STEP    = REFRESH_RATE / COLLECTOR_CYCLE;

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
	private CheckBox averaging;

	@FXML
	private Button export;

	@FXML
	private CheckBox annotations;

	@FXML
	private CheckBox dash;

	private int id = 0;

	private  XYChart.Series<Number,Number> series1;
	private  XYChart.Series<Number,Number> series2;
	private  XYChart.Series<Number,Number> series3;

	private AnimationTimer task;

	private IMAVController control;

	private KeyFigureMetaData type1 = null;
	private KeyFigureMetaData type2=  null;
	private KeyFigureMetaData type3=  null;

	private StateProperties state = null;
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty  scroll        = new SimpleFloatProperty(0);

	private int resolution_ms 	= 50;

	private int current_x_pt      = 0;

	private int current_x0_pt     = 0;
	private int current_x1_pt     = 0;

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();
	private AnalysisModelService  dataService = AnalysisModelService.getInstance();

	private ArrayList<KeyFigureMetaData> recent = null;

	private Gson gson = new GsonBuilder().create();
	private int   yoffset = 0;
	private int   last_annotation_pos = 0;

	private double x;
	private float timeframe;
	private boolean display_annotations = true;
	private boolean isPaused = false;

	private DashBoardAnnotation dashboard1 = null;
	private DashBoardAnnotation dashboard2 = null;
	private DashBoardAnnotation dashboard3 = null;

	private XYDataPool pool = null;

	private Preferences prefs = MAVPreferences.getInstance();

	private boolean refreshRequest = false;


	public LineChartWidget() {

		FXMLLoadHelper.load(this, "LineChartWidget.fxml");

		this.state = StateProperties.getInstance();
		this.pool  = new XYDataPool();

		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);
		series3 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series3);

		task = new AnimationTimer() {
			long last = 0;
			@Override public void handle(long now) {
				if(!isDisabled() && (now - last) > (REFRESH_RATE*500000)) {
					updateGraph(refreshRequest);
					last = now;
				}
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

		dashboard1 = new DashBoardAnnotation(10);
		dashboard2 = new DashBoardAnnotation(90);
		dashboard3 = new DashBoardAnnotation(170);

		current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

		xAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setLabel("Seconds");
		xAxis.setUpperBound(timeFrame.intValue());
		yAxis.setPrefWidth(40);

		yAxis.setAutoRanging(true);

	    xAxis.setAnimated(false);
	    yAxis.setAnimated(false);

		linechart.setAnimated(false);
		linechart.setLegendVisible(true);
		linechart.setLegendSide(Side.TOP);
		linechart.setCache(true);

		linechart.prefWidthProperty().bind(widthProperty());
		linechart.prefHeightProperty().bind(heightProperty());

		final Group chartArea = (Group)linechart.getPlotArea();
		final Rectangle zoom = new Rectangle();
		zoom.setStrokeWidth(0);
		chartArea.getChildren().add(zoom);
		zoom.setFill(Color.color(0,0.6,1.0,0.1));
		zoom.setVisible(false);
		zoom.setY(0);
		zoom.setHeight(1000);

		linechart.setOnMousePressed(mouseEvent -> {
			if(dataService.isCollecting() && !isPaused)
				return;
			x = mouseEvent.getX();
			zoom.setX(x-chartArea.getLayoutX()-7);
		});

		linechart.setOnMouseReleased(mouseEvent -> {
			if(dataService.isCollecting() && !isPaused)
				return;

			linechart.setCursor(Cursor.DEFAULT);
			zoom.setVisible(false);
			double x0 = xAxis.getValueForDisplay(x-xAxis.getLayoutX()).doubleValue();
			double x1 = xAxis.getValueForDisplay(mouseEvent.getX()-xAxis.getLayoutX()).doubleValue();
			if((x1-x0)>1 && ( type1.hash!=0 || type2.hash!=0 || type3.hash!=0)) {

				current_x0_pt = (int)(x0 * 1000f / COLLECTOR_CYCLE);
				setXResolution((int)(x1-x0));
			}
			Platform.runLater(() -> {
				updateGraph(true);
			});
			mouseEvent.consume();
		});

		linechart.setOnMouseClicked(click -> {
			if (click.getClickCount() == 2) {
				if(dataService.isCollecting()) {
					if(isPaused) {
						current_x0_pt =  dataService.calculateX0Index(scroll.get());
						setXResolution(timeFrame.get());
						Platform.runLater(() -> {
							updateGraph(true);
						});
						task.start();
					}
					else {
						task.stop();
					}
					isPaused = !isPaused;
				}
				else {
					scroll.set(1);
					current_x0_pt =  dataService.calculateX0Index(scroll.get());
					setXResolution(timeFrame.get());
				}
			}
			click.consume();
		});

		linechart.setOnMouseDragged(mouseEvent -> {
			if(dataService.isCollecting() && !isPaused)
				return;

			if(type1.hash!=0 || type2.hash!=0 || type3.hash!=0) {
				zoom.setVisible(true);
				linechart.setCursor(Cursor.H_RESIZE);
				zoom.setWidth(mouseEvent.getX()-x);
			}
			mouseEvent.consume();
		});

		linechart.setOnScroll(event -> {

			if(dataService.isCollecting() && !isPaused)
				return;

			int delta = (int)(timeframe * 1000f / linechart.getWidth() * -event.getDeltaX() * 0.3f
					/ COLLECTOR_CYCLE + 0.5f);
			event.consume();

			current_x0_pt = current_x0_pt + delta;
			if(current_x0_pt<0)
				current_x0_pt=0;

			Platform.runLater(() -> {
				updateGraph(true);
			});
		});

		readRecentList();


		type1 = type2 = type3 = new KeyFigureMetaData();

		group.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {

			if(nv==null)
				return;

			if(nv.contains("All")) {
				initKeyFigureSelection(cseries1, type1, meta.getKeyFigures());
				initKeyFigureSelection(cseries2, type2, meta.getKeyFigures());
				initKeyFigureSelection(cseries3, type3, meta.getKeyFigures());
			} else if(nv.contains("used")) {
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
					series1.setName(nv.desc1+" ["+nv.uom+"]   ");
				}
				else
					series1.setName(nv.desc1+"   ");
				type1 = nv;
				prefs.putInt(MAVPreferences.LINECHART_FIG_1+id,nv.hash);
				updateRequest();
			}
		});

		cseries2.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			if(nv!=null && ov != nv) {
				if(nv.hash!=0) {
					addToRecent(nv);
					series2.setName(nv.desc1+" ["+nv.uom+"]   ");
				}
				else
					series2.setName(nv.desc1+"   ");
				type2 = nv;
				prefs.putInt(MAVPreferences.LINECHART_FIG_2+id,nv.hash);
				updateRequest();
			}
		});

		cseries3.getSelectionModel().selectedItemProperty().addListener((observable, ov, nv) -> {
			if(nv!=null && ov != nv) {
				if(nv.hash!=0) {
					addToRecent(nv);
					series3.setName(nv.desc1+" ["+nv.uom+"]   ");
				}
				else
					series3.setName(nv.desc1+"   ");
				type3 = nv;
				prefs.putInt(MAVPreferences.LINECHART_FIG_3+id,nv.hash);
				updateRequest();
			}
		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
			event.consume();
		});

		timeFrame.addListener((v, ov, nv) -> {
			this.current_x_pt = 0;
			xAxis.setTickUnit(resolution_ms/20);
			xAxis.setMinorTickCount(10);
			current_x0_pt =  dataService.calculateX0Index(1);
			setXResolution(timeFrame.get());
		});


		scroll.addListener((v, ov, nv) -> {
			current_x0_pt =  dataService.calculateX0Index(nv.floatValue());
			setXResolution(timeFrame.get());
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x0_pt = dataService.calculateX0Index(1);
				current_x0_pt = dataService.calculateX0Index(1);
				setXResolution(timeFrame.get());
			}
		});

		averaging.selectedProperty().addListener((v, ov, nv) -> {
			updateRequest();
		});

		dash.selectedProperty().addListener((v, ov, nv) -> {
			updateRequest();
		});

		annotations.setSelected(false);
	}

	public LineChartWidget setup(IMAVController control, int id) {
		this.id      = id;
		this.control = control;

		setXResolution(30);

		series1.setName(type1.desc1);
		series2.setName(type2.desc1);
		series3.setName(type3.desc1);

		state.getRecordingProperty().addListener((o,ov,nv) -> {
			if(nv.booleanValue()) {
				current_x0_pt = 0;
				setXResolution(timeFrame.get());
				scroll.setValue(0);
				task.start();
			} else
				task.stop();
		});

		KeyFigureMetaData k1 = meta.getKeyFigureMap().get(prefs.getInt(MAVPreferences.LINECHART_FIG_1+id,0));
		if(k1!=null) type1 = k1;
		KeyFigureMetaData k2 = meta.getKeyFigureMap().get(prefs.getInt(MAVPreferences.LINECHART_FIG_2+id,0));
		if(k2!=null) type2 = k2;
		KeyFigureMetaData k3 = meta.getKeyFigureMap().get(prefs.getInt(MAVPreferences.LINECHART_FIG_3+id,0));
		if(k3!=null) type3 = k3;

		meta.addObserver((o,e) -> {

			group.getItems().clear();

			if(e == null) {

				group.getItems().add("Last used...");
				group.getItems().add("All");
				group.getItems().addAll(meta.getGroups());
				group.getSelectionModel().select(0);

				initKeyFigureSelection(cseries1, type1, recent);
				initKeyFigureSelection(cseries2, type2, recent);
				initKeyFigureSelection(cseries3, type3, recent);

			} else {

				group.getItems().add("All");
				group.getItems().addAll(meta.getGroups());
				group.getSelectionModel().select(0);

				initKeyFigureSelection(cseries1, type1, meta.getKeyFigures());
				initKeyFigureSelection(cseries2, type2, meta.getKeyFigures());
				initKeyFigureSelection(cseries3, type3, meta.getKeyFigures());

			}

		});

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
		current_x0_pt = dataService.calculateX0Index(1);
		if(!isDisabled())
			updateRequest();
	}

	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = linechart.snapshot(param, null);
		File file = new File(path+"/chart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {  }
	}

	private void addToRecent(KeyFigureMetaData nv) {

		if(recent.size()>MAXRECENT)
			recent.remove(0);

		for(KeyFigureMetaData s : recent) {
			if(s.hash == nv.hash)
				return;
		}
		recent.add(nv);
		storeRecentList();
	}

	private void setXResolution(float frame) {
		if(frame > 600)
			resolution_ms = 2000;
		else if(frame > 200)
			resolution_ms = 500;
		else if(frame > 50)
			resolution_ms = 200;
		else if(frame > 20)
			resolution_ms = 100;
		else
			resolution_ms = 50;

		if(resolution_ms > COLLECTOR_CYCLE) {
			averaging.setDisable(false);
		} else {
			averaging.setSelected(false);
			averaging.setDisable(true);
		}

		timeframe = frame;

		if(!isDisabled()) {
			updateRequest();
			Platform.runLater(() -> {
				xAxis.setLabel("Seconds ("+resolution_ms+"ms)");
			});
		}
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

	private  void updateGraph(boolean refresh) {
		float dt_sec = 0; AnalysisDataModel m =null; boolean set_bounds = false;

		float v1 ; float v2; float v3;

		if(isDisabled()) {
			return;
		}

		if(refresh) {

			if(dataService.getModelList().size()==0) {
				refreshRequest = true; return;
			}

			refreshRequest = false;
			pool.invalidateAll();
			series1.getData().clear();
			series2.getData().clear();
			series3.getData().clear();
			linechart.getAnnotations().clearAnnotations(Layer.FOREGROUND);
			last_annotation_pos = 0;
			yoffset = 0;

			if(dash.isSelected()) {
				if(type1.hash!=0)
					linechart.getAnnotations().add(dashboard1, Layer.FOREGROUND);
				if(type2.hash!=0)
					linechart.getAnnotations().add(dashboard2, Layer.FOREGROUND);
				if(type3.hash!=0)
					linechart.getAnnotations().add(dashboard3, Layer.FOREGROUND);

				setDashboardData(dashboard1,type1);
				setDashboardData(dashboard2,type2);
				setDashboardData(dashboard3,type3);
			}

			current_x_pt  = current_x0_pt;
			current_x1_pt = current_x0_pt + (int)(timeframe * 1000f / COLLECTOR_CYCLE);
			setXAxisBounds(current_x0_pt,current_x1_pt);

			if(type1.hash==0 && type2.hash==0 && type3.hash==0)
				return;
		}

		if(current_x_pt<dataService.getModelList().size() && dataService.getModelList().size()>0 ) {

			int max_x = dataService.getModelList().size();
			if((!state.getRecordingProperty().get() || isPaused) && current_x1_pt < max_x)
				max_x = current_x1_pt;

			if(dash.isSelected() && dataService.getModelList().size()>0 &&
					((current_x_pt * COLLECTOR_CYCLE * 10) % resolution_ms) == 0) {
				setDashboardData(dashboard1,type1);
				setDashboardData(dashboard2,type2);
				setDashboardData(dashboard3,type3);
			}

			while(current_x_pt<max_x ) {

				dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

				m = dataService.getModelList().get(current_x_pt);

				if(m.msg!=null && current_x_pt > 0 && m.msg!=null && m.msg.msg!=null
						&& ( type1.hash!=0 || type2.hash!=0 || type3.hash!=0)
						&& display_annotations) {
					if((current_x_pt - last_annotation_pos) > 150 || yoffset > 12)
						yoffset=0;

					linechart.getAnnotations().add(new LineMessageAnnotation(this,dt_sec,yoffset++, m.msg,
							(resolution_ms<300) && annotations.isSelected()),
							Layer.FOREGROUND);
					last_annotation_pos = current_x_pt;
				}

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0 && current_x_pt > 0) {

					if(current_x_pt > current_x1_pt) {
						if(series1.getData().size()>0 && type1.hash!=0) {
							pool.invalidate(series1.getData().get(0));
							series1.getData().remove(0);
						}

						if(series2.getData().size()>0 && type2.hash!=0) {
							pool.invalidate(series2.getData().get(0));
							series2.getData().remove(0);
						}

						if(series3.getData().size()>0 && type3.hash!=0) {
							pool.invalidate(series3.getData().get(0));
							series3.getData().remove(0);
						}
					}

					if(type1.hash!=0)  {
						v1 = determineValueFromRange(current_x_pt,resolution_ms/COLLECTOR_CYCLE,type1,averaging.isSelected());
						if(!Float.isNaN(v1))
							series1.getData().add(pool.checkOut(dt_sec,v1));

					}
					if(type2.hash!=0)  {
						v2 = determineValueFromRange(current_x_pt,resolution_ms/COLLECTOR_CYCLE,type2,averaging.isSelected());
						if(!Float.isNaN(v2))
							series2.getData().add(pool.checkOut(dt_sec,v2));
					}
					if(type3.hash!=0)  {
						v3 = determineValueFromRange(current_x_pt,resolution_ms/COLLECTOR_CYCLE,type3,averaging.isSelected());
						if(!Float.isNaN(v3))
							series3.getData().add(pool.checkOut(dt_sec,v3));
					}

				}

				if(current_x_pt > current_x1_pt) {
					set_bounds = true;
					if(!isPaused) {
						current_x0_pt += REFRESH_STEP;
						current_x1_pt += REFRESH_STEP;
					}
				}
				current_x_pt++;
			}

			if(set_bounds)
				setXAxisBounds(current_x0_pt,current_x1_pt);
		}
	}

	private void setDashboardData(DashBoardAnnotation d, KeyFigureMetaData kf) {
		AnalysisDataModel m = null; int count=0;
		float _min = Float.NaN; float _max = Float.NaN;
		float _avg = 0; float mean = 0; float std=0;

		if(kf.hash==0)
			return;

		d.setKeyFigure(kf);
		for(int i = current_x0_pt; i < current_x1_pt && i< dataService.getModelList().size();i++) {
			m = dataService.getModelList().get(i);
			if(m.getValue(kf)<_min || Float.isNaN(_min)) _min = m.getValue(kf);
			if(m.getValue(kf)>_max || Float.isNaN(_max)) _max = m.getValue(kf);
			_avg = _avg + m.getValue(kf); count++;
		}

		d.setMinMax(_min, _max);
		if(count>0) {
			mean = _avg / count; std = 0;
			for(int i = current_x0_pt; i < current_x1_pt && i< dataService.getModelList().size();i++) {
				m = dataService.getModelList().get(i);
				std = std + (m.getValue(kf) - mean) * (m.getValue(kf) - mean);
			}
			std = (float)Math.sqrt(std / count);
			d.setAvg(mean, std);
		}
	}

	private  void setXAxisBounds(int lower_pt, int upper_pt) {
		double tick = timeframe/5;
		if(tick < 1) tick = 1;
		xAxis.setTickUnit(tick);
		xAxis.setMinorTickCount(10);
		xAxis.setLowerBound(lower_pt * COLLECTOR_CYCLE / 1000F);
		xAxis.setUpperBound(upper_pt * COLLECTOR_CYCLE / 1000f);
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
		try {
			String rc = gson.toJson(recent);
			prefs.put(MAVPreferences.RECENT_FIGS, rc);
		} catch(Exception w) { }
	}

	private void readRecentList() {

		String rc = prefs.get(MAVPreferences.RECENT_FIGS, null);
		try {
			if(rc!=null)
				recent = gson.fromJson(rc, new TypeToken<ArrayList<KeyFigureMetaData>>() {}.getType());
		} catch(Exception w) { }
		if(recent==null)
			recent = new ArrayList<KeyFigureMetaData>();

	}

	// Determines spikes or average, if not all datapoints are reported.
	private float determineValueFromRange(int current_x, int length, KeyFigureMetaData m, boolean average) {
		float max = -Float.MAX_VALUE; float a = 0; float v; int index=0;

		if(dataService.getModelList().size() < length || Float.isNaN(dataService.getModelList().get(current_x).getValue(m)))
			return 0;

		if(length==1)
			return dataService.getModelList().get(current_x).getValue(m);

		if(average) {
			a = 0;
			for(int i=0;i<length;i++)
				a = a + dataService.getModelList().get(current_x-i).getValue(m);
			return a / length;

		} else {
			for(int i=0;i<length;i++) {
				v = Math.abs(dataService.getModelList().get(current_x-i).getValue(m));
				if(v>max && v != Float.NaN)
					max = v; index = i;
			}
			return dataService.getModelList().get(current_x-index).getValue(m);
		}
	}
}
