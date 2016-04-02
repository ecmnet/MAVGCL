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

package com.comino.flight.tabs.inspector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVInspectorTab extends BorderPane implements IMAVLinkListener {

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
		control.addMAVLinkListener(this);
		return this;
	}

	@Override
	public void received(Object _msg) {
		System.out.println(this.isDisabled());
		if(!this.isDisabled())
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

		long tms = 0;

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
			if((System.currentTimeMillis() - tms)> 100) {
				tms = System.currentTimeMillis();
				this.value.set(no);
			}
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
