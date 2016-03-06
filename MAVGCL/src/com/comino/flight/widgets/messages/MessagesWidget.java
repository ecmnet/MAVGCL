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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVMessageListener;
import com.comino.msp.model.segment.Message;
import com.comino.msp.utils.ExecutorService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class MessagesWidget extends FadePane  {


	@FXML
	private GridPane m_grid;

	private MessageList[] l_messages = null;


	private IMAVController control;

	private final SimpleDateFormat fo = new SimpleDateFormat("mm:ss.SSS");

	public MessagesWidget() {

		super(300);

		l_messages = new MessageList[5];

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


		fadeProperty().setValue(false);

//		for(int i=0;i<5;i++)
//			l_messages[i] = new MessageList(i);
//
//
//		control.addMAVMessageListener( new IMAVMessageListener() {
//
//			@Override
//			public void messageReceived(List<Message> ml, Message m) {
//				fadeProperty().setValue(true);
//
//				Platform.runLater(new Runnable() {
//					@Override
//					public void run() {
//                      l_messages[0].setMessage(m);
//					}
//				});
//
//				System.out.println(ml.size());
//
//				ExecutorService.get().schedule(new Runnable() {
//					@Override
//					public void run() {
//						fadeProperty().setValue(true);
//					}
//				},2500,TimeUnit.MILLISECONDS);
//
//			}
//
//		});


	}


	private class MessageList {
		private Label l_tms;
		private Label l_msg;

		private Message ms;

		public MessageList(int row) {
			l_tms = new Label();
			l_tms.setPrefWidth(65); l_tms.setPrefHeight(19);
			m_grid.add(l_tms, 0, row);
			l_msg = new Label();
			l_msg.setPrefWidth(120); l_msg.setPrefHeight(19);
			m_grid.add(l_msg, 1, row);
		}

		public void setMessage(Message m) {
			this.ms = m;
			l_tms.setText(Long.toString(m.tms));
			l_msg.setText(m.msg);
		}

		public Message getMessage() {
			return ms;
		}

	}

}
