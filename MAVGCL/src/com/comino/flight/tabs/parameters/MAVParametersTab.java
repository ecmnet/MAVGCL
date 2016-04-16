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

package com.comino.flight.tabs.parameters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_request_read;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.DeviceStateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVParametersTab extends BorderPane implements IMAVLinkListener {

	@FXML
	private TreeTableView<Parameter> treetableview;

	@FXML
	private TreeTableColumn<Parameter, String> message_col;

	@FXML
	private TreeTableColumn<Parameter, String> variable_col;

	@FXML
	private TreeTableColumn<Parameter, String>  value_col;

	@FXML
	private TreeTableColumn<Parameter, String>  desc_col;


	private final ObservableMap<String,ParameterGroup> groups = FXCollections.observableHashMap();

	private Task<Boolean> task;

	private IMAVController control;

	private ParameterFactMetaData metadata = null;


	public MAVParametersTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVParametersTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		metadata = new ParameterFactMetaData("PX4ParameterFactMetaData.xml");

		task = new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception {
				getParameterList();
				return true;
			}
		};

	}

	@FXML
	private void initialize() {


		TreeItem<Parameter> root = new TreeItem<Parameter>(new Parameter("","",""));
		treetableview.setRoot(root);
		treetableview.setShowRoot(false);
		root.setExpanded(true);

		message_col.setCellValueFactory((param) -> {
			return param.getValue().isLeaf() ? new SimpleStringProperty("") : param.getValue().getValue().strProperty();
		});

		message_col.setSortType(SortType.ASCENDING);

		variable_col.setCellValueFactory(new Callback<CellDataFeatures<Parameter, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Parameter, String> param) {
				return param.getValue().isLeaf() ? param.getValue().getValue().strProperty() : new SimpleStringProperty("");
			}
		});



		value_col.setCellValueFactory(new Callback<CellDataFeatures<Parameter, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Parameter, String> param) {
				return param.getValue().getValue().getParamValue();
			}
		});

		value_col.setStyle( "-fx-alignment: CENTER-RIGHT;");

		desc_col.setCellValueFactory(new Callback<CellDataFeatures<Parameter, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Parameter, String> param) {
				return param.getValue().getValue().getDescription();
			}
		});

		desc_col.setStyle("-fx-padding: 0 0 0 30");


	}


	public MAVParametersTab setup(IMAVController control) {
		this.control = control;
		control.addMAVLinkListener(this);

		DeviceStateProperties.getInstance().getConnectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
				new Thread(task).start();
			}
		});
		return this;
	}

	@Override
	public void received(Object _msg) {

		if( _msg instanceof msg_param_value)
			parseMessageString((msg_param_value)_msg);

	}

	private synchronized void parseMessageString(msg_param_value msg) {

		if(msg.param_id[0]=='_')
			return;

		ParameterAttributes attributes = metadata.getMetaData(msg.getParam_id());
		if(attributes == null) {
			attributes = new ParameterAttributes(msg.getParam_id(),"(DefaultGroup)");
		}

		TreeItem<Parameter> p = null;
		ParameterGroup group = null;
		Parameter parameter = null;

		group = groups.get(attributes.group_name);
		if(group == null) {
			p = new TreeItem<Parameter>(new Parameter(attributes.group_name,"",""));
			p.setExpanded(false);
			treetableview.getRoot().getChildren().add(p);
			group = new ParameterGroup(attributes.group_name, p);
			groups.put(attributes.group_name, group);
		} else {
			p = group.getTreeItem();
		}

		parameter = group.get(msg.getParam_id());
		if(parameter == null) {
			if(attributes.decimals>0)
			  parameter = new Parameter(msg.getParam_id(), String.valueOf(msg.param_value),attributes.description);
			else
			  parameter = new Parameter(msg.getParam_id(), String.valueOf((int)msg.param_value),attributes.description);
			group.getData().put(msg.getParam_id(), parameter);
			TreeItem<Parameter> treeItem = new TreeItem<Parameter>(parameter);
			p.getChildren().add(treeItem);

		} else {
			if(attributes.decimals>0)
			  parameter.setValue(Float.toString(msg.param_value));
			else
			  parameter.setValue(String.valueOf((int)msg.param_value));
		}
	}


	private void getParameterList() {
		msg_param_request_list msg = new msg_param_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);

	}


	class ParameterGroup {

		private TreeItem<Parameter> p = null;
		private StringProperty name = new SimpleStringProperty();
		private Map<String,Parameter> data = FXCollections.observableHashMap();

		public ParameterGroup(String name,TreeItem<Parameter> p ) {
			this.p = p;
			this.name.set(name);
		}

		public Map<String,Parameter> getData() {
			return data;
		}

		public TreeItem<Parameter> getTreeItem() {
			return p;
		}

		public StringProperty strProperty() {
			return name;
		}

		public String getName() {
			return name.get();
		}

		public Parameter get(String name) {
			return data.get(name);
		}
	}

	class Parameter {

		long tms = 0;

		StringProperty str = new SimpleStringProperty();
		StringProperty desc = new SimpleStringProperty();
		StringProperty value = new SimpleStringProperty();

		public Parameter(String s, String n, String d) {
			str.set(s);
			value.set(n);
			desc.set(d);
		}



		public StringProperty getParamValue() {
			return value;
		}

		public StringProperty getDescription() {
			return desc;
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
