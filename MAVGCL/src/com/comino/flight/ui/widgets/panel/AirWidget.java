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
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.jfx.extensions.WidgetPane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.utils.MSPMathUtils;

import eu.hansolo.airseries.AirCompass;
import eu.hansolo.airseries.Horizon;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;

public class AirWidget extends WidgetPane  {

	@FXML
	private AirCompass g_compass;

	@FXML
	private Horizon g_horizon;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();

	private AnimationTimer task;

	private AnalysisDataModel model;

	private double pitch,roll,bearing;

	private long tms = 0;

	public AirWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "AirWidget.fxml");

		task = new AnimationTimer() {
			@Override public void handle(long now) {
				if(!isDisabled() && !isVisible() && (System.currentTimeMillis()-tms)>50) {
					tms = System.currentTimeMillis();
						bearing = model.getValue("HEAD");
						g_compass.setBearing(bearing);
					if(Math.abs(pitch - MSPMathUtils.fromRad(model.getValue("PITCH")))>0.05) {
						pitch = model.getValue("PITCH");
						g_horizon.setPitch(MSPMathUtils.fromRad(pitch));
					}
					if(Math.abs(roll - MSPMathUtils.fromRad(model.getValue("ROLL")))>0.05) {
						roll = model.getValue("ROLL");
						g_horizon.setRoll(MSPMathUtils.fromRad(roll));
					}
				}
			}
		};
	}


	@FXML
	private void initialize() {
		this.model = dataService.getCurrent();
		this.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());
		this.disabledProperty().addListener((v,ov,nv) -> {
			if(!nv.booleanValue())
				task.start();
			else
				task.stop();
		});
	}


	public void setup(IMAVController control) {
		//task.start();
	}

}
