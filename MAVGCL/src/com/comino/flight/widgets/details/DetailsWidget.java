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

package com.comino.flight.widgets.details;

import java.io.IOException;
import java.text.DecimalFormat;

import com.comino.mav.control.IMAVController;
import com.comino.model.types.ModelUtils;
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

public class DetailsWidget extends Pane  {

	@FXML
	private Label f_altitude;

	@FXML
	private Label f_anglex;

	@FXML
	private Label f_angley;

	@FXML
	private Label f_compass;

	@FXML
	private Label f_speed;

	@FXML
	private Label f_quality;

	private Task<Long> task;
	private IMAVController control;
	private DataModel model;


	private final DecimalFormat fo = new DecimalFormat("#0.0");

	public DetailsWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DetailsWidget.fxml"));
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
				return model.battery.tms;
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
                   f_altitude.setText(fo.format(model.attitude.ag));
                   f_anglex.setText(fo.format(ModelUtils.fromRad(model.attitude.aX)));
                   f_angley.setText(fo.format(ModelUtils.fromRad(model.attitude.aY)));
                   f_compass.setText(fo.format(model.attitude.h));
                   f_speed.setText(fo.format(Math.sqrt(model.state.vx * model.state.vx + model.state.vy * model.state.vy)));
                   f_quality.setText("0");

			}
		});

	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		ExecutorService.get().execute(task);
	}

}
