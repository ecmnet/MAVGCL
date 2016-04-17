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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.mavlink.messages.lquac.msg_param_request_list;
import org.mavlink.messages.lquac.msg_param_value;

import com.comino.flight.observables.DeviceStateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;

public class MAVParametersTab extends BorderPane implements IMAVLinkListener {

	@FXML
	private TreeTableView<Parameter> treetableview;

	@FXML
	private TreeTableColumn<Parameter, Parameter> message_col;

	@FXML
	private TreeTableColumn<Parameter, Parameter> variable_col;

	@FXML
	private TreeTableColumn<Parameter, Parameter>  value_col;

	@FXML
	private TreeTableColumn<Parameter, Parameter>  unit_col;

	@FXML
	private TreeTableColumn<Parameter, Parameter>  desc_col;


	private final Map<String,ParameterGroup> groups = new HashMap<String,ParameterGroup>();;

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


		TreeItem<Parameter> root = new TreeItem<Parameter>(new Parameter(""));
		treetableview.setRoot(root);
		treetableview.setShowRoot(false);
		root.setExpanded(true);

		message_col.setSortType(SortType.ASCENDING);
		message_col.setCellValueFactory(cellData -> {
			return cellData.getValue().getValue();
		});

		message_col.setComparator(new Comparator<Parameter>() {

			@Override
			public int compare(Parameter o1, Parameter o2) {
				if(o1.group!=null && o2.group!=null)
				  return o1.group.compareTo(o2.group);
				return 0;
			}
		});

		message_col.setCellFactory(column -> {
			return new TreeTableCell<Parameter, Parameter>() {

				@Override
				protected void updateItem(Parameter item, boolean empty) {
					if(!empty) {
						setText(item.getGroupName());
						setStyle("-fx-text-fill: #D0D0F0;");
					} else
						setText("");
				}
			};
		});

		variable_col.setCellValueFactory(cellData -> {
			if(cellData.getValue().isLeaf())
				return cellData.getValue().getValue();
			else
				return new Parameter("");
		});

		variable_col.setCellFactory(column -> {
			return new TreeTableCell<Parameter, Parameter>() {

				@Override
				protected void updateItem(Parameter item, boolean empty) {
					if(!empty && item.att!=null) {
						setText(item.getName());
						setStyle("-fx-text-fill: #80F080;");
					} else
						setText("");
				}
			};
		});

		value_col.setCellValueFactory(cellData -> {
			if(cellData.getValue().isLeaf())
				return cellData.getValue().getValue();
			else
				return new Parameter("");
		});


		value_col.setCellFactory(column -> {
			return new TreeTableCell<Parameter, Parameter>() {

				@Override
				protected void updateItem(Parameter item, boolean empty) {
					if(!empty && item.att!=null) {
						if(item.att.type.contains("INT"))
							setText(String.valueOf((int)item.getParamValue()));
						else
							setText(String.valueOf(item.getParamValue()));
						if(item.isDefault()) {
							setStyle("-fx-text-fill: #F0F0F0; -fx-alignment: CENTER-RIGHT;");
						}
						else {
							setStyle("-fx-text-fill: #F0D080; -fx-alignment: CENTER-RIGHT;");
						}
					} else
						setText("");
				}
			};
		});

		unit_col.setCellValueFactory(cellData -> {
			if(cellData.getValue().isLeaf())
				return cellData.getValue().getValue();
			else
				return new Parameter("");
		});

		unit_col.setCellFactory(column -> {
			return new TreeTableCell<Parameter, Parameter>() {

				@Override
				protected void updateItem(Parameter item, boolean empty) {
					if(!empty && item.att!=null) {
						setText(item.getUnit());
					} else
						setText("");
				}
			};
		});


		desc_col.setCellValueFactory(cellData -> {
			if(cellData.getValue().isLeaf())
				return cellData.getValue().getValue();
			else
				return new Parameter("");
		});


		desc_col.setCellFactory(column -> {
			return new TreeTableCell<Parameter, Parameter>() {

				@Override
				protected void updateItem(Parameter item, boolean empty) {
					if(!empty && item.att!=null) {
						setText(item.getDescription());
					} else
						setText("");
				}
			};
		});

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
			p = new TreeItem<Parameter>(new Parameter(attributes.group_name));
			p.setExpanded(false);
			treetableview.getRoot().getChildren().add(p);
			group = new ParameterGroup(attributes.group_name, p);
			groups.put(attributes.group_name, group);
		} else {
			p = group.getTreeItem();
		}

		parameter = group.get(msg.getParam_id());
		if(parameter == null) {
			parameter = new Parameter(attributes, msg.param_value);
			group.getData().put(msg.getParam_id(), new Parameter(attributes, msg.param_value));
			TreeItem<Parameter> treeItem = new TreeItem<Parameter>(parameter);
			p.getChildren().add(treeItem);

		} else {
			parameter.setValue(msg.param_value);
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
		private Map<String,Parameter> data = new HashMap<String,Parameter>();

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



	class Parameter extends ObjectBinding<Parameter> {

		private ParameterAttributes att = null;
		private String group = null;
		private float value = 0;

		public Parameter(ParameterAttributes att, float value) {
			this.att = att;
			this.value = value;
		}

		public Parameter(String group) {
			this.group = group;
		}

		public float getParamValue() {
			return value;
		}


		public void setValue(float no) {
			value = no;
		}

		public String getName() {
			return att.name;
		}

		public String getGroupName() {
			return group;
		}

		public String getDescription() {
			return att.description;
		}

		public String getUnit() {
			return att.unit;
		}

		public boolean isDefault() {
			return value == att.default_val;
		}

		@Override
		protected Parameter computeValue() {
			return this;
		}
	}







}
