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

package com.comino.flight.ui.sidebar;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.mavlink.messages.MAV_PARAM_TYPE;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_param_set;

import com.comino.flight.MainApp;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.flight.ui.sidebar.bitselection.BitSelectionDialog;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.param.ParamUtils;
import com.comino.mavcom.param.ParameterAttributes;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class ParameterWidget extends ChartControlPane  {

	@FXML
	private Button reload;

	@FXML
	private GridPane grid;

	@FXML
	private ScrollPane scroll;

	@FXML
	private ComboBox<String> groups;

	private MAVGCLPX4Parameters  params;

	private List<ParamItem> items = new ArrayList<ParamItem>();

	private StateProperties state = null;

	private int timeout;
	private int timeout_count=0;


	private final WorkQueue wq = WorkQueue.getInstance();

	public ParameterWidget() {
		super();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ParameterWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}


	}

	@FXML
	public void initialize() {

		state = StateProperties.getInstance();

		params = MAVGCLPX4Parameters.getInstance();

		params.addRefreshListener(() -> {
			Platform.runLater(() -> {
				for(ParamItem i : items) {
					i.setValueOf(i.editor, i.att.value);
				}
			});
		});


		groups.getItems().add("-select group-");
		groups.setEditable(true);
		groups.getSelectionModel().clearAndSelect(0);
		groups.setVisibleRowCount(50);
		groups.getEditor().setDisable(true);

		scroll.setBorder(Border.EMPTY);
		scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		scroll.prefHeightProperty().bind(this.heightProperty().subtract(80));
		grid.setVgap(4); grid.setHgap(6);

		this.visibleProperty().addListener((e,o,n) -> {
			if(n.booleanValue() && state.getParamLoadedProperty().get()) {
				String group = MAVPreferences.getInstance().get(MAVPreferences.TUNING_GROUP, "-None-");
				groups.getSelectionModel().select(group);
			} else
				groups.getSelectionModel().select(0);
		});

		reload.setOnAction((ActionEvent event)-> {
			//	groups.getSelectionModel().clearAndSelect(0);
			params.refreshParameterList(false);
		});

		reload.disableProperty().bind(state.getArmedProperty()
				.or(state.getRecordingProperty().isNotEqualTo(AnalysisModelService.STOPPED))
				.or(state.getConnectedProperty().not()));

		params.getAttributeProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				if(newValue!=null) {
					ParameterAttributes p = (ParameterAttributes)newValue;

					Platform.runLater(() -> {
						if(!groups.getItems().contains(p.group_name) && p !=null) {
							groups.getItems().add(p.group_name);
							groups.getItems().sort(new Comparator<String>() {
								@Override
								public int compare(String o1, String o2) {
									return o1.compareTo(o2);
								}

							});
						}

						// Issue: BAT parameters are always sent back when changing a parameter.
						//System.out.println(wq.isInQueue("LP", timeout)+" P:"+p.name);
						if(wq.isInQueue("LP", timeout)) {
							BigDecimal bd = new BigDecimal(p.value).setScale(p.decimals,BigDecimal.ROUND_HALF_UP);
							MSPLogger.getInstance().writeLocalMsg("[mgc] "+p.name+" set to "+bd.toPlainString(),MAV_SEVERITY.MAV_SEVERITY_NOTICE);
							if(p.reboot_required)
								MSPLogger.getInstance().writeLocalMsg("Change of "+p.name+" requires reboot",MAV_SEVERITY.MAV_SEVERITY_NOTICE);
							wq.removeTask("LP", timeout);  timeout_count=0;
						}

					});


				}
			}
		});
	}

	public void setup(IMAVController control) {
		super.setup(control);

		groups.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if(state.getParamLoadedProperty().get() && newValue!=null) {
					prefs.put(MAVPreferences.TUNING_GROUP, newValue);
					populateParameterList(newValue);
				}
			}
		});

		grid.disableProperty().bind(state.getLogLoadedProperty().or(state.getConnectedProperty().not()));

	}

	private void populateParameterList(String group) {
		grid.setVisible(false);
		grid.getChildren().clear();
		int i = 0;
		for(ParameterAttributes p : params.getList()) {
			if(group.contains(p.group_name)) {
				Label unit = new Label(p.unit); unit.setPrefWidth(38);
				Label name = new Label(p.name); name.setPrefWidth(95);
				name.setTooltip(createParameterToolTip(p));
				ParamItem item = createParamItem(p, true);
				items.add(item);
				grid.addRow(i++, name,item.editor,unit);
			}
		}
		Platform.runLater(() -> {
			grid.setVisible(true);
		});
	}

	private ParamItem createParamItem(ParameterAttributes p, boolean editable) {
		ParamItem item = new ParamItem(p,editable);
		return item;
	}

	private Tooltip createParameterToolTip(ParameterAttributes att) {

		StringBuilder sb = new StringBuilder();
		Tooltip tooltip = new Tooltip();

		tooltip.setMaxWidth(300);
		tooltip.setWrapText(true);

		sb.append(att.name);
		sb.append("\n");
		sb.append(att.description);

		if(att.unit!=null && att.unit.length()>0)
			sb.append(" in ["+att.unit+"]");

		if(att.min_val!=0 && att.max_val!=0) {
			sb.append("\n");
			if(att.valueList.size()==0 && att.min_val > -Double.MAX_VALUE && att.max_val < Double.MAX_VALUE )
				sb.append(String.format("\nMin: %."+att.decimals+"f Max: %."+att.decimals+"f",att.min_val, att.max_val));

			if(att.valueList.size()>0)
				sb.append(String.format("\nDefault: %s",att.valueList.get((int)att.default_val)));
			else
				sb.append(String.format("\nDefault: %."+att.decimals+"f",att.default_val));
		}

		tooltip.setText(sb.toString());
		return tooltip;
	}


	private class ParamItem {

		public Control editor = null;
		private ParameterAttributes att = null;

		public ParamItem(ParameterAttributes att, boolean editable) {
			this.att= att;

			if(att.increment != 0) {
				if(att.vtype==MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32) {
					Spinner<Integer> sp = new Spinner<Integer>(att.min_val, att.max_val, att.value,1);
					sp.setEditable(true);
					this.editor = sp;
					sp.getEditor().setOnKeyPressed(keyEvent -> {
						if(keyEvent.getCode() == KeyCode.ENTER) {
							setValueOf(editor,getValueOf(sp.getEditor()));
							groups.requestFocus();
						}
						if(keyEvent.getCode() == KeyCode.ESCAPE) {
							setValueOf(editor,att.value);
							groups.requestFocus();
						}
					});
				} else {
					Spinner<Double> sp = new Spinner<Double>(new SpinnerAttributeFactory(att));
					sp.setEditable(true);
					this.editor = sp;
					sp.getEditor().setOnKeyPressed(keyEvent -> {
						if(keyEvent.getCode() == KeyCode.ENTER) {
							setValueOf(editor,getValueOf(sp.getEditor()));
							groups.requestFocus();
						}
						if(keyEvent.getCode() == KeyCode.ESCAPE) {
							setValueOf(editor,att.value);
							groups.requestFocus();
						}
					});
				}
			} else {
				if(att.valueList.size()>0) {
					ChoiceBox<Entry<Integer,String>> cb = new ChoiceBox<Entry<Integer,String>>();
					this.editor = cb;
					cb.getItems().addAll(att.valueList.entrySet());
					cb.setConverter(new StringConverter<Entry<Integer,String>>() {
						@Override
						public String toString(Entry<Integer, String> o) {
							if(o!=null)
								return o.getValue();
							return "";
						}
						@Override
						public Entry<Integer, String> fromString(String o) {
							return null;
						}
					});
					cb.getSelectionModel().
					selectedItemProperty().addListener((v,ov,nv) -> {
						groups.requestFocus();
					});
				}
				else {
					this.editor = new TextField();
					this.editor.setOnKeyPressed(keyEvent -> {
						if(keyEvent.getCode() == KeyCode.ENTER)
							groups.requestFocus();
						if(keyEvent.getCode() == KeyCode.ESCAPE) {
							setValueOf(editor,att.value);
							groups.requestFocus();
						}
					});

					if(att.bitMask!=null && att.bitMask.size()>0) {
						editor.setStyle("-fx-text-fill: #B0F0B0; -fx-control-inner-background: #606060;");
						att.decimals = 0;
						editor.setCursor(Cursor.DEFAULT);
						((TextField)editor).setEditable(false);
						editor.setOnMouseClicked((event) -> {
							groups.requestFocus();
							BitSelectionDialog bd = new BitSelectionDialog(att.bitMask);
							bd.setValue((int)att.value);
							int val = bd.show();
							if(val!=att.value) {
								att.value = val;
								setValueOf(editor,val);
								sendParameter(att,val);
							}
						});
					}
				}

				state.getLandedProperty().addListener((v,ov,nv) -> {
					if(att.reboot_required) {
						editor.setDisable(!nv.booleanValue());
					}
				});

				if(att.reboot_required)
					editor.setDisable(!state.getLandedProperty().get());
			}

			this.editor.setPrefWidth(85);
			this.editor.setPrefHeight(19);
			//		this.editor.setTooltip(new Tooltip(att.description_long));

			if(editable)
				setContextMenu(editor);
			else
				editor.setDisable(true);

			setValueOf(editor,att.value);

			this.editor.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if(!editor.isFocused() && !wq.isInQueue("LP", timeout)) {
					try {
						final float val =  getValueOf(editor);
						if(val != att.value) {
							if((val >= att.min_val && val <= att.max_val) ||
									att.min_val == att.max_val ) {
								sendParameter(att,val);
								checkDefaultOf(editor,val);
							}
							else {

								Alert alert = new Alert(AlertType.CONFIRMATION,
										att.name+" is out of bounds "+
												"(Min: "+format(att.min_val,att.decimals)+", Max: "+format(att.max_val,att.decimals)+").\n"+
												"Force saving ?",
												ButtonType.YES, ButtonType.NO);

								alert.getDialogPane().getStylesheets().add(MainApp.class.getResource("application.css").toExternalForm());
								alert.getDialogPane().getScene().setFill(Color.rgb(32,32,32));

								setDefaultButton(alert, ButtonType.NO).showAndWait();

								if (alert.getResult() == ButtonType.YES) {
									sendParameter(att,val);
									checkDefaultOf(editor,val);
								} else {
									logger.writeLocalMsg(att.name+" = "+val+" is out of bounds ("+format(att.min_val,att.decimals)+
											","+format(att.max_val,att.decimals)+"). Not saved",
											MAV_SEVERITY.MAV_SEVERITY_DEBUG);
									setValueOf(editor,att.value);
								}
							}
						}
					} catch(NumberFormatException e) {
						setValueOf(editor,att.value);
					}
				}
			});
		}

		private String format(double value, int decimals) {
			if(value!=Double.MAX_VALUE && value!= -Double.MAX_VALUE) {
				BigDecimal bd = new BigDecimal(value).setScale(decimals,BigDecimal.ROUND_HALF_UP);
				return bd.toPlainString();
			}
			return "NaN";
		}

		private void sendParameter(ParameterAttributes att, float val) {
			System.out.println("Try to set "+att.name+" to "+val+"...");
			att.value = val;
			final msg_param_set msg = new msg_param_set(255,1);
			msg.target_component = 1;
			msg.target_system = 1;
			msg.param_type = att.vtype;
			msg.setParam_id(att.name);
			msg.param_value = ParamUtils.valToParam(att.vtype, val);
			control.sendMAVLinkMessage(msg);
			timeout = wq.addCyclicTask("LP",300,() -> {
				if(++timeout_count > 3) {
					wq.removeTask("LP", timeout);
					MSPLogger.getInstance().writeLocalMsg(att.name+" was not set to "+val+" (timeout)",
							MAV_SEVERITY.MAV_SEVERITY_DEBUG);
					setValueOf(editor,att.value);
				}
				else {
					MSPLogger.getInstance().writeLocalMsg("[mgc] Timeout setting parameter. Retry "
							+timeout_count,MAV_SEVERITY.MAV_SEVERITY_DEBUG);
					control.sendMAVLinkMessage(msg);
				}
			});
		}


		public float getValueOf(Control p) throws NumberFormatException {
			try {
				if(p==null)
					return 0;
				if(p instanceof TextField) {
					((TextField)p).commitValue();
					return Float.parseFloat(((TextField)p).getText());
				}
				else if(p instanceof Spinner)
					return (((Spinner<Double>)editor).getValueFactory().getValue()).floatValue();
				else if(p instanceof ChoiceBox) {
					return ((ChoiceBox<Entry<Integer,String>>)editor).getSelectionModel().getSelectedItem().getKey();
				} else
					return 0;
			} catch(Exception e) {
				System.err.println(e.getMessage());
				return 0;
			}
		}


		public void setValueOf(Control p, double v) {
			Platform.runLater(() -> {
				if(p instanceof TextField) {
					if(att.vtype==MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32)
						((TextField)p).setText(String.valueOf((int)v));
					else {
						if(Double.isFinite(v)) {
							BigDecimal bd = new BigDecimal(v).setScale(att.decimals,BigDecimal.ROUND_HALF_UP);
							((TextField)p).setText(bd.toPlainString());
						} else
							MSPLogger.getInstance().writeLocalMsg(att.name+" was not set. Value is invalid",
									MAV_SEVERITY.MAV_SEVERITY_WARNING);
					}
				}
				else if(p instanceof Spinner)
					((Spinner<Double>)p).getValueFactory().setValue(new Double(v));
				else {
					for(Entry<Integer,String> e : att.valueList.entrySet())
						if(e.getKey()==(int)v)
							((ChoiceBox<Entry<Integer,String>>)editor).getSelectionModel().select(e);
				}

				checkDefaultOf(p,v);
			});
		}


		private void checkDefaultOf(Control p, double v) {
			Control e = p;
			if(p instanceof Spinner)
				e = ((Spinner<Double>)p).getEditor();
			if(att.bitMask!=null && att.bitMask.size()>0) {
				return;
			}
			if(v==att.default_val)
				e.setStyle("-fx-text-fill: #F0F0F0; -fx-control-inner-background: #606060;");
			else
				e.setStyle("-fx-text-fill: #F0D080; -fx-control-inner-background: #606060;");
		}


		private void setContextMenu(Control p) {
			ContextMenu ctxm = new ContextMenu();
			MenuItem cmItem1 = new MenuItem("Set default");
			cmItem1.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) {
					setValueOf(p,att.default_val);
				}
			});
			ctxm.getItems().add(cmItem1);
			if(p instanceof Spinner)
				((Spinner<Double>)p).getEditor().setContextMenu(ctxm);
			else
				p.setContextMenu(ctxm);

		}
	}

	private class SpinnerAttributeFactory extends DoubleSpinnerValueFactory {

		public SpinnerAttributeFactory(ParameterAttributes att) {
			super(att.min_val, att.max_val, att.value, att.increment);
			if(att.increment==0)
				this.setAmountToStepBy(1);

			setConverter(new StringConverter<Double>() {

				@Override public String toString(Double value) {
					BigDecimal bd = new BigDecimal(value).setScale(att.decimals,BigDecimal.ROUND_HALF_UP);
					return bd.toPlainString();
				}

				@Override
				public Double fromString(String string) {
					if(string!=null)
						return Double.valueOf(string);
					return 0.0;
				}
			});
		}
	}

	private static Alert setDefaultButton ( Alert alert, ButtonType defBtn ) {
		DialogPane pane = alert.getDialogPane();
		for ( ButtonType t : alert.getButtonTypes() )
			( (Button) pane.lookupButton(t) ).setDefaultButton( t == defBtn );
		return alert;
	}
}
