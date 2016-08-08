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

package com.comino.flight.widgets.air;

import java.text.DecimalFormat;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.fx.controls.WidgetPane;
import com.comino.mav.control.IMAVController;

import eu.hansolo.airseries.AirCompass;
import eu.hansolo.airseries.Horizon;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

public class AirWidget extends WidgetPane  {

	@FXML
	private AirCompass g_compass;

	@FXML
	private Horizon g_horizon;

	private AnalysisModelService dataService = AnalysisModelService.getInstance();

	private final DecimalFormat fo = new DecimalFormat("#0.0");

	private Task<Integer> task;
	private AnalysisDataModel model;

	private float pitch,roll,bearing;

	public AirWidget() {
		super(300,true);

		FXMLLoadHelper.load(this, "AirWidget.fxml");

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					Thread.sleep(100);
					if(isDisabled() || !isVisible()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					Platform.runLater(() -> {
						if(Math.abs(bearing - model.getValue("HEAD"))>2) {
							bearing = model.getValue("HEAD");
							g_compass.setBearing(bearing);
						}
						if(Math.abs(pitch - model.getValue("PITCH"))>2) {
							pitch = model.getValue("PITCH");
							g_horizon.setPitch(pitch);
						}
						if(Math.abs(roll - model.getValue("ROLL"))>2) {
							roll = model.getValue("ROLL");
							g_horizon.setRoll(roll);
						}
					});
				}
				return 0;
			}
		};


	}


	@FXML
	private void initialize() {
		this.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());


	}


	public void setup(IMAVController control) {
		this.model = dataService.getCurrent();
		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
		th.setDaemon(true);
		th.start();
	}

}
