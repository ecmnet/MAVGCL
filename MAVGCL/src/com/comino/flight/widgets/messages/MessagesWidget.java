/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.widgets.messages;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVMessageListener;
import com.comino.msp.model.segment.LogMessage;
import com.comino.msp.utils.ExecutorService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;

public class MessagesWidget extends FadePane  {

	@FXML
	private ListView<String> listview;

	private IMAVController control;

	private ScheduledFuture f = null;

	private final SimpleDateFormat fo = new SimpleDateFormat("HH:mm:ss");
	private ConcurrentLinkedQueue<LogMessage> list = null;


	public MessagesWidget() {

		super(300);
		list = new ConcurrentLinkedQueue<LogMessage>();


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

		this.setOnMouseEntered(event -> {
			if(f!=null)
				f.cancel(true);
		});

		this.setOnMouseExited(event -> {
			fadeProperty().setValue(false);
		});

		fadeProperty().setValue(false);

		disableProperty().addListener((observable, oldvalue, newvalue) -> {
			if(newvalue.booleanValue())
				fadeProperty().setValue(false);

		});

		control.addMAVMessageListener( new IMAVMessageListener() {


			@Override
			public void messageReceived(List<LogMessage> ml, LogMessage message) {

				if(message.severity< MAV_SEVERITY.MAV_SEVERITY_DEBUG  && !isDisabled()) {
					list.add(message);

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							while(!list.isEmpty()) {
								LogMessage m = list.poll();
								fadeProperty().setValue(true);
								listview.getItems().add(fo.format(new Date(m.tms))+" : \t"+m.msg);
								listview.scrollTo(listview.getItems().size()-1);
								fadeProperty().setValue(true);
							}
							fadeProperty().setValue(true);
						}
					});
					showMessages();
				}
			}

		});

		listview.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> observable, Object oldvalue, Object newValue) {
				Platform.runLater(new Runnable() {
					public void run() {
						listview.getSelectionModel().select(-1);
					}
				});
			}
		});
	}

	public void showMessages() {

		if(listview.getItems().isEmpty()) {
			fadeProperty().setValue(false);
			return;
		}


		fadeProperty().setValue(true);

		if(f!=null) {
			f.cancel(true);
		}

		f = ExecutorService.get().schedule(new Runnable() {
			@Override
			public void run() {
				fadeProperty().setValue(false);
			}
		},3,TimeUnit.SECONDS);
	}

}
