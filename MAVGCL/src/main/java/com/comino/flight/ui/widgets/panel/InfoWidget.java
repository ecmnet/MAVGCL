/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.widgets.panel;

import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.IMAVMessageListener;
import com.comino.mavcom.model.segment.LogMessage;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;

public class InfoWidget extends ChartControlPane implements IChartControl {

	private static final int MAX_ITEMS = 200;

	@FXML
	private ListView<LogMessage> listview;

	private long tms_old = 0;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();

	private boolean debug_message_enabled = false;
	private FloatProperty   replay       = new SimpleFloatProperty(0);
	private StateProperties state        = null;


	public InfoWidget() {
		super(300, true);
		FXMLLoadHelper.load(this, "InfoWidget.fxml");
	}

	@FXML
	private void initialize() {

		this.state = StateProperties.getInstance();

		listview.prefHeightProperty().bind(this.heightProperty().subtract(13));

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
						setStyle("-fx-text-fill:yellow;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_ALERT:
						setStyle("-fx-text-fill:yellowgreen;");
						break;
					default:
						setStyle("-fx-text-fill:white;");
					}
					setText(m.text);
				} else
					setText(null);
			}
		});

		ContextMenu ctxm = new ContextMenu();
		MenuItem cmItem1 = new MenuItem("Clear list");
		cmItem1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				listview.getItems().clear();
			}
		});
		ctxm.getItems().add(cmItem1);
		listview.setContextMenu(ctxm);
	}

	public void setup(IMAVController control) {

		Preferences userPrefs = MAVPreferences.getInstance();
		this.debug_message_enabled = userPrefs.getBoolean(MAVPreferences.DEBUG_MSG, false);
		ChartControlPane.addChart(7,this);
		control.addMAVMessageListener( new IMAVMessageListener() {

			@Override
			public void messageReceived(LogMessage message) {

				if(message.severity ==  MAV_SEVERITY.MAV_SEVERITY_DEBUG && !debug_message_enabled)
					return;

				final LogMessage m = message;

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						addMessageToList(m);
					}
				});
			}
		});


		state.getReplayingProperty().addListener((v,o,n) -> {
			if(n.booleanValue())
				Platform.runLater(() -> {
					listview.getItems().clear();
				});
		});

		replay.addListener((v, ov, nv) -> {
			Platform.runLater(() -> {
				final LogMessage message;
				if(nv.intValue()<=1) {
					message = dataService.getModelList().get(1).msg;
				} else
					message = dataService.getModelList().get(nv.intValue()).msg;
				addMessageToList(message);
			});
		});

	}

	public void clear() {
		Platform.runLater(() -> {
			listview.getItems().clear();
		});
	}

	private void addMessageToList(LogMessage m) {

		if(m==null || m.text==null)
			return;

		if(listview.getItems().size() == 0 ||
				!m.text.contentEquals(listview.getItems().get(listview.getItems().size()-1).text) ||
				System.currentTimeMillis() - tms_old > 1000	) {
			tms_old = System.currentTimeMillis();
			listview.getItems().add(m);
			if(listview.getItems().size()>MAX_ITEMS)
				listview.getItems().remove(0);
			listview.scrollTo(listview.getItems().size()-1);
			listview.getSelectionModel().select(-1);
		}
	}

	@Override
	public IntegerProperty getTimeFrameProperty() {

		return null;
	}

	@Override
	public FloatProperty getScrollProperty() {

		return null;
	}

	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}

	@Override
	public BooleanProperty getIsScrollingProperty() {

		return null;
	}

	@Override
	public void refreshChart() {

	}

	@Override
	public KeyFigurePreset getKeyFigureSelection() {

		return null;
	}

	@Override
	public void setKeyFigureSelection(KeyFigurePreset preset) {

	}

}
