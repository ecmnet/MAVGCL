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

package com.comino.flight.ui.widgets.statusline;

import java.io.IOException;
import java.util.List;

import com.comino.flight.base.UBXRTCM3Base;
import com.comino.flight.file.FileHandler;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.panel.IChartControl;
import com.comino.jfx.extensions.Badge;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class StatusLineWidget extends Pane implements IChartControl {


	@FXML
	private Badge driver;

	@FXML
	private Badge messages;

	@FXML
	private Badge time;

	@FXML
	private Badge mode;

	@FXML
	private Badge rc;

	@FXML
	private Badge gps;

	@FXML
	private Badge gpos;

	@FXML
	private Badge lpos;


	private IMAVController control;

	private FloatProperty scroll       = new SimpleFloatProperty(0);

	private AnalysisModelService collector = AnalysisModelService.getInstance();
	private StateProperties state = null;

	private String filename;

	private Timeline task = null;
	private Timeline out = null;

	private DataModel model;

	public StatusLineWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusLineWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {

			List<AnalysisDataModel> list = null;

			@Override
			public void handle(ActionEvent event) {

				if(UBXRTCM3Base.getInstance()!=null && UBXRTCM3Base.getInstance().getSVINStatus().get()) {
					gps.setMode(Badge.MODE_ON);
					gps.setText("SVIN");
				} else {
					switch(model.gps.fixtype) {

					case 2:
						gps.setMode(Badge.MODE_ON);
						gps.setText("GPS");
					case 3:
						gps.setMode(Badge.MODE_ON);
						gps.setText("GPS Fix");
						break;
					case 4:
						gps.setMode(Badge.MODE_ON);
						gps.setText("DGPS");
						break;
					case 5:
						gps.setMode(Badge.MODE_ON);
						gps.setText("RTK float");
						break;
					case 6:
						gps.setMode(Badge.MODE_ON);
						gps.setText("RTK fixed");
						break;

					default:
						gps.setText("No GPS");
						gps.setMode(Badge.MODE_OFF);
					}
				}

				filename = FileHandler.getInstance().getName();

				if(control.isConnected()) {
					messages.setMode(Badge.MODE_ON);
					driver.setText(control.getCurrentModel().sys.getSensorString());
					if(!control.getCurrentModel().sys.isSensorAvailable(Status.MSP_IMU_AVAILABILITY))
						driver.setBackgroundColor(Color.DARKRED);
					else
						driver.setBackgroundColor(Color.DARKCYAN);
					driver.setMode(Badge.MODE_ON);
				} else {
					messages.setMode(Badge.MODE_OFF);
					driver.setText("no sensor info available");
					driver.setMode(Badge.MODE_OFF);
				}

				list = collector.getModelList();

				if(list.size()>0) {

					int current_x0_pt = collector.calculateX0IndexByFactor(scroll.floatValue());
					int current_x1_pt = collector.calculateX1IndexByFactor(scroll.floatValue());
					time.setText(
							String.format("TimeFrame: [ %1$tM:%1$tS - %2$tM:%2$tS ]",
									list.get(current_x0_pt).tms/1000,
									list.get(current_x1_pt).tms/1000)
							);
					time.setBackgroundColor(Color.DARKCYAN);
				} else {
					time.setText("TimeFrame: [ 00:00 - 00:00 ]");
					time.setBackgroundColor(Color.GRAY);
				}

				if(!state.getLogLoadedProperty().get()) {
					if(control.isConnected()) {
						time.setMode(Badge.MODE_ON);
						if(model.sys.isStatus(Status.MSP_SITL)) {
							mode.setBackgroundColor(Color.BEIGE);
							mode.setText("SITL");
						} else {
							mode.setBackgroundColor(Color.DARKCYAN);
							mode.setText("Connected");
						}
						mode.setMode(Badge.MODE_ON);
					} else {
						mode.setMode(Badge.MODE_OFF); mode.setText("offline");
						time.setMode(Badge.MODE_OFF);
					}
				} else {
					time.setMode(Badge.MODE_ON);
					messages.clear();
					mode.setBackgroundColor(Color.LIGHTSKYBLUE);
					mode.setText(filename);
					mode.setMode(Badge.MODE_ON);
				}

			}
		} ) );

		task.setCycleCount(Timeline.INDEFINITE);
		driver.setAlignment(Pos.CENTER_LEFT);
	}

	public void setup(ChartControlWidget chartControlWidget, IMAVController control) {
		chartControlWidget.addChart(99,this);
		this.control = control;
		this.model = control.getCurrentModel();
		this.state = StateProperties.getInstance();

		messages.setText(control.getClass().getSimpleName()+ " loaded");
		messages.setBackgroundColor(Color.GRAY);

		control.addMAVMessageListener(msg -> {
			Platform.runLater(() -> {
				if(filename!=null && filename.isEmpty())  {
					out.stop();
					messages.setText(msg.msg);
					out.play();
				}
			});
		});

		control.getStatusManager().addListener(Status.MSP_RC_ATTACHED, (o,n) -> {
			if((n.isStatus(Status.MSP_RC_ATTACHED)) && n.isStatus(Status.MSP_CONNECTED))
				rc.setMode(Badge.MODE_ON);
			else
				rc.setMode(Badge.MODE_OFF);
		});

		control.getStatusManager().addListener(Status.MSP_GPOS_VALID, (o,n) -> {
			if((n.isStatus(Status.MSP_GPOS_VALID)) && n.isStatus(Status.MSP_CONNECTED))
				gpos.setMode(Badge.MODE_ON);
			else
				gpos.setMode(Badge.MODE_OFF);
		});

		control.getStatusManager().addListener(Status.MSP_LPOS_VALID, (o,n) -> {
			if((n.isStatus(Status.MSP_LPOS_VALID)) && n.isStatus(Status.MSP_CONNECTED))
				lpos.setMode(Badge.MODE_ON);
			else
				lpos.setMode(Badge.MODE_OFF);
		});

		out = new Timeline(new KeyFrame(
				Duration.millis(5000),
				ae -> { messages.clear();  }));

		task.play();
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public IntegerProperty getTimeFrameProperty() {
		return null;
	}

	public BooleanProperty getIsScrollingProperty() {
		return null;
	}

	@Override
	public void refreshChart() {

	}

	public void setKeyFigureSeletcion(KeyFigurePreset preset) {

	}

	public KeyFigurePreset getKeyFigureSelection() {
		return null;
	}


}
