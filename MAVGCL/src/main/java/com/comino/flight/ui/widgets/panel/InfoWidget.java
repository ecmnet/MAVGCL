/****************************************************************************
 *
 *   Copyright (c) 2017,2021 Eike Mansfeld ecm@gmx.de. All rights reserved.
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
import org.mavlink.messages.lquac.msg_event;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.events.MAVEventMataData;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.IMAVMessageListener;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;

public class InfoWidget extends ChartControlPane implements IChartControl {

	private static final int MAX_ITEMS = 50;

	@FXML
	private ListView<LogMessage> listview;

	private final AnalysisModelService dataService = AnalysisModelService.getInstance();
	private final MAVEventMataData     eventMetaData = MAVEventMataData.getInstance( );

	private boolean debug_message_enabled = false;
	private FloatProperty   replay       = new SimpleFloatProperty(0);
	private StateProperties state        = null;

	private boolean is_light = false;
	
	private static final String EMPTY = "";


	public InfoWidget() {
		super(0, true);
		FXMLLoadHelper.load(this, "InfoWidget.fxml");
		
		if(MAVPreferences.getInstance().get(MAVPreferences.PREFS_THEME,"").contains("Light"))
			is_light = true;
	}
	@FXML
	private void initialize() {

		this.state = StateProperties.getInstance();

		listview.prefHeightProperty().bind(this.heightProperty().subtract(18));
		listview.maxHeightProperty().bind(this.heightProperty().subtract(18));
		
		listview.setCellFactory(list -> new ListCell<LogMessage>() {

			@Override
			protected void updateItem(LogMessage m, boolean empty) {
		
				super.updateItem(m, empty);
//				setGraphic(null);
				if(!empty && m!=null) {
					setPrefWidth(140);
					setWrapText(false);
					setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
					
					switch(m.severity) {
					case MAV_SEVERITY.MAV_SEVERITY_NOTICE:
						if(is_light) setStyle("-fx-text-fill:darkblue;"); else setStyle("-fx-text-fill:lightblue;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
						if(is_light) setStyle("-fx-text-fill:green;"); else setStyle("-fx-text-fill:lightgreen;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_WARNING:
						if(is_light) setStyle("-fx-text-fill:Chocolate;"); else setStyle("-fx-text-fill:wheat;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
						if(is_light) setStyle("-fx-text-fill:red;"); else setStyle("-fx-text-fill:salmon;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_EMERGENCY:
						if(is_light) setStyle("-fx-text-fill:red;"); else setStyle("-fx-text-fill:tomato;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_ERROR:
						if(is_light) setStyle("-fx-text-fill:red;"); else setStyle("-fx-text-fill:yellow;");
						break;
					case MAV_SEVERITY.MAV_SEVERITY_ALERT:
						if(is_light) setStyle("-fx-text-fill:red;"); else setStyle("-fx-text-fill:yellowgreen;");
						break;
					default:
						if(is_light) setStyle("-fx-text-fill:black;"); else setStyle("-fx-text-fill:white;");
					}
					setText(m.text);
				} else {
					setText(EMPTY);
				}
		   }
		});


		ContextMenu ctxm = new ContextMenu();
		MenuItem cmItem1 = new MenuItem("Clear list");
		cmItem1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				clear();
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

				addMessageToList(message);
			}
		});
		
		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_event) {
				msg_event msg = (msg_event)o;
				if((msg.log_levels >> 4 & 0x0F) < MAV_SEVERITY.MAV_SEVERITY_DEBUG) {
					LogMessage message = new LogMessage();
					message.text = eventMetaData.buildMessageFromMAVLink(msg).trim();
					message.severity = (msg.log_levels >> 4 & 0x0F);
					control.getCurrentModel().msg.set(message);
					addMessageToList(message);
				}
			}
		});


		state.getReplayingProperty().addListener((v,o,n) -> {
			if(n.booleanValue())
				clear();
		});

		replay.addListener((v, ov, nv) -> {
				final LogMessage message;
				if(nv.intValue()<=1) {
					message = dataService.getModelList().get(1).msg;
				} else
					message = dataService.getModelList().get(nv.intValue()).msg;
				addMessageToList(message);
		});

	}

	public void clear() {

		Platform.runLater(() -> {
			listview.getItems().clear();
		});

	}

	private void addMessageToList(final LogMessage m) {

		if(m==null || m.text==null)
			return;
		
		if(m.isNew()) {
			
			Platform.runLater(() -> {
				listview.getItems().add(m);
				if(listview.getItems().size()>=MAX_ITEMS)
					listview.getItems().remove(0,1);
				listview.scrollTo(m);
			});
		
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
