/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.tabs;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.LicenceLayer;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.providers.BingTileProvider;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.FileHandler;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.gps.details.GPSDetailsWidget;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.panel.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.openmapfx.ext.CanvasLayer;
import com.comino.openmapfx.ext.CanvasLayerPaintListener;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class MAVOpenMapTab extends BorderPane implements IChartControl {

	private final static int MAP_UPDATE_MS = 100;

	private final static String[] GPS_SOURCES    = { "Global Position", "Raw GPS data" };

	private final static String[] CENTER_OPTIONS = { "Vehicle", "Home", "Base", "Takeoff" };


	private final static String TYPES[][] =
		{ { "GLOBLAT",  "GLOBLON"   },
				{ "RGPSLAT",  "RGPSLON"   }
		};

	@FXML
	private BorderPane mapviewpane;

	@FXML
	private Slider zoom;

	@FXML
	private ChoiceBox<String> gpssource;

	@FXML
	private CheckBox viewdetails;

	@FXML
	private ChoiceBox<String> center;

	@FXML
	private GPSDetailsWidget gpsdetails;

	@FXML
	private Button export;

	private LayeredMap map;

	private PositionLayer 		positionLayer;
	private PositionLayer 		homeLayer;
	private PositionLayer 		baseLayer;
	private LicenceLayer  		licenceLayer;
	private CanvasLayer			canvasLayer;

	private Timeline task = null;

	private AnalysisDataModel model;
	private int type = 0;

	private double takeoff_lon = 0;
	private double takeoff_lat = 0;


	private int index=0;

	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);

	private FloatProperty  scroll       = new SimpleFloatProperty(0);

	private Image plane_valid, plane_invalid, plane_lpe;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();

	private IMAVController control;

	private  StateProperties state;


	protected int centermode;

	public MAVOpenMapTab() {
		FXMLLoadHelper.load(this, "MAVOpenMapTab.fxml");

		this.state = StateProperties.getInstance();
		task = new Timeline(new KeyFrame(Duration.millis(50), ae -> {
			updateMap(true);
		} ) );
		task.setCycleCount(Timeline.INDEFINITE);
	}


	@FXML
	private void initialize() {

		this.boundsInParentProperty().addListener((e,o,n) -> {

		});


		gpsdetails.setVisible(false);
		gpsdetails.fadeProperty().bind(viewdetails.selectedProperty());

		center.getItems().addAll(CENTER_OPTIONS);
		center.getSelectionModel().select(0);

		String mapFileName = FileHandler.getInstance().getBasePath()+"/MapCache";
		DefaultBaseMapProvider provider = new DefaultBaseMapProvider(new BingTileProvider("http://t0.tiles.virtualearth.net/tiles/a",mapFileName));

		gpssource.getItems().addAll(GPS_SOURCES);
		gpssource.getSelectionModel().select(0);
		type = 0;

		map = new LayeredMap(provider);

		mapviewpane.setCenter(map);

		Rectangle clip = new Rectangle();
		mapviewpane.setClip(clip);
		clip.heightProperty().bind(map.heightProperty());
		clip.widthProperty().bind(mapviewpane.widthProperty());
		//		//		map.setCenter(49.142899,11.577723);
		map.setZoom(19.5);

		canvasLayer = new CanvasLayer();
		map.getLayers().add(canvasLayer);

		homeLayer = new PositionLayer(new Image(getClass().getResource("resources/home.png").toString()));
		map.getLayers().add(homeLayer);

		baseLayer = new PositionLayer(new Image(getClass().getResource("resources/base.png").toString()));
		map.getLayers().add(baseLayer);

		plane_valid   = new Image(getClass().getResource("resources/airplane_g.png").toString());
		plane_lpe     = new Image(getClass().getResource("resources/airplane_b.png").toString());
		plane_invalid = new Image(getClass().getResource("resources/airplane_r.png").toString());

		positionLayer = new PositionLayer(plane_valid);
		map.getLayers().add(positionLayer);
		positionLayer.setVisible(true);

		positionLayer.updatePosition(49.142899,11.577723);

		licenceLayer = new LicenceLayer(provider);
		map.getLayers().add(licenceLayer);

		// Test paintlistener
		//		canvasLayer.addPaintListener(new CanvasLayerPaintListener() {
		//
		//			Point2D p0; Point2D p1;  boolean first = true; AnalysisDataModel m;
		//
		//
		//			@Override
		//			public void redraw(GraphicsContext gc, double width, double height, boolean refresh) {
		//
		//
		//				if(refresh) {
		//					index = dataService.calculateX0IndexByFactor(1);
		//					first = true;
		//				}
		//
		//				// TODO MAVOpenMapTab: Draw path also in replay
		//
		//				if(state.getRecordingProperty().get()!=AnalysisModelService.STOPPED &&
		//						(dataService.getModelList().size()-index)>2*MAP_UPDATE_MS/dataService.getCollectorInterval_ms()) {
		//
		//
		//					gc.setStroke(Color.DARKKHAKI); gc.setFill(Color.DARKKHAKI);
		//					gc.setLineWidth(1.5);
		//					for(int i=index; i<dataService.getModelList().size();
		//							i += MAP_UPDATE_MS/dataService.getCollectorInterval_ms()) {
		//
		//						m = dataService.getModelList().get(i);
		//
		//						if(m.getValue(TYPES[type][0])==0 && m.getValue(TYPES[type][1]) == 0)
		//							continue;
		//
		//						if(first) {
		//							p0 = map.getMapArea().getMapPoint(
		//									m.getValue(TYPES[type][0]),m.getValue(TYPES[type][1]));
		//
		//							gc.fillOval(p0.getX()-4, p0.getY()-4,8,8);
		//							first = false; continue;
		//						}
		//						p1 = map.getMapArea().getMapPoint(
		//								m.getValue(TYPES[type][0]),m.getValue(TYPES[type][1]));
		//
		//						gc.strokeLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
		//						p0 = map.getMapArea().getMapPoint(
		//								m.getValue(TYPES[type][0]),m.getValue(TYPES[type][1]));
		//					}
		//					index = dataService.getModelList().size();
		//				}
		//			}
		//
		//		});

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				//				if((System.currentTimeMillis()-tms)>100) {
				//					tms = System.currentTimeMillis();
				Platform.runLater(() -> {
					map.setZoom(zoom.getValue());
					updateMap(true);
				});
				//				}
			}
		});

		zoom.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					zoom.setValue(19.5f);
					Platform.runLater(() -> {
						map.setZoom(zoom.getValue());
						updateMap(true);
					});
				}
			}
		});


		gpssource.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type = newValue.intValue();
				Platform.runLater(() -> {
					canvasLayer.redraw(true);
				});
			}

		});

		center.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				centermode = newValue.intValue();
				Platform.runLater(() -> {
					updateMap(true);
				});
			}

		});

		scroll.addListener((v, ov, nv) -> {
			if(state.getRecordingProperty().get()==AnalysisModelService.STOPPED) {

				int current_x1_pt = dataService.calculateX0IndexByFactor(nv.floatValue());

				if(dataService.getModelList().size()>0 && current_x1_pt > 0)
					model = dataService.getModelList().get(current_x1_pt);
				else
					model = dataService.getCurrent();

			}
		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});


		zoom.setTooltip(new Tooltip("Zooming"));
	}


	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = mapviewpane.snapshot(param, null);
		File file = new File(path+"/map.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}


	public MAVOpenMapTab setup(ChartControlWidget recordControl, IMAVController control) {
		this.model=dataService.getCurrent();
		this.control = control;

		gpsdetails.setup(control);
		recordControl.addChart(3,this);

		StateProperties.getInstance().getLandedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				takeoff_lon = model.getValue( "GLOBLON");
				takeoff_lat = model.getValue( "GLOBLAT");
			}
		});

		task.play();

		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
