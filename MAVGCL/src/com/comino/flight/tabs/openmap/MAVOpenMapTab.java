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
import java.net.URL;

import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.LicenceLayer;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.core.TileProvider;
import org.lodgon.openmapfx.core.TileType;
import org.lodgon.openmapfx.service.MapViewPane;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.msp.model.segment.GPS;
import com.comino.msp.utils.ExecutorService;
import com.comino.openmapfx.ext.CanvasLayer;
import com.comino.openmapfx.ext.CanvasLayerPaintListener;
import com.comino.openmapfx.ext.InformationLayer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MAVOpenMapTab extends BorderPane {

	private final static int MAP_UPDATE_MS = 330;

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

	private LayeredMap map;

	private PositionLayer 		positionLayer;
	private PositionLayer 		homeLayer;
	private LicenceLayer  		licenceLayer;
	private InformationLayer 	infoLayer;
	private CanvasLayer			canvasLayer;

	private Task<Long> task;

	private DataModel model;
	private int type = 1;

	private boolean map_changed = false;
	private boolean home_set = false;

	private boolean isCollecting = false;

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
						Thread.sleep(200);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled() ) {
						break;
					}

					if(!isCollecting && collector.isCollecting()) {
						canvasLayer.redraw(true);
					}
					isCollecting = collector.isCollecting();

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
						map.setZoom(zoom.getValue());
						if(model.gps.isFlagSet(GPS.GPS_REF_VALID))
							map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
						canvasLayer.redraw(true);
						map_changed = false;
						return;
					}

					if(model.gps.numsat>3) {
						positionLayer.updatePosition(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]),model.attitude.h);

						if(model.gps.isFlagSet(GPS.GPS_REF_VALID)) {
							if(!home_set)
								map.setCenter(model.gps.ref_lat, model.gps.ref_lon);
							home_set = true;
							homeLayer.updatePosition(model.gps.ref_lat, model.gps.ref_lon);
						} else {
							map.setCenter(MSTYPE.getValue(model,TYPES[type][0]),MSTYPE.getValue(model,TYPES[type][1]));
						}
					 canvasLayer.redraw(false);
					}

					infoLayer.update();

				} catch(Exception e) { e.printStackTrace(); }

			}
		});


	}


	@FXML
	private void initialize() {
		DefaultBaseMapProvider provider = new DefaultBaseMapProvider();

		gpssource.getItems().addAll(GPS_SOURCES);
		gpssource.getSelectionModel().select(1);

		map = new LayeredMap(provider);
		MapViewPane mapPane = new MapViewPane(map);
		mapviewpane.setCenter(mapPane);

		Rectangle clip = new Rectangle();
		mapPane.setClip(clip);
		clip.heightProperty().bind(mapPane.heightProperty());
		clip.widthProperty().bind(mapPane.widthProperty());

		map.setCenter(48.142899,11.577723);
		map.setZoom(20);

		positionLayer = new PositionLayer(new Image(getClass().getResource("airplane.png").toString()));
		homeLayer = new PositionLayer(new Image(getClass().getResource("home.png").toString()));
		map.getLayers().add(positionLayer);
		map.getLayers().add(homeLayer);
		positionLayer.updatePosition(48.142899,11.577723);

		licenceLayer = new LicenceLayer(provider);
		map.getLayers().add(licenceLayer);

		canvasLayer = new CanvasLayer();
		map.getLayers().add(canvasLayer);

		// Test paintlistener
		canvasLayer.addPaintListener(new CanvasLayerPaintListener() {

			Point2D p0; Point2D p1; int index=0; boolean first = true; DataModel m;

			@Override
			public void redraw(GraphicsContext gc, double width, double height, boolean refresh) {


				if(refresh) {
					index = 0;
					first = true;
				}


				if(isCollecting &&
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

			}

		});

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				map_changed = true;
			}
		});

		gpssource.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type = newValue.intValue();
				map_changed = true;
			}

		});


		zoom.setTooltip(new Tooltip("Zooming"));
	}


	public MAVOpenMapTab setup(IMAVController control) {
		this.collector = control.getCollector();
		this.model=control.getCurrentModel();

		infoLayer = new InformationLayer(model);
		map.getLayers().add(infoLayer);

		ExecutorService.get().execute(task);
		return this;
	}
}
