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

package com.comino.flight.widgets.battery;

import java.io.IOException;
import java.text.DecimalFormat;

import com.comino.flight.control.FlightModeProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class BatteryWidget extends Pane  {

	private static final float vo_range[] = { 10.0f, 13.0f, 11.5f,  0 };
	private static final float cu_range[] = { 0.0f,  15.0f, 0,     12 };
	private static final float ca_range[] = { 0.0f,  100.0f, 60.0f, 0 };



	@FXML
	private Gauge g_voltage;

	@FXML
	private Gauge g_capacity;



	private final DecimalFormat fo = new DecimalFormat("#0.0");

	private Task<Long> task;
	private IMAVController control;
	private DataModel model;

	public BatteryWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BatteryWidget.fxml"));
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
						Thread.sleep(1000);
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

				g_voltage.setValue(model.battery.b0);
				g_capacity.setValue(model.battery.p);

			}
		});

	}


	@FXML
	private void initialize() {
         setupGauge(g_voltage,8,13,"V",Color.DARKORANGE);
         g_voltage.setDecimals(1);
         setupGauge(g_capacity,0,100,"%",Color.DEEPSKYBLUE);
         g_capacity.setDecimals(0);
	}


	private void setupGauge(Gauge gauge, float min, float max, String unit, Color color) {
		gauge.setSkinType(SkinType.SLIM);
		gauge.setBarColor(color);
		gauge.setMinValue(min);
		gauge.setMaxValue(max);
		gauge.setDecimals(1);
	    gauge.setTitle(unit);
	    gauge.setUnit("Battery");
	    gauge.disableProperty().bind(FlightModeProperties.getInstance().getConnectedProperty().not());
		gauge.setValueColor(Color.WHITE);
		gauge.setTitleColor(Color.WHITE);
		gauge.setUnitColor(Color.WHITE);

	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		ExecutorService.get().execute(task);
	}

}
