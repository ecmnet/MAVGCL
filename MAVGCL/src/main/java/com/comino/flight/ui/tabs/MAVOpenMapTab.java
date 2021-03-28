/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import org.lodgon.openmapfx.core.BaseMapProvider;
import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.Position;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.providers.BingTileProvider;
import org.lodgon.openmapfx.providers.OSMTileProvider;
import org.mavlink.messages.MSP_AUTOCONTROL_MODE;
import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.base.UBXRTCM3Base;
import com.comino.flight.file.FileHandler;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.model.service.ICollectorRecordingListener;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.gps.details.GPSDetailsWidget;
import com.comino.flight.ui.widgets.panel.AirWidget;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavutils.MSPMathUtils;
import com.comino.openmapfx.ext.CanvasLayer;
import com.comino.openmapfx.ext.GoogleMapsTileProvider;

import javafx.animation.AnimationTimer;
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
import javafx.scene.SnapshotParameters;
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

	private final static float MINEPH = 5.0f;


	private final static String[] GPS_SOURCES    	= { "Global Position", "Local Position", "Raw GPS data" };
	private final static String[] CENTER_OPTIONS 	= { "Vehicle", "Home", "Base", "Takeoff" };
	private final static String[] PROVIDER_OPTIONS 	= { "Satellite", "StreetMap","Terrain" };

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
	private ChoiceBox<String> provider;

	@FXML
	private GPSDetailsWidget gpsdetails;

	@FXML
	private CheckBox aircontrol;

	@FXML
	private AirWidget air;

	@FXML
	private Button export;

	private LayeredMap map;

	private PositionLayer 		positionLayer;
	private PositionLayer 		homeLayer;
	private PositionLayer 		baseLayer;
	private PositionLayer 		targetLayer;
	//	private LicenceLayer  		licenceLayer;
	private CanvasLayer			canvasLayer;

	private AnimationTimer task = null;

	private AnalysisDataModel model;
	private int type = 0;

	private double takeoff_lon = 0;
	private double takeoff_lat = 0;

	private double[] pos = new double[2];
	private double[] tar = new double[2];

	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty   scroll        = new SimpleFloatProperty(0);
	private FloatProperty   replay       = new SimpleFloatProperty(0);

	private Image plane_valid, plane_invalid, plane_lpos;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();
	private Preferences preferences = MAVPreferences.getInstance();

	private  StateProperties state;

	private  BaseMapProvider satellite_provider = null;
	private  BaseMapProvider street_provider = null;
	private  BaseMapProvider terrain_provider = null;

	private StateProperties properties = null;

	private  double zoom_start=0;

	protected int centermode;

	private IMAVController control;

	public MAVOpenMapTab() {
		FXMLLoadHelper.load(this, "MAVOpenMapTab.fxml");

		this.state = StateProperties.getInstance();
		
		task = new AnimationTimer() {
			@Override
			public void handle(long now) {
				updateMap(true);
			}		
		};

	}


	@FXML
	private void initialize() {

		gpsdetails.setVisible(false);
		gpsdetails.fadeProperty().bind(viewdetails.selectedProperty());

		air.setVisible(false);
		air.fadeProperty().bind(aircontrol.selectedProperty());

		center.getItems().addAll(CENTER_OPTIONS);
		center.getSelectionModel().select(0);

		provider.getItems().addAll(PROVIDER_OPTIONS);
		provider.getSelectionModel().select(0);

		String mapDirName = FileHandler.getInstance().getBasePath()+"/MapCache";
		satellite_provider = new DefaultBaseMapProvider(new BingTileProvider(mapDirName));
		street_provider = new DefaultBaseMapProvider(new OSMTileProvider(mapDirName));
		terrain_provider = new DefaultBaseMapProvider(new GoogleMapsTileProvider(mapDirName));
		//		terrain_provider = new DefaultBaseMapProvider(new OpenTopoMapTileProvider(mapDirName));

		gpssource.getItems().addAll(GPS_SOURCES);
		gpssource.getSelectionModel().select(0);
		type = 0;

		map = new LayeredMap(satellite_provider);

		mapviewpane.setCenter(map);

		final Rectangle clip = new Rectangle();

		mapviewpane.setClip(clip);
		clip.heightProperty().bind(map.heightProperty());
		clip.widthProperty().bind(map.widthProperty());

		map.setZoom(zoom.getValue());

		canvasLayer = new CanvasLayer();
		map.getLayers().add(canvasLayer);

		homeLayer = new PositionLayer(new Image(getClass().getResource("resources/home.png").toString()));
		map.getLayers().add(homeLayer);

		targetLayer = new PositionLayer(new Image(getClass().getResource("resources/target.png").toString()));
		map.getLayers().add(targetLayer);

		baseLayer = new PositionLayer(new Image(getClass().getResource("resources/base.png").toString()));
		map.getLayers().add(baseLayer);

		plane_valid   = new Image(getClass().getResource("resources/airplane_g.png").toString());
		plane_lpos    = new Image(getClass().getResource("resources/airplane_y.png").toString());
		plane_invalid = new Image(getClass().getResource("resources/airplane_r.png").toString());

		positionLayer = new PositionLayer(plane_valid);
		map.getLayers().add(positionLayer);
		positionLayer.setVisible(true);

		positionLayer.updatePosition(47.142899,11.577723);

		//
		//		licenceLayer = new LicenceLayer(satellite_provider);
		//		map.getLayers().add(licenceLayer);

		// Test paintlistener
		//		canvasLayer.addPaintListener(new CanvasLayerPaintListener() {
		//
		//			Point2D p0; Point2D p1;  boolean first = true; AnalysisDataModel m;
		//
		//
		//			@Override
		//			public void redraw(GraphicsContext gc, double width, double height, boolean refresh) {
		//
		//				int index=0;
		//				if(refresh) {
		//					index = dataService.calculateX0IndexByFactor(1);
		//					first = true;
		//				}
		//
		//				// TODO MAVOpenMapTab: Draw path also in replay
		//
		//				if(state.getRecordingProperty().get()!=AnalysisModelService.STOPPED &&
		//						(dataService.getModelList().size()-index)>2*50/dataService.getCollectorInterval_ms()) {
		//
		//
		//					gc.setStroke(Color.LIGHTSKYBLUE); gc.setFill(Color.LIGHTSKYBLUE);
		//					gc.setLineWidth(1.5);
		//					for(int i=index; i<dataService.getModelList().size();
		//							i += 50/dataService.getCollectorInterval_ms()) {
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
		//							//		gc.fillOval(p0.getX()-4, p0.getY()-4,8,8);
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


		mapviewpane.widthProperty().addListener((v,o,n) -> {
			Platform.runLater(() -> {
				setCenter(centermode);
			});
		});

		map.setOnScroll(event -> {
			if(centermode!=0) {
				map.getMapArea().moveX(-event.getDeltaX()/3);
				map.getMapArea().moveY(-event.getDeltaY()/3);
			}
		});

		map.setOnZoom(event -> {
			double z = zoom.getValue() * ((( event.getZoomFactor() - 1 ) / 10) + 1.0);
			zoom.setValue(z);
		});

		map.setOnMouseClicked(click -> {
			if (click.getClickCount() == 2)
				setCenter(centermode);
			else {
				float[] xy = new float[2];
				if(control.getCurrentModel().sys.isAutopilotMode(MSP_AUTOCONTROL_MODE.INTERACTIVE)
						&& MSPMathUtils.is_projection_initialized()) {
					Position p = map.getMapPosition(click.getX(), click.getY());
					if(MSPMathUtils.map_projection_project(p.getLatitude(), p.getLongitude(), xy)) {
						msg_msp_command msp = new msg_msp_command(255,1);
						msp.command = MSP_CMD.MSP_CMD_OFFBOARD_SETLOCALPOS;
						msp.param1 =  xy[0];
						msp.param2 =  xy[1];
						control.sendMAVLinkMessage(msp);
					}
				}
			}
			click.consume();
		});

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				Platform.runLater(() -> {
					map.setZoom(zoom.getValue());
					updateMap(true);
				});
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
					setCenter(centermode);
					canvasLayer.redraw(true);
				});
			}

		});

		center.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				centermode = newValue.intValue();
				Platform.runLater(() -> {
					setCenter(centermode);
					updateMap(true);
				});
			}

		});

		provider.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				switch(newValue.intValue()) {
				case 0:
					zoom.setMax(20.76);
					map.setBaseMapProvider(satellite_provider);
					break;
				case 1:
					zoom.setMax(19.5);
					map.setBaseMapProvider(street_provider);
					break;
				case 2:

					//						if(zoom.getValue()>17.5)
					//							map.setZoom(17.5);
					zoom.setMax(20.5);
					map.setBaseMapProvider(terrain_provider);
					break;
				}
				setCenter(centermode);
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


	public MAVOpenMapTab setup(IMAVController control) {
		this.control = control;
		this.model=dataService.getCurrent();
		this.properties = StateProperties.getInstance();
		gpsdetails.setup(control);
		ChartControlPane.addChart(3,this);
		air.setup(control);

		properties.getLandedProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				takeoff_lon = model.getValue( "GLOBLON");
				takeoff_lat = model.getValue( "GLOBLAT");
			}
		});

		if(UBXRTCM3Base.getInstance()!=null) {
			UBXRTCM3Base.getInstance().getCurrentLocationProperty().addListener((e,o,n) -> {
				if(n.booleanValue()) {
					Platform.runLater(() -> {
						center.getSelectionModel().select(2);
						map.setCenter(model.getValue("BASELAT"), model.getValue("BASELON"));
					});
				}
			});
		}


		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				task.start();
				model = dataService.getCurrent();
			} else {
				task.stop();
			}
		});

		state.getBaseAvailableProperty().addListener((e,o,n) -> {
			if(n.booleanValue()) {
				Platform.runLater(() -> {
					updateMap(true);
				});
			}
		});

		replay.addListener((v, ov, nv) -> {
			Platform.runLater(() -> {
				if(nv.intValue()<=5) {
					updateMap(true);
				} else
					updateMap(false);
			});
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
	public FloatProperty getReplayProperty() {
		return replay;
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

		pos[0] = 0; pos[1] = 0;

		switch(type) {
		case 0:
			pos[0] = model.getValue("GLOBLAT");
			pos[1] = model.getValue("GLOBLON");
			break;
		case 1:
			MSPMathUtils.map_projection_init(preferences.getDouble(MAVPreferences.REFLAT, 0),
					preferences.getDouble(MAVPreferences.REFLON, 0));
			MSPMathUtils.map_projection_reproject((float)model.getValue("LPOSX"),
					(float)model.getValue("LPOSY"),
					(float)model.getValue("LPOSZ"), pos);

			break;
		case 2:
			pos[0] = model.getValue("RGPSLAT");
			pos[1] = model.getValue("RGPSLON");
			break;
		}

		if(model.getValue("SLAMDIR") !=0) {
			targetLayer.setVisible(true);
			MSPMathUtils.map_projection_reproject((float)model.getValue("SLAMPX"),
					(float)model.getValue("SLAMPY"),
					(float)model.getValue("SLAMPZ"), tar);
			targetLayer.updatePosition(tar[0], tar[1]);
		} else
			targetLayer.setVisible(false);

		//	canvasLayer.redraw(refreshCanvas);

		if(centermode==0 && pos[0]!=0)
			map.setCenter(pos[0],pos[1]);

		try {
			if(type!=1) {
				if(model.getValue("HOMLAT")!=0 && model.getValue("HOMLON")!=0) {
					//map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
					homeLayer.setVisible(true);
					homeLayer.updatePosition(model.getValue("HOMLAT"), model.getValue("HOMLON"));
				} else
					homeLayer.setVisible(false);
			} else {
				homeLayer.setVisible(true);
				homeLayer.updatePosition(preferences.getDouble(MAVPreferences.REFLAT,0),
						preferences.getDouble(MAVPreferences.REFLON,0));
			}

			if(model.getValue("BASELAT")!=0 && model.getValue("BASELON")!=0) {
				baseLayer.setVisible(true);
				baseLayer.updatePosition(model.getValue("BASELAT"), model.getValue("BASELON"));
			} else
				baseLayer.setVisible(false);

			if((model.getValue("RGPSEPH") > MINEPH || model.getValue("RGPSNO") > 4) && type!=1)
				positionLayer.getIcon().setImage(plane_valid);
			else if(StateProperties.getInstance().getLPOSAvailableProperty().get())
				positionLayer.getIcon().setImage(plane_lpos);
			else
				positionLayer.getIcon().setImage(plane_invalid);

			positionLayer.updatePosition(pos[0],pos[1],model.getValue("HEAD"));

		} catch(Exception e) { e.printStackTrace(); }
	}


	public void setKeyFigureSelection(KeyFigurePreset preset) {

	}

	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}

	private void setCenter(int mode) {
		switch(mode) {
		case 1:
			if(model.getValue("HOMLAT")!=0 && type!=1)
				map.setCenter(model.getValue("HOMLAT"), model.getValue("HOMLON"));
			else
				map.setCenter(preferences.getDouble(MAVPreferences.REFLAT,0),
						preferences.getDouble(MAVPreferences.REFLON,0));
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
	}
}
