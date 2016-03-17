package com.comino.model.file;

import java.io.File;
import java.io.IOException;

import com.comino.mav.control.IMAVController;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

public class FileHandler {

	private Stage stage;
	private IMAVController control;



	public FileHandler(Stage stage, IMAVController control) {
		super();
		this.stage = stage;
		this.control = control;
	}

	public void fileImport() {
		FileChooser fileChooser = getFileDialog("Open MAVGCL model file...",
				new ExtensionFilter("MAVGCL Model Files", "*.mgc"));
		File file = fileChooser.showOpenDialog(stage);
		try {
			if(file!=null)
				control.getCollector().readFromFile(file);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

	}


	public void fileImportPX4Log() {
		FileChooser fileChooser = getFileDialog("Import PX4Log...",
				new ExtensionFilter("PX4Log Files", "*.px4log"));
		File file = fileChooser.showOpenDialog(stage);
		try {
			if(file!=null)
				control.getCollector().importPX4Log(file);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

	}

	public void fileExport() {

		FileChooser fileChooser = getFileDialog("Save to MAVGCL model file...",
				new ExtensionFilter("MAVGCL Model Files", "*.mgc"));
		File file = fileChooser.showSaveDialog(stage);
		try {
			if(file!=null)
				control.getCollector().writeToFile(file);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

	}

	private FileChooser getFileDialog(String title, ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(filter);
		fileChooser.setInitialDirectory(
				new File(System.getProperty("user.home")));
		return fileChooser;
	}

}
