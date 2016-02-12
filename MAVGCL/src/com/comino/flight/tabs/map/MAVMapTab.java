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

package com.comino.flight.tabs.map;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.locks.LockSupport;

import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapShape;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;
import com.lynden.gmapsfx.shapes.Circle;
import com.lynden.gmapsfx.shapes.CircleOptions;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

public class MAVMapTab extends BorderPane implements Initializable, MapComponentInitializedListener {

	@FXML
	private GoogleMapView mapView;

	private Task<Long> task;

	private GoogleMap map;

	private Marker vehicle;
	private MarkerOptions markerOptions;

	private DataModel model;

	public MAVMapTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVMapTab.fxml"));
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
					if(map!=null) {
						map.setCenter(new LatLong(model.gps.latitude, model.gps.longitude));
						Thread.sleep(5);
						vehicle.setPosition(new LatLong(model.gps.latitude, model.gps.longitude));

					}

				} catch(Exception e) { e.printStackTrace(); }

			}
		});

	}

	@Override
	public void mapInitialized() {
		LatLong center = new LatLong(47.606189, 13.335842);

		MapOptions mapOptions = new MapOptions();

		mapOptions.center(center)
		.mapMarker(false)
		.zoom(18)
		.overviewMapControl(false)
		.panControl(false)
		.rotateControl(false)
		.scaleControl(false)
		.streetViewControl(false)
		.zoomControl(true)
		.mapType(MapTypeIdEnum.TERRAIN);


		map = mapView.createMap(mapOptions);

		markerOptions = new MarkerOptions();
		markerOptions.position(new LatLong(47.606189, 13.335842));
		markerOptions.icon(getClass().getResource("airplane_S.png").getFile());
		vehicle= new Marker(markerOptions);

		map.addMarker(vehicle);


	}

	@FXML
	private void initialize() {


	}


	public MAVMapTab setup(IMAVController control) {
		this.model=control.getCurrentModel();
		ExecutorService.get().execute(task);
		return this;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mapView.addMapInializedListener(this);

	}




}
