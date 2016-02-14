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

import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.GPS;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class MAVOpenMapTab extends BorderPane  {


	@FXML
	private BorderPane mapviewpane;

	@FXML
	private Slider zoom;

	private LayeredMap map;

	private PositionLayer positionLayer;
	private PositionLayer homeLayer;
	private LicenceLayer licenceLayer;

	private Task<Long> task;

	private DataModel model;
	private float zoom_value = 20;

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

					if (isCancelled()) {
						break;
					}
					updateValue(System.currentTimeMillis());
				}
				return System.currentTimeMillis();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
				try {
					map.setZoom(zoom_value);
					if(model.gps.numsat>3) {
						positionLayer.updatePosition(model.gps.latitude,model.gps.longitude,model.attitude.h);

						if(model.gps.isFlagSet(GPS.GPS_REF_VALID)) {
							homeLayer.updatePosition(model.gps.ref_lat, model.gps.ref_lon);
						} else {
							map.setCenter(model.gps.latitude,model.gps.longitude);
						}

					}

				} catch(Exception e) { e.printStackTrace(); }

			}
		});


	}


	@FXML
	private void initialize() {
		DefaultBaseMapProvider provider = new DefaultBaseMapProvider();


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

		zoom.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				zoom_value = new_val.floatValue();
			}
		});


	}



	public MAVOpenMapTab setup(IMAVController control) {
		this.model=control.getCurrentModel();
		ExecutorService.get().execute(task);
		return this;
	}


}
