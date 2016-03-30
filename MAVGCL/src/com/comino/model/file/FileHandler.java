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
import javafx.stage.Stage;
import me.drton.jmavlib.log.px4.PX4LogReader;


public class FileHandler {

	private static final String BASEPATH = "/.MAVGCL";

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

	public String getName() {
			return name;
	}

	public void clear() {
		name = "";
	}


	public String getBasePath() {
		return System.getProperty("user.home")+BASEPATH;
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
				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
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
