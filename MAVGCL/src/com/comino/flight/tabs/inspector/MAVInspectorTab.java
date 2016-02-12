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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkMsgListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVInspectorTab extends BorderPane implements IMAVLinkMsgListener {

	@FXML
	private TreeTableView<Dataset> treetableview;

	@FXML
	private TreeTableColumn<Dataset, String> message_col;

	@FXML
	private TreeTableColumn<Dataset, String> variable_col;

	@FXML
	private TreeTableColumn<Dataset, String>  value_col;


	final ObservableMap<String,Data> allData = FXCollections.observableHashMap();

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


		TreeItem<Dataset> root = new TreeItem<Dataset>(new Dataset("", ""));
		treetableview.setRoot(root);
		treetableview.setShowRoot(false);
		root.setExpanded(true);

		message_col.setCellValueFactory((param) -> {
			return param.getValue().isLeaf() ? new SimpleStringProperty("") : param.getValue().getValue().strProperty();
		});

		variable_col.setCellValueFactory(new Callback<CellDataFeatures<Dataset, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Dataset, String> param) {
				return param.getValue().isLeaf() ? param.getValue().getValue().strProperty() : new SimpleStringProperty("");
			}
		});

		variable_col.setSortType(SortType.ASCENDING);

		value_col.setCellValueFactory(new Callback<CellDataFeatures<Dataset, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Dataset, String> param) {
				return param.getValue().getValue().getValue();
			}
		});

		value_col.setStyle( "-fx-alignment: CENTER-RIGHT;");


	}


	public MAVInspectorTab setup(IMAVController control) {
		control.addMAVLinkMsgListener(this);
		return this;
	}

	@Override
	public void received(Object _msg) {
			parseMessageString(_msg.toString().split(" "));
	}

	private synchronized void parseMessageString(String[] msg) {
		String _msg = msg[0].trim();

		if(!allData.containsKey(_msg)) {

			ObservableMap<String,Dataset> variables =  FXCollections.observableHashMap();

			for(String v : msg)
             if(v.contains("=")) {
            	String[] p = v.split("=");
			    variables.put(p[0], new Dataset(p[0],p[1]));
			}

			Data data = new Data(_msg,variables);
			allData.put(_msg,data);

			TreeItem<Dataset> ti = new TreeItem<>(new Dataset(data.getName(), null));
			ti.setExpanded(false);
			treetableview.getRoot().getChildren().add(ti);
			for (Dataset dataset : data.getData().values()) {
				TreeItem treeItem = new TreeItem(dataset);
				ti.getChildren().add(treeItem);
			}
		} else {

			Data data = allData.get(_msg);
			for(String v : msg)
	             if(v.contains("=")) {
	            	String[] p = v.split("=");
				    data.getData().get(p[0]).setValue(p[1]);
				}
		}

	}


	class Data {

		private String name;
		private Map<String,Dataset> data = new HashMap<String,Dataset>();

		public Data(String name, ObservableMap<String,Dataset> data) {
			this.name = name;
			this.data = data;
		}

		public Map<String,Dataset> getData() {
			return data;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	class Dataset {

		StringProperty str = new SimpleStringProperty();
		StringProperty value = new SimpleStringProperty();

		public Dataset(String s, String n) {
			str.set(s);
			value.set(n);
		}


		public StringProperty getValue() {
			return value;
		}

		public void setValue(String no) {
			this.value.set(no);
		}

		public String getStr() {
			return str.get();
		}

		public StringProperty strProperty() {
			return str;
		}

		public void setStr(String str) {
			this.str.set(str);
		}
	}







}