//				int current_x1_pt = dataService.calculateX0IndexByFactor(scroll.get());
//				if(dataService.getModelList().size()>0 && current_x1_pt > 0)
//					model = dataService.getModelList().get(current_x1_pt);
//				else
					model = dataService.getCurrent();
			}
		});

		state.getBaseAvailableProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				Platform.runLater(() -> {
					updateMap(true);
				});
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

	public BooleanProperty getIsScrollingProperty() {
		return null;
	}


	@Override
	public void refreshChart() {

		Platform.runLater(() -> {
		//	this.model=dataService.getLast(1);
			updateMap(true);
		});
	}

	private void updateMap(boolean refreshCanvas) {

		if(state.getRecordingProperty().get()==AnalysisModelService.STOPPED  && dataService.isCollecting()) {
			canvasLayer.redraw(refreshCanvas);
		}

		switch(centermode) {
		case 0:
			if(model.getValue(TYPES[type][0])!=0)
				map.setCenter(model.getValue(TYPES[type][0]),model.getValue(TYPES[type][1]));
			break;
		case 1:
			if(model.getValue("HOMLAT")!=0)
				map.setCenter(model.getValue("HOMLAT"), model.getValue("HOMLON"));
			break;
		case 2:
			if(model.getValue("BASELAT")!=0)
				map.setCenter(model.getValue("BASELAT"), model.getValue("BASELON"));
			break;
		case 3:
			if(takeoff_lat!=0)
				map.setCenter(takeoff_lat, takeoff_lon);
			break;

		}

		try {
			if(model.getValue("HOMLAT")!=0 && model.getValue("HOMLON")!=0) {
				//map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
				homeLayer.setVisible(true);
				homeLayer.updatePosition(model.getValue("HOMLAT"), model.getValue("HOMLON"));
			} else
				homeLayer.setVisible(false);

			if(model.getValue("BASELAT")!=0 && model.getValue("BASELON")!=0) {
				baseLayer.setVisible(true);
				baseLayer.updatePosition(model.getValue("BASELAT"), model.getValue("BASELON"));
			} else
				baseLayer.setVisible(false);

			if(model.getValue("RGPSHDOP") > 2.5)
				positionLayer.getIcon().setImage(plane_invalid);
			else
				positionLayer.getIcon().setImage(plane_valid);

			positionLayer.updatePosition(
					model.getValue(TYPES[type][0]),model.getValue(TYPES[type][1]),model.getValue("HEAD"));

		} catch(Exception e) { e.printStackTrace(); }
	}


	public void setKeyFigureSeletcion(KeyFigurePreset preset) {

	}

	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}

}
