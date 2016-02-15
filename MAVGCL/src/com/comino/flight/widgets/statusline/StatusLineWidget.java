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

package com.comino.flight.widgets.statusline;

import java.io.IOException;

import com.comino.mav.control.IMAVController;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class StatusLineWidget extends Pane  {

	@FXML
	private Label version;

	@FXML
	private Label driver;

	@FXML
	private Label messages;

	@FXML
	private Label modelcount;

	private Task<Long> task;
	private IMAVController control;


	public StatusLineWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusLineWidget.fxml"));
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
						Thread.sleep(500);
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
				return System.currentTimeMillis();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
				if(control.getCurrentModel().sys.isStatus(Status.MSP_CONNECTED)) {
                  driver.setText(control.getCurrentModel().sys.getSensorString());
                  if(control.getMessageList().size()>0)
                     messages.setText(control.getMessageList().remove(0).msg);
				} else
					driver.setText("not connected");

		//		modelcount.setText("#"+Integer.toString(control.getCollector().getModelList().size()));

			}
		});


	}

	public void setup(IMAVController control) {
		this.control = control;
		messages.setText(control.getClass().getSimpleName()+ " loaded");
		ExecutorService.get().execute(task);

	}



}
