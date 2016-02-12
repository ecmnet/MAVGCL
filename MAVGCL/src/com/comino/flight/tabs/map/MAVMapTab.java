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

import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;

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

	private Task<Double> task;

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

		task = new Task<Double>() {

			@Override
			protected Double call() throws Exception {
				while(true) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if (isCancelled()) {
						break;
					}
					updateValue(model.gps.latitude + model.gps.longitude);
				}
				return model.gps.latitude + model.gps.longitude;
			}
		};

		task.valueProperty().addListener(new ChangeListener<Double>() {

			@Override
			public void changed(ObservableValue<? extends Double> observableValue, Double oldData, Double newData) {
                try {
					LatLong current_pos = new LatLong(model.gps.latitude, model.gps.longitude);
					map.setCenter(current_pos);
					vehicle.setPosition(current_pos);
					vehicle.setTitle("vehicle");
                } catch(Exception e) { }

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
		markerOptions.icon(getClass().getResource("qc.png").getFile());
		vehicle= new Marker(markerOptions);

		map.addMarker(vehicle);

		ExecutorService.get().execute(task);

	}

	@FXML
	private void initialize() {


	}


	public MAVMapTab setup(IMAVController control) {
		this.model=control.getCurrentModel();
		return this;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mapView.addMapInializedListener(this);

	}




}
