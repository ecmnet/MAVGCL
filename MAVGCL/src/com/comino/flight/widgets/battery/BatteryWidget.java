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

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class BatteryWidget extends Pane  {

	private static final float vo_range[] = { 10.0f, 13.0f, 11.5f,  0 };
	private static final float cu_range[] = { 0.0f,  15.0f, 0,     12 };
	private static final float ca_range[] = { 0.0f,  100.0f, 60.0f, 0 };

	@FXML
	private ProgressBar voltage;

	@FXML
	private ProgressBar current;

	@FXML
	private ProgressBar capacity;

	@FXML
	private Label f_voltage;

	@FXML
	private Label f_current;

	@FXML
	private Label f_capacity;

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

				checkBarLimits(voltage, model.battery.b0, vo_range[2], vo_range[3], model.sys.isStatus(Status.MSP_CONNECTED));
				if(model.battery.b0 > vo_range[0]) {
					voltage.setProgress((model.battery.b0 - vo_range[0]) / (vo_range[1] - vo_range[0]));
					f_voltage.setText(fo.format(model.battery.b0));
				}

				checkBarLimits(current, model.battery.c0, cu_range[2], cu_range[3], model.sys.isStatus(Status.MSP_CONNECTED));
				if(model.battery.c0 > cu_range[0]) {
					current.setProgress((model.battery.c0 - cu_range[0]) / (cu_range[1] - cu_range[0]));
					f_current.setText(fo.format(model.battery.c0));
				}

				checkBarLimits(capacity, model.battery.p, ca_range[2], ca_range[3], model.sys.isStatus(Status.MSP_CONNECTED));
				if(model.battery.p > ca_range[0]) {
					capacity.setProgress((model.battery.p - ca_range[0]) / (ca_range[1] - ca_range[0]));
					f_capacity.setText(fo.format(model.battery.p));
				}

			}
		});

	}

	private void checkBarLimits(ProgressBar bar, float val, float low, float high, boolean valid) {
		if(low!=0 && val < low && val > 0) {
			bar.setStyle("-fx-accent: red;");
			return; }

		if(high!=0 && val > high) {
			bar.setStyle("-fx-accent: red;");
			return; }

		if(!valid) {
			bar.setStyle("-fx-accent: lightgrey;");
			return; }

		bar.setStyle("-fx-accent: darkcyan;");
	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		ExecutorService.get().execute(task);
	}

}
