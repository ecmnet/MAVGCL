package com.comino.flight.prefs.dialog;

import java.io.File;

import com.comino.flight.prefs.MAVPreferences;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

public class PreferencesDialog {

	private Dialog<Boolean> prefDialog;

	public PreferencesDialog() {
		prefDialog = new Dialog<Boolean>();
		prefDialog.setTitle("MAVAnalysis preferences");
		prefDialog.getDialogPane().getStylesheets().add(getClass().getResource("preferences.css").toExternalForm());

		Label label1 = new Label("Data storage to: ");
		Label label2 = new Label("Video URL: ");
		TextField path = new TextField(); path.setPrefWidth(300);
		TextField text2 = new TextField();

		Button browse = new Button("Browse...");
		browse.setOnAction(event -> {
			DirectoryChooser fc = new DirectoryChooser();
			fc.setTitle("Store data to...");
			path.setText(fc.showDialog(prefDialog.getOwner()).getAbsolutePath());
		});

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 35, 20, 35));
		grid.add(label1, 1, 1);
		grid.add(path, 2, 1);
		grid.add(browse, 3, 1);
		grid.add(label2, 1, 2);
		grid.add(text2, 2, 2);
		prefDialog.getDialogPane().setContent(grid);

		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		prefDialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
		ButtonType buttonTypeOk =     new ButtonType("Save", ButtonData.OK_DONE);
		prefDialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

		prefDialog.setResultConverter(new Callback<ButtonType, Boolean>() {
		    @Override
		    public Boolean call(ButtonType b) {

		        if (b == buttonTypeOk) {

		            return true;
		        }

		        return false;
		    }
		});
	}

	public void show() {
		if(prefDialog.showAndWait().get().booleanValue()) {

		}
	}

}
