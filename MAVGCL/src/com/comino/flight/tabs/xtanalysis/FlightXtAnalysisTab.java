/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.tabs.xtanalysis;

import java.io.IOException;

import com.comino.flight.MainApp;
import com.comino.flight.widgets.analysiscontrol.AnalysisControlWidget;
import com.comino.flight.widgets.battery.BatteryWidget;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.linechart.LineChartWidget;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class FlightXtAnalysisTab extends Pane {


	@FXML
	private LineChartWidget chart1;

	@FXML
	private LineChartWidget chart2;


	public FlightXtAnalysisTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FlightXtAnalysisTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}



	public void setup(AnalysisControlWidget recordControl,IMAVController control) {

		chart1.prefWidthProperty().bind(widthProperty());
		chart2.prefWidthProperty().bind(widthProperty());

		recordControl.addChart(chart1.setup(control));
		recordControl.addChart(chart2.setup(control));




	}



}
