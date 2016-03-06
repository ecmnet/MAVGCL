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

package com.comino.flight.widgets.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.layout.GridPane;

public class MessagesWidget extends FadePane  {


	@FXML
	private GridPane m_grid;

	private List<Message> messages = null;


	private IMAVController control;

	public MessagesWidget() {

		super(300);

		messages = new ArrayList<Message>();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MessagesWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}



	public void setup(IMAVController control) {



	}

	private class Message {

		Label  value = null;

		public Message(GridPane grid, int row) {

			//			Label l1 = new Label(k.getDescription()+" :");
			//			l1.setPrefWidth(85); l1.setPrefHeight(19);
			//			grid.add(l1, 0, row);
			//			value = new Label("-"); value.setPrefWidth(45); value.setAlignment(Pos.CENTER_RIGHT);
			//			grid.add(value, 1, row);
			//			Label l3 = new Label(" "+k.getUnit()); l3.setPrefWidth(35);
			//			grid.add(l3, 2, row);

		}


	}

}
