package com.comino.model.file;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.comino.mav.control.IMAVController;
import com.comino.msp.model.DataModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.scene.Cursor;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import me.drton.jmavlib.log.px4.PX4LogReader;
import javafx.stage.Stage;


public class FileHandler {

	private static FileHandler handler = null;

	private Stage stage;
	private IMAVController control;
	private String name;


	public static FileHandler getInstance() {
		return handler;
	}

	public static FileHandler getInstance(Stage stage, IMAVController control) {
		if(handler==null)
			handler = new FileHandler(stage,control);
		return handler;
	}


	private FileHandler(Stage stage, IMAVController control) {
		super();
		this.stage = stage;
		this.control = control;
	}

	public void close() {
		this.name = "";
	}

	public String getName() {
		return name;
	}

	public void fileImport() {
		FileChooser fileChooser = getFileDialog("Open MAVGCL model file...",
				new ExtensionFilter("MAVGCL Model Files", "*.mgc"));
		File file = fileChooser.showOpenDialog(stage);
		try {
			if(file!=null) {
				Type listType = new TypeToken<ArrayList<DataModel>>() {}.getType();
				Reader reader = new FileReader(file);
				Gson gson = new GsonBuilder().create();
				stage.getScene().setCursor(Cursor.WAIT); //Change cursor to wait style
				ArrayList<DataModel>modelList = gson.fromJson(reader,listType);
				reader.close();
				control.getCollector().setModelList(modelList);
				stage.getScene().setCursor(Cursor.DEFAULT);
				name = file.getName();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}


	public void fileImportPX4Log() {
		FileChooser fileChooser = getFileDialog("Import PX4Log...",
				new ExtensionFilter("PX4Log Files", "*.px4log"));
		File file = fileChooser.showOpenDialog(stage);
		try {
			if(file!=null) {
				ArrayList<DataModel>modelList = new ArrayList<DataModel>();
				PX4LogReader reader = new PX4LogReader(file.getAbsolutePath());
				stage.getScene().setCursor(Cursor.WAIT);
				PX4toModelConverter converter = new PX4toModelConverter(reader,modelList);
				converter.doConversion();
				control.getCollector().setModelList(modelList);
				stage.getScene().setCursor(Cursor.DEFAULT);
				name = file.getName();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	public void fileExport() {
        String defaultName = new SimpleDateFormat("ddMMyy-HHmmss'.mgc'").format(new Date());
		FileChooser fileChooser = getFileDialog("Save to MAVGCL model file...",
				new ExtensionFilter("MAVGCL Model Files", "*.mgc"));
		fileChooser.setInitialFileName(defaultName);
		File file = fileChooser.showSaveDialog(stage);
		try {
			if(file!=null) {
				Writer writer = new FileWriter(file);
				Gson gson = new GsonBuilder().create();
				stage.getScene().setCursor(Cursor.WAIT);
				gson.toJson(control.getCollector().getModelList(), writer);
				writer.close();
				stage.getScene().setCursor(Cursor.DEFAULT);
				name = file.getName();
			}
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
