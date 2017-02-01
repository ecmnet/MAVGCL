/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.info;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVMessageListener;
import com.comino.msp.model.segment.LogMessage;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class InfoWidget extends WidgetPane  {

	private static final int MAX_ITEMS = 200;

	@FXML
	private ListView<LogMessage> listview;


	public InfoWidget() {
		super(300, true);
		FXMLLoadHelper.load(this, "InfoWidget.fxml");
	}

	@FXML
	private void initialize() {

		listview.setCellFactory(list -> new ListCell<LogMessage>() {

			@Override
			protected void updateItem(LogMessage m, boolean empty) {
				super.updateItem(m,empty);
				if(!empty) {
					setPrefWidth(130);
					setWrapText(true);
					switch(m.severity) {
					case MAV_SEVERITY.MAV_SEVERITY_NOTICE:
						setStyle("-fx-text-fill:lightblue;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
						setStyle("-fx-text-fill:lightgreen;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_WARNING:
						setStyle("-fx-text-fill:wheat;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
						setStyle("-fx-text-fill:salmon;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_EMERGENCY:
						setStyle("-fx-text-fill:tomato;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_ERROR:
						setStyle("-fx-text-fill:orange;");
						break;
					default:
						setStyle("-fx-text-fill:darkcyan;");
					}
					setText(m.msg);
				}
			}
		});
	}

	public void setup(IMAVController control) {


		control.addMAVMessageListener( new IMAVMessageListener() {

			@Override
			public void messageReceived(LogMessage message) {

				if(message.severity< MAV_SEVERITY.MAV_SEVERITY_DEBUG ) {
					final LogMessage m = message;

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							listview.getItems().add(m);
							if(listview.getItems().size()>MAX_ITEMS)
								listview.getItems().remove(0);
							Platform.runLater(() -> {
								listview.scrollTo(listview.getItems().size()-1);
							});
						}
					});
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

	public void clear() {

	}

}
