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
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

public class MAVMapTab extends BorderPane implements Initializable, MapComponentInitializedListener {

	@FXML
	private GoogleMapView mapView;

	private GoogleMap map;

	public MAVMapTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVMapTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	@Override
	public void mapInitialized() {
		LatLong center = new LatLong(47.606189, 13.335842);

		MapOptions mapOptions = new MapOptions();

		mapOptions.center(center)
		.mapMarker(false)
		.zoom(12)
		.overviewMapControl(false)
		.panControl(false)
		.rotateControl(false)
		.scaleControl(false)
		.streetViewControl(false)
		.zoomControl(true)
		.mapType(MapTypeIdEnum.TERRAIN);

		map = mapView.createMap(mapOptions);

	}

	@FXML
	private void initialize() {


	}


	public MAVMapTab setup(IMAVController control) {

		return this;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mapView.addMapInializedListener(this);

	}




}
