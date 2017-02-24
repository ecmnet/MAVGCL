package com.comino.flight.widgets.tuning;

import java.util.List;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
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
		bitDialog.initStyle(StageStyle.UNDECORATED);

		DialogPane dialogPane = bitDialog.getDialogPane();
		dialogPane.getStylesheets().add(getClass().getResource("bitdialog.css").toExternalForm());
		dialogPane.setContent(pane);
		dialogPane.getStyleClass().add("bitDialog");
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
