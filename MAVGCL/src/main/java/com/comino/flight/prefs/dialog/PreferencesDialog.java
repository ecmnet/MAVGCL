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

package com.comino.flight.prefs.dialog;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.mavlink.messages.MSP_CMD;
import org.mavlink.messages.lquac.msg_msp_command;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
	private static final String DEF_BIND_PORT 		= "14550";
	private static final String DEF_VIDEO_URL       =  "http://camera1.mairie-brest.fr/mjpg/video.mjpg?resolution=320x240";


	private Dialog<Boolean> prefDialog;

	@FXML
	private GridPane dialog;

	@FXML
	private TextField ip_address;

	@FXML
	private TextField peer_port;

	@FXML
	private TextField bind_port;

	@FXML
	private CheckBox sitl;

	@FXML
	private TextField video;

	@FXML
	private ComboBox<?> path;

	@FXML
	private ComboBox<?> prespath;

	@FXML
	private CheckBox autosave;

	@FXML
	private CheckBox vidrec;

	@FXML
	private CheckBox ulog;

	@FXML
	private CheckBox download;

	@FXML
	private CheckBox speech;

	@FXML
	private CheckBox debug;

	@FXML
	private CheckBox alert;

	@FXML
	private TextField svinacc;

	@FXML
	private TextField reflat;

	@FXML
	private TextField reflon;
	
	@FXML
	private TextField refalt;

	@FXML
	private TextField icao;

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


		prespath.setEditable(true);
		prespath.setOnShowing(event -> {
			DirectoryChooser dir = new DirectoryChooser();
			if(!prespath.getEditor().getText().isEmpty())
				dir.setInitialDirectory(new File(prespath.getEditor().getText()));
			File file = dir.showDialog(null);
			Platform.runLater(() -> {
				if(file!=null)
					prespath.getEditor().setText(file.getAbsolutePath());
				prespath.hide();
			});
		});

		svinacc.textProperty().addListener(new ChangeListener<String>() {
			@Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue.length()>1 && !newValue.matches("[+-]?([0-9]*[.]?)?[0-9]?")) {
					svinacc.setText(oldValue);
				}
			}
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


		StateProperties.getInstance().preferencesChangedProperty().set(false);

		ip_address.setText(userPrefs.get(MAVPreferences.PREFS_IP_ADDRESS, DEF_IP_ADDRESS));
		peer_port.setText(userPrefs.get(MAVPreferences.PREFS_IP_PORT, DEF_IP_PORT));
		bind_port.setText(userPrefs.get(MAVPreferences.PREFS_BIND_PORT, DEF_BIND_PORT));
		video.setText(userPrefs.get(MAVPreferences.PREFS_VIDEO,DEF_VIDEO_URL));
		path.getEditor().setText(userPrefs.get(MAVPreferences.PREFS_DIR,System.getProperty("user.home")));
		prespath.getEditor().setText(userPrefs.get(MAVPreferences.PRESET_DIR,System.getProperty("user.home")));
		autosave.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.AUTOSAVE, false));
		ulog.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.ULOGGER, false));
		sitl.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.PREFS_SITL, true));
		svinacc.setText(userPrefs.get(MAVPreferences.RTKSVINACC, "3.5"));
		vidrec.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.VIDREC, false));
		reflat.setText(userPrefs.get(MAVPreferences.REFLAT, "47.3977420"));
		reflon.setText(userPrefs.get(MAVPreferences.REFLON, "8.5455940"));
		refalt.setText(userPrefs.get(MAVPreferences.REFALT, "400"));
		speech.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.SPEECH, true));
		debug.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.DEBUG_MSG, true));
		download.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.DOWNLOAD, true));
		alert.selectedProperty().set(userPrefs.getBoolean(MAVPreferences.ALERT, false));
		icao.setText(userPrefs.get(MAVPreferences.ICAO, "EDDM"));

		if(prefDialog.showAndWait().get().booleanValue()) {

			userPrefs.put(MAVPreferences.PREFS_IP_ADDRESS, ip_address.getText());
			userPrefs.put(MAVPreferences.PREFS_IP_PORT, peer_port.getText());
			userPrefs.put(MAVPreferences.PREFS_BIND_PORT, bind_port.getText());
			userPrefs.put(MAVPreferences.PREFS_VIDEO,video.getText());
			userPrefs.put(MAVPreferences.PREFS_DIR,path.getEditor().getText());
			userPrefs.put(MAVPreferences.PRESET_DIR,prespath.getEditor().getText());
			userPrefs.putBoolean(MAVPreferences.AUTOSAVE,autosave.isSelected());
			userPrefs.putBoolean(MAVPreferences.ULOGGER,ulog.isSelected());
			userPrefs.putBoolean(MAVPreferences.PREFS_SITL,sitl.isSelected());
			userPrefs.putBoolean(MAVPreferences.VIDREC,vidrec.isSelected());
			userPrefs.put(MAVPreferences.RTKSVINACC,svinacc.getText());
			userPrefs.put(MAVPreferences.REFLAT, reflat.getText());
			userPrefs.put(MAVPreferences.REFLON, reflon.getText());
			userPrefs.put(MAVPreferences.REFALT, refalt.getText());
			userPrefs.putBoolean(MAVPreferences.SPEECH,speech.isSelected());
			userPrefs.putBoolean(MAVPreferences.DEBUG_MSG,debug.isSelected());
			userPrefs.putBoolean(MAVPreferences.DOWNLOAD,download.isSelected());
			userPrefs.putBoolean(MAVPreferences.ALERT,alert.isSelected());
			userPrefs.put(MAVPreferences.ICAO, icao.getText());

			StateProperties.getInstance().preferencesChangedProperty().set(true);

			try {
				userPrefs.flush();
				userPrefs.sync();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
			
			MSPLogger.getInstance().writeLocalMsg("MAVGCL preferences saved");
		}
	}


}
