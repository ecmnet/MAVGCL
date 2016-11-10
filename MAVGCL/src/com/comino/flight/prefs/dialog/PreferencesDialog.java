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

package com.comino.flight.prefs.dialog;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.comino.flight.prefs.MAVPreferences;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

public class PreferencesDialog  {

	private static final String DEF_IP_ADDRESS 		= "172.168.178.1";
	private static final String DEF_IP_PORT 		= "14555";
	private static final String DEF_VIDEO_URL       =  "http://camera1.mairie-brest.fr/mjpg/video.mjpg?resolution=320x240";


	private Dialog<Boolean> prefDialog;

	@FXML
	private GridPane dialog;

	@FXML
	private TextField ip_address;

	@FXML
	private TextField ip_port;

	@FXML
	private TextField video;

	@FXML
	private ComboBox<?> path;

	@FXML
	private CheckBox autosave;

	@FXML
	private CheckBox ulog;

	private IMAVController control;
	private Preferences userPrefs;

	public PreferencesDialog(IMAVController control) {
		this.control = control;
		prefDialog = new Dialog<Boolean>();
		prefDialog.setTitle("MAVAnalysis preferences");

		userPrefs = MAVPreferences.getInstance();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Preferences.fxml"));
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}
		prefDialog.getDialogPane().getStylesheets().add(getClass().getResource("preferences.css").toExternalForm());
		prefDialog.setHeight(500);

		prefDialog.getDialogPane().setContent(dialog);

		path.setEditable(true);
		path.setOnShowing(event -> {
			DirectoryChooser dir = new DirectoryChooser();
			if(!path.getEditor().getText().isEmpty())
				dir.setInitialDirectory(new File(path.getEditor().getText()));
			File file = dir.showDialog(null);
			Platform.runLater(() -> {
				if(file!=null)
					path.getEditor().setText(file.getAbsolutePath());
				path.hide();
			});
		});

		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		prefDialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
		ButtonType buttonTypeOk =     new ButtonType("Save", ButtonData.OK_DONE);
		prefDialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

		prefDialog.setResultConverter(new Callback<ButtonType, Boolean>() {
			@Override
			public Boolean call(ButtonType b) {
				if (b == buttonTypeOk)
					return true;
				return false;
			}
		});
	}

	public void show() {

		ip_address.setText(userPrefs.get(MAVPreferences.PREFS_IP_ADDRESS, DEF_IP_ADDRESS));
		ip_port.setText(userPrefs.get(MAVPreferences.PREFS_IP_PORT, DEF_IP_PORT));
		video.setText(userPrefs.get(MAVPreferences.PREFS_VIDEO,DEF_VIDEO_URL));
		path.getEditor().setText(userPrefs.get(MAVPreferences.PREFS_DIR,System.getProperty("user.home")));
		autosave.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.AUTOSAVE, false));
		ulog.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.ULOGGER, false));

		if(prefDialog.showAndWait().get().booleanValue()) {

			userPrefs.put(MAVPreferences.PREFS_IP_ADDRESS, ip_address.getText());
			userPrefs.put(MAVPreferences.PREFS_IP_PORT, ip_port.getText());
			userPrefs.put(MAVPreferences.PREFS_VIDEO,video.getText());
			userPrefs.put(MAVPreferences.PREFS_DIR,path.getEditor().getText());
			userPrefs.putBoolean(MAVPreferences.AUTOSAVE,autosave.isSelected());
			userPrefs.putBoolean(MAVPreferences.ULOGGER,ulog.isSelected());

			try {
				userPrefs.flush();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
			MSPLogger.getInstance().writeLocalMsg("MAVGCL preferences saved");
		}
	}


}
