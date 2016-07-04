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

package com.comino.flight.widgets.battery;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class BatteryWidget extends Pane  {

	private static final float vo_range[] = { 10.0f, 13.0f, 11.5f,  0 };
	private static final float cu_range[] = { 0.0f,  15.0f, 0,     12 };
	private static final float ca_range[] = { 0.0f,  100.0f, 60.0f, 0 };



	@FXML
	private Gauge g_voltage;

	@FXML
	private Gauge g_capacity;



	private final DecimalFormat fo = new DecimalFormat("#0.0");

	private Task<Long> task;
	private IMAVController control;
	private DataModel model;

	public BatteryWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BatteryWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					LockSupport.parkNanos(1000000000L);
					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					Platform.runLater(() -> {
						g_voltage.setValue(model.battery.b0);
						g_capacity.setValue(model.battery.p);
					});
				}
				return model.battery.tms;
			}
		};


	}


	@FXML
	private void initialize() {
		setupGauge(g_voltage,8,13,"V",Color.DARKORANGE);
		g_voltage.setDecimals(1);
		setupGauge(g_capacity,0,100,"%",Color.DEEPSKYBLUE);
		g_capacity.setDecimals(0);
	}


	private void setupGauge(Gauge gauge, float min, float max, String unit, Color color) {
		gauge.animatedProperty().set(false);
		gauge.setSkinType(SkinType.SLIM);
		gauge.setBarColor(color);
		gauge.setMinValue(min);
		gauge.setMaxValue(max);
		gauge.setDecimals(1);
		gauge.setTitle(unit);
		gauge.setUnit("Battery");
		gauge.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());
		gauge.setValueColor(Color.WHITE);
		gauge.setTitleColor(Color.WHITE);
		gauge.setUnitColor(Color.WHITE);

	}


	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
		ExecutorService.get().execute(task);
	}

}
