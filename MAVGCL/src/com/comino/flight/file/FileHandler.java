/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.log.ProgressInputStream;
import com.comino.flight.log.ProgressInputStream.Listener;
import com.comino.flight.log.px4log.PX4toModelConverter;
import com.comino.flight.log.ulog.UlogtoModelConverter;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.KeyFigureMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.utils.ExecutorService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import me.drton.jmavlib.log.px4.PX4LogReader;
import me.drton.jmavlib.log.ulog.ULogReader;

// TODO: Save Microslam or put microslam into the data collection

public class FileHandler {

	private static final String BASEPATH = "/.MAVGCL";
	private static final String TMPFILE  =  "/logtmp.tmp";

	private static FileHandler handler = null;

	private Stage stage;
	private String name="";
	private Preferences userPrefs;

	private UlogtoModelConverter converter = null;

	private AnalysisModelService modelService = AnalysisModelService.getInstance();
	private IMAVController control;

	private Map<String,String> ulogFields = null;
	private List<String> presetfiles = null;


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
		this.presetfiles = new ArrayList<String>();
		this.userPrefs = MAVPreferences.getInstance();
		this.control = control;

		readPresetFiles();

	}

	private void readPresetFiles() {
		presetfiles.clear();
		File file = new File(userPrefs.get(MAVPreferences.PRESET_DIR,System.getProperty("user.home")));
		if(file.isDirectory()) {
			File[] list = file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.contains(".mgs");
				}
			});
			Arrays.sort(list, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return (int)(o2.lastModified() - o1.lastModified());
				}
			});
			System.out.println(list.length+" presets found");
			for(int i=0;i<list.length && i< 5;i++)
				presetfiles.add(list[i].getName().substring(0, list[i].getName().length()-4));
		}

	}

	public String getName() {
		return name;
	}

	public void clear() {
		name = "";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBasePath() {
		return System.getProperty("user.home")+BASEPATH;
	}

	public List<String> getPresetList() {
		return presetfiles;
	}

	public void fileImport() {
		FileChooser fileChooser = getFileDialog("Open MAVGCL model file...",
				userPrefs.get(MAVPreferences.PREFS_DIR,System.getProperty("user.home")),
				new ExtensionFilter("MAVGCL model files", "*.mgc"),
				new ExtensionFilter("ULog files", "*.ulg"),
				new ExtensionFilter("PX4Log files", "*.px4log"));

		final StateProperties state = StateProperties.getInstance();

		File file = fileChooser.showOpenDialog(stage);

		if(file!=null) {

			new Thread(new Task<Void>() {
				@Override protected Void call() throws Exception {

					state.getLogLoadedProperty().set(false);

					if(file.getName().endsWith("ulg")) {
						ULogReader reader = new ULogReader(file.getAbsolutePath());
						PX4Parameters.getInstance().setParametersFromLog(reader.getParameters());
						converter = new UlogtoModelConverter(reader,modelService.getModelList());
						converter.doConversion();
						ulogFields = reader.getFieldList();
					}

					if(file.getName().endsWith("mgc")) {
						Type listType = new TypeToken<ArrayList<AnalysisDataModel>>() {}.getType();

						ProgressInputStream raw = new ProgressInputStream(new FileInputStream(file));
						raw.addListener(new ProgressInputStream.Listener() {
							@Override
							public void onProgressChanged(int percentage) {
								state.getProgressProperty().set(percentage);
							}
						});
						Reader reader = new BufferedReader(new InputStreamReader(raw));
						Gson gson = new GsonBuilder().create();
						ArrayList<AnalysisDataModel>modelList = gson.fromJson(reader,listType);
						reader.close();
						state.getProgressProperty().set(StateProperties.NO_PROGRESS);
						modelService.setModelList(modelList);
					}

					if(file.getName().endsWith("px4log")) {
						PX4LogReader reader = new PX4LogReader(file.getAbsolutePath());
						PX4Parameters.getInstance().setParametersFromLog(reader.getParameters());
						PX4toModelConverter converter = new PX4toModelConverter(reader,modelService.getModelList());
						converter.doConversion();
					}
					name = file.getName();
					state.getLogLoadedProperty().set(true);
					return null;
				}
			}).start();
		}
	}


	public void fileExport() {

		FileChooser fileChooser = getFileDialog("Save to MAVGCL model file...",
				userPrefs.get(MAVPreferences.PREFS_DIR,System.getProperty("user.home")),
				new ExtensionFilter("MAVGCL model files", "*.mgc"));

		if(name.length()<2)
			name = new SimpleDateFormat("ddMMyy-HHmmss'.mgc'").format(new Date());

		fileChooser.setInitialFileName(name);
		File file = fileChooser.showSaveDialog(stage);
		if(file!=null) {
			new Thread(new Task<Void>() {
				@Override protected Void call() throws Exception {
					Writer writer = new FileWriter(file);
					Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
					stage.getScene().setCursor(Cursor.WAIT);
					gson.toJson(modelService.getModelList(), writer);
					writer.close();
					stage.getScene().setCursor(Cursor.DEFAULT);
					StateProperties.getInstance().getLogLoadedProperty().set(true);
					name = file.getName();
					return null;
				}
			}).start();

		}
	}


	public void autoSave() throws IOException {

		new Thread(new Task<Void>() {
			@Override protected Void call() throws Exception {
				if(control.isSimulation())
					return null;
				stage.getScene().setCursor(Cursor.WAIT);
				name = new SimpleDateFormat("ddMMyy-HHmmss'.mgc'").format(new Date());
				String path = userPrefs.get(MAVPreferences.PREFS_DIR,System.getProperty("user.home"));
				File f = new File(path+"/"+name);
				MSPLogger.getInstance().writeLocalMsg("[mgc] Saving "+f.getName(),MAV_SEVERITY.MAV_SEVERITY_WARNING);
				if(f.exists())
					f.delete();
				f.createNewFile();
				Writer writer = new FileWriter(f);
				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
				gson.toJson(modelService.getModelList(), writer);
				writer.close();
				stage.getScene().setCursor(Cursor.DEFAULT);
				return null;
			}
		}).start();
	}

	public void presetsExport(Map<Integer,KeyFigurePreset> preset) {
		FileChooser fileChooser = getFileDialog("Save key figure preset to...",
				userPrefs.get(MAVPreferences.PRESET_DIR,System.getProperty("user.home")),
				new ExtensionFilter("MAVGCL preset files", "*.mgs"));
		File file = fileChooser.showSaveDialog(stage);
		if(file!=null) {
			new Thread(new Task<Void>() {
				@Override protected Void call() throws Exception {
					Writer writer = new FileWriter(file);
					stage.getScene().setCursor(Cursor.WAIT);
					Gson gson = new GsonBuilder().create();
					gson.toJson(preset, writer);
					writer.close();
					stage.getScene().setCursor(Cursor.DEFAULT);
					return null;
				}
			}).start();
		}
	}

	public Map<Integer,KeyFigurePreset> presetsImport(String name) {
		File file = null;

		if(name!=null) {
			file = new File(userPrefs.get(MAVPreferences.PRESET_DIR,System.getProperty("user.home"))+"/"+name+".mgs");
		} else {
			FileChooser fileChooser = getFileDialog("Open key figure preset to...",
					userPrefs.get(MAVPreferences.PRESET_DIR,System.getProperty("user.home")),
					new ExtensionFilter("MAVGCL preset files", "*.mgs"));
			file = fileChooser.showOpenDialog(stage);
		}
		if(file!=null) {
			try {
				Type listType = new TypeToken<Map<Integer,KeyFigurePreset>>() {}.getType();
				stage.getScene().setCursor(Cursor.WAIT);
				Reader reader = new FileReader(file);
				Gson gson = new GsonBuilder().create();
				stage.getScene().setCursor(Cursor.DEFAULT);
				return gson.fromJson(reader,listType);
			} catch(Exception e) { };
		}
		return null;
	}


	public void openKeyFigureMetaDataDefinition() {
		final AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();

		FileChooser metaFile = new FileChooser();
		metaFile.getExtensionFilters().addAll(new ExtensionFilter("Custom KeyFigure Definition File..", "*.xml"));
		File f = metaFile.showOpenDialog(stage);
		if(f!=null) {
			try {
				meta.loadModelMetaData(new FileInputStream(f));
				clear();
				modelService.clearModelList();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}


	public void dumpUlogFields() {
		if(ulogFields==null)
			return;
		System.out.println("=======DUMP ULOG Fields===================");
		List<String> sortedKeys=new ArrayList<String>(ulogFields.keySet());
		Collections.sort(sortedKeys);
		sortedKeys.forEach((e) -> {
			System.out.print(e);
			//			AnalysisDataModelMetaData.getInstance().getKeyFigures().forEach((k) -> {
			//				if(k.sources.get(KeyFigureMetaData.ULG_SOURCE)!=null) {
			//					if(k.sources.get(KeyFigureMetaData.ULG_SOURCE).field.equals(e)) {
			//						System.out.print("\t\t\t\t=> mapped to "+k.desc1);
			//					}
			//				}
			//			});
			System.out.println();
		});
	}


	public File getTempFile() throws IOException {
		File f = new File(getBasePath()+TMPFILE);
		if(f.exists())
			f.delete();
		f.createNewFile();
		return f;

	}



	private FileChooser getFileDialog(String title, String initDir, ExtensionFilter...filter) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(filter);
		fileChooser.setInitialDirectory(
				new File(initDir));
		return fileChooser;
	}

}
