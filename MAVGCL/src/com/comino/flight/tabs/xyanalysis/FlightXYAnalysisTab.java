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

package com.comino.flight.tabs.xyanalysis;

import java.io.IOException;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.xy.XYChartWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class FlightXYAnalysisTab extends Pane {


	@FXML
	private XYChartWidget chart1;

	public FlightXYAnalysisTab() {
		FXMLLoadHelper.load(this, "FlightXYAnalysisTab.fxml");
	}

	@FXML
	private void initialize() {
		chart1.prefWidthProperty().bind(widthProperty());
		chart1.disableProperty().bind(this.disabledProperty());
	}


	public void setup(ChartControlWidget recordControl,IMAVController control) {
		recordControl.addChart(chart1.setup(control));
		chart1.prefHeightProperty().bind(heightProperty().subtract(1));
	}

	public void setWidthBinding(double horizontal_space) {
		chart1.prefWidthProperty().bind(widthProperty().subtract(horizontal_space));
	}





}
