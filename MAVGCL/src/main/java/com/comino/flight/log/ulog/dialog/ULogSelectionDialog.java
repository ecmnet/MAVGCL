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

import com.comino.flight.log.ulog.MavLinkULOGHandler;
import com.comino.flight.log.ulog.entry.ULogEntry;
import com.comino.flight.prefs.MAVPreferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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
	private TableView<ULogEntry>    table  = new TableView<ULogEntry>() ;
	private ObservableList<ULogEntry> data = FXCollections.observableArrayList();
	
	private DateFormat date_f = null;

	@SuppressWarnings("unchecked")
	public ULogSelectionDialog(Map<Integer,ULogEntry> list) {
		
		date_f = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		date_f.setTimeZone(TimeZone.getDefault());
		
		pane.setPrefWidth(300);
		dialog.initStyle(StageStyle.TRANSPARENT);
		
		DialogPane dialogPane = dialog.getDialogPane();
		
		if(MAVPreferences.isLightTheme()) 
			dialogPane.getStylesheets().add(ULogSelectionDialog.class.getResource("ulogsel_light.css").toExternalForm());
		else
			dialogPane.getStylesheets().add(ULogSelectionDialog.class.getResource("ulogsel_dark.css").toExternalForm());	
		
		dialogPane.getStyleClass().add("UlogDialog");
		dialogPane.setContent(pane);
		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		dialogPane.getButtonTypes().add(buttonTypeCancel);
		ButtonType buttonTypeOk =  new ButtonType("Ok", ButtonData.OK_DONE);
		dialogPane.getButtonTypes().add(buttonTypeOk);
		
		table.prefWidthProperty().bind(pane.widthProperty());
		
		
		TableColumn<ULogEntry, Integer> colId= new TableColumn<ULogEntry, Integer>("Id");
		colId.setMinWidth(15); 
		colId.setCellValueFactory( new PropertyValueFactory<ULogEntry, Integer>("id"));
		
		TableColumn<ULogEntry, String> colName = new TableColumn<ULogEntry, String>("Timestamp");
		colName.setMinWidth(215); 
		colName.setCellValueFactory( new PropertyValueFactory<ULogEntry, String>("name"));
		colName.setSortType(TableColumn.SortType.DESCENDING);
		
		TableColumn<ULogEntry, String> colSize= new TableColumn<ULogEntry, String>("Size");
		colSize.setMinWidth(50); 
		colSize.setCellValueFactory( new PropertyValueFactory<ULogEntry, String>("size"));
		
		table.getColumns().addAll(colId,colName,colSize);
		
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		
		SortedList<ULogEntry> sortedData = new SortedList<>(data);
		sortedData.comparatorProperty().bind(table.comparatorProperty());
		
		table.setItems(sortedData);
		
		table.getSortOrder().addAll(colName);
		pane.getChildren().add(table);
		
		data.addAll(list.values());
		
		table.setRowFactory( tv -> {
		    TableRow<ULogEntry> row = new TableRow<>();
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
		ObservableList<ULogEntry> selectedItems = table.getSelectionModel().getSelectedItems();
		if(selectedItems.size()>0)
		 return selectedItems.get(0).id;
		return -1;
	}
	
//	public class TabData {
//		
//		private int    id;
//		private String name;
//		private String size;
//		
//		public TabData(MavLinkULOGHandler.LogEntry e) {
//			
//			id   = e.id;
//			name = date_f.format(e.time_utc*1000);
//			
//			float s = e.size / 1024f;
//			if(s < 1024)
//			    size = String.format("%#.0fkb", s);
//			else
//				size = String.format("%#.1fMb", s/1024f);
//
//		}
//		
//		public int getId() {
//			return id;
//		}
//		public String getName() {
//			return name;
//		}
//		public String getSize() {
//			return size;
//		}
//		public void setId(int id) {
//			this.id = id;
//		}
//		public void setName(String name) {
//			this.name = name;
//		}
//		public void setSize(String size) {
//			this.size = size;
//		}
//			
//	}

	
}
