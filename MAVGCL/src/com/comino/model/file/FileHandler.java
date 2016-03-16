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
		FileChooser fileChooser = getFileDialog("Import collected data from...");
		File file = fileChooser.showOpenDialog(stage);
		try {
			control.getCollector().readFromFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void fileExport() {

		FileChooser fileChooser = getFileDialog("Export collected data to...");
		File file = fileChooser.showSaveDialog(stage);
		try {
			control.getCollector().writeToFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private FileChooser getFileDialog(String title) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(
		         new ExtensionFilter("MAVGCL Model Files", "*.mgc"));
		fileChooser.setInitialDirectory(
	            new File(System.getProperty("user.home")));
		return fileChooser;
	}

}
