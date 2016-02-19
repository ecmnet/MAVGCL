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

package com.comino.flight.tabs.world;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.mav.control.IMAVController;
import com.comino.msp.utils.ExecutorService;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;

public class MAVWorldTab extends BorderPane  {


	private Task<Long> task;


	public MAVWorldTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVWorldTab.fxml"));
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
						Thread.sleep(50);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled() ) {
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

				} catch(Exception e) { e.printStackTrace(); }

			}
		});



	}


	@FXML
	private void initialize() {




	}


	public MAVWorldTab setup(ChartControlWidget recordControl, IMAVController control) {
		ExecutorService.get().execute(task);
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return null;
	}

	public IntegerProperty getTimeFrameProperty() {
		return null;
	}





}


