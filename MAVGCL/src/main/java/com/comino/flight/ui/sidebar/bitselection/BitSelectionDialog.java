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

package com.comino.flight.ui.sidebar.bitselection;

import java.util.List;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class BitSelectionDialog  {

	private int         bitmask = 0;
	private CheckBox[]  checkboxes = new CheckBox[32];

	private Dialog<Boolean> bitDialog;
	private GridPane    pane;

	public BitSelectionDialog(List<String> bitlist) {
		int i=0;

		this.pane = new GridPane();
		this.pane.setVgap(5);
		for(String bit : bitlist) {
			checkboxes[i] = new CheckBox();
			checkboxes[i].setId("BITBOX"+i);
			checkboxes[i].selectedProperty().addListener((c,o,n) -> {
				bitmask = 0;
				for(int k=0;k<32 && checkboxes[k]!=null ;k++) {
					if(checkboxes[k].isSelected())
						bitmask = (int) (bitmask | (1<<k));
					else
						bitmask = (int) (bitmask & ~(1<<k));
				}
			});
			Label bit_label = new Label(bit);
			pane.addRow(i, checkboxes[i++],bit_label);
		}

		bitDialog = new Dialog<Boolean>();
		bitDialog.initStyle(StageStyle.TRANSPARENT);
		DialogPane dialogPane = bitDialog.getDialogPane();
		dialogPane.getStylesheets().add(getClass().getResource("bitdialog.css").toExternalForm());
		dialogPane.getStyleClass().add("bitDialog");
		dialogPane.setContent(pane);
		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		dialogPane.getButtonTypes().add(buttonTypeCancel);
		ButtonType buttonTypeOk =  new ButtonType("Ok", ButtonData.OK_DONE);
		dialogPane.getButtonTypes().add(buttonTypeOk);

		bitDialog.setResultConverter(new Callback<ButtonType, Boolean>() {
			@Override
			public Boolean call(ButtonType b) {
				if (b == buttonTypeOk)
					return true;
				return false;
			}
		});

	}

	public int getValue() {
		return bitmask;
	}

	public int show() {
		int mask = bitmask;
		if(!bitDialog.showAndWait().get().booleanValue())
			bitmask = mask;
		return bitmask;
	}

	public void setValue(int mask) {
		this.bitmask = mask;
		for(int k=0;k<32 && checkboxes[k]!=null ;k++)
			checkboxes[k].setSelected((mask & (1<<k))!=0);
	}



}
