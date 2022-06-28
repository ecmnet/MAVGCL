/****************************************************************************
 *
 *   Copyright (c) 2022 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.log.ulog.dialog;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.mavlink.messages.lquac.msg_log_entry;

import com.comino.flight.prefs.MAVPreferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class ULogSelectionDialog  {


	private Pane                    pane = new Pane();
	private Dialog<Boolean>       dialog = new Dialog<Boolean>();
	private TableView<TabData>    table  = new TableView<TabData>() ;
	private ObservableList<TabData> data = FXCollections.observableArrayList();
	
	private DateFormat date_f = null;

	@SuppressWarnings("unchecked")
	public ULogSelectionDialog(Map<Integer,msg_log_entry> list) {
		
		date_f = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		date_f.setTimeZone(TimeZone.getDefault());
		
		pane.setPrefWidth(300);
		dialog.initStyle(StageStyle.TRANSPARENT);
		dialog.setTitle("Select ULOG data file");
		DialogPane dialogPane = dialog.getDialogPane();
		
		if(MAVPreferences.isLightTheme()) 
			dialogPane.getStylesheets().add(getClass().getResource("ULogSelection_light.css").toExternalForm());
		else
			dialogPane.getStylesheets().add(getClass().getResource("ULogSelection_dark.css").toExternalForm());	
		
		dialogPane.getStyleClass().add("UlogDialog");
		dialogPane.setContent(pane);
		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		dialogPane.getButtonTypes().add(buttonTypeCancel);
		ButtonType buttonTypeOk =  new ButtonType("Ok", ButtonData.OK_DONE);
		dialogPane.getButtonTypes().add(buttonTypeOk);
		
		table.prefWidthProperty().bind(pane.widthProperty());
		
		TableColumn<TabData, Integer> colId= new TableColumn<TabData, Integer>("Id");
		colId.setMinWidth(15); 
		colId.setCellValueFactory( new PropertyValueFactory<TabData, Integer>("id"));
		
		TableColumn<TabData, String> colName = new TableColumn<TabData, String>("Timestamp");
		colName.setMinWidth(200); 
		colName.setCellValueFactory( new PropertyValueFactory<TabData, String>("name"));
		
		TableColumn<TabData, Integer> colSize= new TableColumn<TabData, Integer>("Size");
		colSize.setMinWidth(25); 
		colSize.setCellValueFactory( new PropertyValueFactory<TabData, Integer>("size"));
		
		table.getColumns().addAll(colId,colName,colSize);
		
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		table.setItems(data);
		
		pane.getChildren().add(table);
		

		list.values().forEach((m) -> {
			data.add(new TabData(m));
		});
		
		table.setRowFactory( tv -> {
		    TableRow<TabData> row = new TableRow<>();
		    row.setOnMouseClicked(event -> {
		        if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
		          dialog.setResult(Boolean.TRUE);
		          dialog.close();   
		        }
		    });
		    return row ;
		});

		dialog.setResultConverter(new Callback<ButtonType, Boolean>() {
			@Override
			public Boolean call(ButtonType b) {
				if (b == buttonTypeOk)
					return true;
				return false;
			}
		});
	}


	public int selectLogId() {
		if(!dialog.showAndWait().get().booleanValue())
			return -1;
		ObservableList<TabData> selectedItems = table.getSelectionModel().getSelectedItems();
		if(selectedItems.size()>0)
		 return selectedItems.get(0).id;
		return -1;
	}
	
	public class TabData {
		
		private int    id;
		private String name;
		private int    size;
		
		public TabData(msg_log_entry e) {
			id   = e.id;
			name = date_f.format(e.time_utc*1000);
			size = (int)(e.size / 1024);
		}
		
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public int getSize() {
			return size;
		}
		public void setId(int id) {
			this.id = id;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setSize(int size) {
			this.size = size;
		}
			
	}

	
}
