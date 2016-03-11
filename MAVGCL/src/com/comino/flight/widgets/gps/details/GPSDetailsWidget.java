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

package com.comino.flight.widgets.gps.details;

import java.io.IOException;
import java.text.DecimalFormat;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;

public class GPSDetailsWidget extends FadePane  {

	@FXML
	private Label f_altitude;

	@FXML
	private Label f_lat;

	@FXML
	private Label f_lon;

	@FXML
	private Label f_compass;

	@FXML
	private Label f_sat;

	@FXML
	private Label f_eph;

	private Task<Long> task;
	private IMAVController control;
	private DataModel model;


	private final DecimalFormat fo = new DecimalFormat("#0.0");
	private final DecimalFormat fo6 = new DecimalFormat("#0.000000");

	public GPSDetailsWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GPSDetailsWidget.fxml"));
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

					if (isCancelled()) {
						break;
					}
					updateValue(System.currentTimeMillis());
				}
				return model.battery.tms;
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
                   f_altitude.setText(fo.format(model.attitude.ag));
                   f_lat.setText(fo6.format(model.gps.latitude));
                   f_lon.setText(fo6.format(model.gps.longitude));
                   f_compass.setText(fo.format(model.attitude.h));
                   f_eph.setText(fo.format(model.gps.eph));
                   f_sat.setText(Integer.toString(model.gps.numsat));

			}
		});

	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		ExecutorService.get().execute(task);
	}

}
