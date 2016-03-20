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

package com.comino.flight.tabs.openmap;

import java.io.IOException;

import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.LicenceLayer;
import org.lodgon.openmapfx.core.MapTileType;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.providers.BingTileProvider;
import org.lodgon.openmapfx.service.MapViewPane;

import com.comino.flight.control.FlightControlPanel;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.flight.widgets.gps.details.GPSDetailsWidget;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.MSTYPE;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.msp.model.segment.GPS;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;
import com.comino.openmapfx.ext.CanvasLayer;
import com.comino.openmapfx.ext.CanvasLayerPaintListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MAVOpenMapTab extends BorderPane  implements IChartControl {

	private final static int MAP_UPDATE_MS = 100;

	private final static String[] GPS_SOURCES = { "Global Position", "Raw GPS data" };


	private final static MSTYPE TYPES[][] =
		{ { MSTYPE.MSP_GLOBPLAT, MSTYPE.MSP_GLOBPLON },
				{ MSTYPE.MSP_RAW_GPSLAT, MSTYPE.MSP_RAW_GPSLON } };

	@FXML
	private BorderPane mapviewpane;

	@FXML
	private Slider zoom;

	@FXML
	private ChoiceBox<String> gpssource;

	@FXML
	private ChoiceBox<MapTileType> tiletype;

	@FXML
	private CheckBox viewdetails;

	@FXML
	private CheckBox mapfollow;

	@FXML
	private GPSDetailsWidget gpsdetails;

	private LayeredMap map;

	private PositionLayer 		positionLayer;
	private PositionLayer 		homeLayer;
	private LicenceLayer  		licenceLayer;
	private CanvasLayer			canvasLayer;

	private Task<Long> task;

	private DataModel model;
	private int type = 1;

	private boolean map_changed = false;
	private boolean home_set = false;

	private int index=0;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);


	private ModelCollectorService collector;

	public MAVOpenMapTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVOpenMapTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}



		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled() ) {
						break;
					}

					if(!isCollecting.get() && collector.isCollecting()) {
						canvasLayer.redraw(true);
					}
					isCollecting.set(collector.isCollecting());

					updateValue(System.currentTimeMillis());
				}
				return System.currentTimeMillis();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
				try {

					if(map_changed) {
						if(model.gps.isFlagSet(GPS.GPS_REF_VALID))
							if(model.gps.ref_lat!=0 && model.gps.ref_lon!=0)
								map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
							else
								map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
						canvasLayer.redraw(true);
						map_changed = false;
						return;
					}

					if(model.gps.numsat>3) {

						if(mapfollow.selectedProperty().get()) {
							map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
							canvasLayer.redraw(true);
						} else {
							canvasLayer.redraw(false);
						}
						positionLayer.updatePosition(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]),model.attitude.h);


					}

				} catch(Exception e) { e.printStackTrace(); }

			}
		});


	}


	@FXML
	private void initialize() {

		gpsdetails.setVisible(false);
		gpsdetails.fadeProperty().bind(viewdetails.selectedProperty());

		mapfollow.selectedProperty().set(true);

		// TODO 1.0: provide application directory

		//	DefaultBaseMapProvider provider = new DefaultBaseMapProvider(new ThunderForestTileProvider());
		String mapFileName = System.getProperty("user.home")+"/.MAVGCLMaps";
		DefaultBaseMapProvider provider = new DefaultBaseMapProvider(new BingTileProvider("http://t0.tiles.virtualearth.net/tiles/a",mapFileName));

		gpssource.getItems().addAll(GPS_SOURCES);
		gpssource.getSelectionModel().select(1);

		tiletype.getItems().addAll(provider.getTileTypes());
		tiletype.getSelectionModel().select(0);

		provider.tileTypeProperty().bind(tiletype.valueProperty());


		map = new LayeredMap(provider);
		MapViewPane mapPane = new MapViewPane(map);
		mapviewpane.setCenter(mapPane);

		Rectangle clip = new Rectangle();
		mapPane.setClip(clip);
		clip.heightProperty().bind(mapPane.heightProperty());
		clip.widthProperty().bind(mapPane.widthProperty());
		//		map.setCenter(49.142899,11.577723);
		map.setZoom(19.5);

		positionLayer = new PositionLayer(new Image(getClass().getResource("airplane.png").toString()));
		homeLayer = new PositionLayer(new Image(getClass().getResource("home.png").toString()));
		map.getLayers().add(positionLayer);
		map.getLayers().add(homeLayer);

		positionLayer.updatePosition(49.142899,11.577723);

		licenceLayer = new LicenceLayer(provider);
		map.getLayers().add(licenceLayer);

		canvasLayer = new CanvasLayer();
		map.getLayers().add(canvasLayer);

		// Test paintlistener
		canvasLayer.addPaintListener(new CanvasLayerPaintListener() {

			Point2D p0; Point2D p1;  boolean first = true; DataModel m;

			@Override
			public void redraw(GraphicsContext gc, double width, double height, boolean refresh) {


				if(refresh) {
					index = 0;
					first = true;
				}

				positionLayer.setVisible(model.sys.isStatus(Status.MSP_CONNECTED));

				if(isCollecting.get() &&
						(collector.getModelList().size()-index)>2*MAP_UPDATE_MS/collector.getCollectorInterval_ms()) {

					//System.out.println(index+" -> "+collector.getModelList().size());

					gc.setStroke(Color.BLUE); gc.setFill(Color.BLUE);
					for(int i=index; i<collector.getModelList().size();
							i += MAP_UPDATE_MS/collector.getCollectorInterval_ms()) {

						m = collector.getModelList().get(i);

						if(first) {
							p0 = map.getMapArea().getMapPoint(
									MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));

							gc.fillOval(p0.getX()-3, p0.getY()-3,6,6);
							first = false; continue;
						}
						p1 = map.getMapArea().getMapPoint(
								MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));

						gc.strokeLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
						p0 = map.getMapArea().getMapPoint(
								MSTYPE.getValue(m,TYPES[type][0]),MSTYPE.getValue(m,TYPES[type][1]));
					}
					index = collector.getModelList().size();
				}

				if(refresh) {

					//					Position p1 = map.getMapArea().getMapPosition(width-150, height-20);
					//					Position p2 = map.getMapArea().getMapPosition(width-50, height-20);
					//					int scale = (int)(0.5f+MSPGeoUtils.getDistance(p1.getLatitude(), p1.getLongitude(),
					//							p2.getLatitude(), p2.getLongitude()));
					//					gc.fillText(scale+"m", width-75, height-10);

					gc.setStroke(Color.DARKGRAY);
					gc.strokeLine(width-150, height-20, width-50,height-20);
					gc.strokeLine(width-150, height-25, width-150,height-20);
					gc.strokeLine(width-50, height-25, width-50,height-20);
				}
			}

		});

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				//map_changed = true;
				map.setZoom(zoom.getValue());
			}
		});

		gpssource.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type = newValue.intValue();
				map_changed = true;
			}

		});

		mapfollow.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(oldValue.booleanValue() && !newValue) {
					if(model.gps.ref_lat!=0)
						map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
					else
						map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
				}
			}
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				map_changed = true;
			}
		});






		zoom.setTooltip(new Tooltip("Zooming"));
	}


	public MAVOpenMapTab setup(FlightControlPanel flightControl, IMAVController control) {
		this.collector = control.getCollector();
		this.model=control.getCurrentModel();

		gpsdetails.setup(control);

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
		return null;
	}


	@Override
	public void refreshChart() {
		map_changed = true;
	}

}
