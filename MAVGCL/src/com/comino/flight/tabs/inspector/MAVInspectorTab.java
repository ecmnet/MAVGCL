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

package com.comino.flight.tabs.inspector;

import java.io.IOException;

import com.comino.flight.MainApp;
import com.comino.flight.widgets.battery.BatteryWidget;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.line.LineChartWidget;
import com.comino.flight.widgets.charts.xy.XYChartWidget;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.mav.control.IMAVController;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

public class MAVInspectorTab extends BorderPane {

	@FXML
	private TreeTableView<String> treetableview;

	@FXML
	private TreeTableColumn message_col;

	@FXML
	private TreeTableColumn variable_col;

	@FXML
	private TreeTableColumn value_col;

	@FXML
	private TreeTableColumn unit_col;

	final TreeItem<String> root = new TreeItem<String>("Messages");

	public MAVInspectorTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVInspectorTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	@FXML
	private void initialize() {

	 treetableview.setRoot(root);
     root.setExpanded(true);

	}

	public MAVInspectorTab setup(IMAVController control) {

		return this;
	}






}
