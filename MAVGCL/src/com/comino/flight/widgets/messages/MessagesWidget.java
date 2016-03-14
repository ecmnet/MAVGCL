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
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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


	public MessagesWidget() {

		super(300);


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

		control.addMAVMessageListener( new IMAVMessageListener() {


			@Override
			public void messageReceived(List<LogMessage> ml, LogMessage m) {
				fadeProperty().setValue(true);

				if(f!=null)
					f.cancel(true);

				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						listview.getItems().add(fo.format(new Date(m.tms))+" : \t"+m.msg);
						listview.scrollTo(listview.getItems().size()-1);
					}

				});

				showMessages();

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

		if(listview.getItems().size()<1)
			return;

		fadeProperty().setValue(true);
		f = ExecutorService.get().schedule(new Runnable() {
			@Override
			public void run() {
				fadeProperty().setValue(false);
			}
		},3,TimeUnit.SECONDS);
	}

}
