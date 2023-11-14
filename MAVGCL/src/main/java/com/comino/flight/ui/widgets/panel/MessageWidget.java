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

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_event;
import org.mavlink.messages.lquac.msg_statustext;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.MainApp;
import com.comino.flight.events.MAVEventMataData;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavcom.model.segment.LogMessage;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MessageWidget extends ChartControlPane implements IChartControl {

	@FXML
	private Label g_message;

	private final AnalysisModelService dataService = AnalysisModelService.getInstance();
	private final MAVEventMataData     eventMetaData = MAVEventMataData.getInstance( );

	private AnimationTimer task;

	private AnalysisDataModel model;
	private LogMessage  message;

	private long tms = 0;

	private FloatProperty   replay       = new SimpleFloatProperty(0);
	private StateProperties state        = null;

	public MessageWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "MessageWidget.fxml");

		task = new AnimationTimer() {
			@Override public void handle(long now) {
				if(!isDisabled() && MainApp.getPrimaryStage().isFocused()) {
					if(message != null && message.text!=null) {
						g_message.setText(message.toString());
						setVisible(true);
						tms = System.currentTimeMillis();
					}

					if((System.currentTimeMillis()-tms)>1500)
						setVisible(false);

				}
			}
		};
	}


	@FXML
	private void initialize() {
		this.state = StateProperties.getInstance();
		this.model = dataService.getCurrent();
		this.disableProperty().bind(state.getConnectedProperty().not().and(state.getReplayingProperty().not()));
		this.disabledProperty().addListener((v,ov,nv) -> {
			if(!nv.booleanValue())
				task.start();
			else
				task.stop();
		});
	}


	public void setup(IMAVController control) {
		ChartControlPane.addChart(6,this);
		setVisible(false);
		replay.addListener((v, ov, nv) -> {
			Platform.runLater(() -> {
				if(nv.intValue()<=1) {
					message = dataService.getModelList().get(1).msg;
				} else
					message = dataService.getModelList().get(nv.intValue()).msg;
			});
		});

		control.addMAVLinkListener(new IMAVLinkListener() {
			@Override
			public void received(Object o) {
				if(o instanceof msg_statustext && !isDisabled()) {
					msg_statustext msg = (msg_statustext) o;
					String s = (new String(msg.text));
					if(!s.contains("/t")) {
						message = new LogMessage();
						message.text = s.trim();
						message.severity = msg.severity;
					}
				}
			}
		});

		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_event) {
				msg_event msg = (msg_event)o;
				if((msg.log_levels >> 4 & 0x0F) < MAV_SEVERITY.MAV_SEVERITY_DEBUG) {
					message = new LogMessage();
					message.text = eventMetaData.buildMessageFromMAVLink(msg).trim();
					message.severity = (msg.log_levels >> 4 & 0x0F);
				}
			}
		});

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
