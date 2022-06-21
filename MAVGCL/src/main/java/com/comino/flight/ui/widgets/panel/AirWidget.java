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

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.MainApp;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;

import eu.hansolo.airseries.AirCompass;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

public class AirWidget extends ChartControlPane implements IChartControl {

	@FXML
	private AirCompass g_compass;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();

	private AnimationTimer task;

	private AnalysisDataModel model;

	private long tms = 0;

	private FloatProperty   replay       = new SimpleFloatProperty(0);

	public AirWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "AirWidget.fxml");

		task = new AnimationTimer() {
			@Override public void handle(long now) {
				if(!isDisabled() && isVisible() && (System.currentTimeMillis()-tms)>50 && MainApp.getPrimaryStage().isFocused()) {
					tms = System.currentTimeMillis();
					//					System.out.println(model.getValue("HEAD"));
					if(Double.isFinite(model.getValue("HEAD")))
						g_compass.setBearing(model.getValue("HEAD"));
					else
						g_compass.setBearing(0);

				}
			}
		};
	}


	@FXML
	private void initialize() {
		this.model = dataService.getCurrent();
		
		if(MAVPreferences.isLightTheme()) {
			this.g_compass.setPlaneColor(Color.BLACK);
			this.g_compass.setOrientationColor(Color.BLACK);
		} else {
			this.g_compass.setPlaneColor(Color.CYAN.darker());
			this.g_compass.setOrientationColor(Color.CYAN.darker());
			this.g_compass.getPane().setStyle(" -fx-background-color: rgba(20.0, 30.0, 30.0, 0.75); ");
		}
		this.disableProperty().bind(this.visibleProperty().not());
		this.disabledProperty().addListener((v,ov,nv) -> {
			if(!nv.booleanValue())
				task.start();
			else
				task.stop();
		});
	}


	public void setup(IMAVController control) {
		ChartControlPane.addChart(5,this);

		replay.addListener((v, ov, nv) -> {

			if(isDisabled() || !isVisible())
				return;

			if(nv.intValue()<=1) {
				model = dataService.getModelList().get(1); 
			} else
				model = dataService.getModelList().get(nv.intValue());
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
