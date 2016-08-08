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

package com.comino.flight.widgets.charts.control;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.log.FileHandler;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.fx.controls.WidgetPane;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.mav.control.IMAVController;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ChartControlWidget extends WidgetPane  {

	private static final Integer[] TOTAL_TIME = { 10, 30, 60, 240, 1200 };


	@FXML
	private ChoiceBox<Integer> totaltime;

	@FXML
	private ComboBox<String> keyfigures;

	@FXML
	private Slider scroll;

	private IMAVController control;
	private List<IChartControl> charts = null;

	protected int totalTime_sec = 30;
	private AnalysisModelService modelService;

	private long scroll_tms = 0;


	public ChartControlWidget() {
		super(300,true);
		FXMLLoadHelper.load(this, "ChartControlWidget.fxml");
		charts = new ArrayList<IChartControl>();
	}

	@FXML
	private void initialize() {

		this.modelService =  AnalysisModelService.getInstance();
		totaltime.getItems().addAll(TOTAL_TIME);
		totaltime.getSelectionModel().select(1);

		buildKeyfigureModelSelection();

		totaltime.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			totalTime_sec  = newValue.intValue();
			modelService.setTotalTimeSec(totalTime_sec);

			for(IChartControl chart : charts) {
				if(chart.getTimeFrameProperty()!=null)
					chart.getTimeFrameProperty().set(newValue.intValue());
			}

			if(modelService.getModelList().size() < totalTime_sec * 1000 /  modelService.getCollectorInterval_ms() || modelService.isCollecting())
				scroll.setDisable(true);
			else
				scroll.setDisable(false);
			scroll.setValue(1);
		});


		scroll.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			if((System.currentTimeMillis() - scroll_tms)>20) {
				scroll_tms = System.currentTimeMillis();
				for(IChartControl chart : charts) {
					if(chart.getScrollProperty()!=null)
						chart.getScrollProperty().set(1f-newvalue.floatValue()/1000);
				}
			}
		});


		state.getLogLoadedProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue.booleanValue()) {
			if(modelService.getModelList().size() < totalTime_sec * 1000 /  modelService.getCollectorInterval_ms() || modelService.isCollecting())
				scroll.setDisable(true);
			else
				scroll.setDisable(false);
			scroll.setValue(1);
			}
		});

		state.getRecordingProperty().addListener((observable, oldvalue, newvalue) -> {
			if(newvalue.booleanValue()) {
				scroll.setDisable(true);
				scroll.setValue(1);
				return;
			}
			if(modelService.getModelList().size() < totalTime_sec * 1000 /  modelService.getCollectorInterval_ms())
				scroll.setDisable(true);
			else
				scroll.setDisable(false);
		});

		//	scroll.disableProperty().bind(StateProperties.getInstance().getRecordingProperty());
		scroll.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					if(scroll.getValue()==1000)
						scroll.setValue(0);
					else
						scroll.setValue(1000);
					for(IChartControl chart : charts) {
						if(chart.getScrollProperty()!=null)
							chart.getScrollProperty().set((float)(1f-scroll.getValue()/1000));
					}
				}
			}
		});
	}


	public void setup(IMAVController control, StatusWidget statuswidget) {
		this.control = control;
		this.modelService =  AnalysisModelService.getInstance(control.getCurrentModel());
		this.modelService.setTotalTimeSec(totalTime_sec);
		this.modelService.clearModelList();

		for(IChartControl chart : charts) {
			if(chart.getTimeFrameProperty()!=null)
				chart.getTimeFrameProperty().set(30);
			if(chart.getScrollProperty()!=null)
				chart.getScrollProperty().set(1);
		}
	}


	public void addChart(IChartControl chart) {
		charts.add(chart);
	}

	public void refreshCharts() {

		for(IChartControl chart : charts) {
			if(chart.getScrollProperty()!=null)
				chart.getScrollProperty().set(1);
			chart.refreshChart();
		}
		scroll.setValue(0);
		if(modelService.getModelList().size() > totalTime_sec * 1000 /  modelService.getCollectorInterval_ms())
			scroll.setDisable(false);
	}

	private void buildKeyfigureModelSelection() {

		final AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();

		keyfigures.getItems().add("Built-In model definition");
		keyfigures.getItems().add("Select custom model (xml)...");
		keyfigures.getEditor().setText(meta.getDescription());
		keyfigures.setEditable(true);
		keyfigures.getEditor().setEditable(false);


		keyfigures.getSelectionModel().selectedIndexProperty().addListener((o,ov,nv) -> {
			switch(nv.intValue()) {
			case 0: meta.loadModelMetaData(null);
			break;
			case 1:
				try {
					FileChooser metaFile = new FileChooser();
					metaFile.getExtensionFilters().addAll(new ExtensionFilter("Custom KeyFigure Definition File..", "*.xml"));

					File f = metaFile.showOpenDialog(ChartControlWidget.this.getScene().getWindow());
					if(f!=null)
						meta.loadModelMetaData(new FileInputStream(f));

					Platform.runLater(() -> {
						keyfigures.getSelectionModel().clearSelection();
						keyfigures.getEditor().setText(meta.getDescription());
					});
				} catch(Exception e) {
					Platform.runLater(() -> {
						keyfigures.getSelectionModel().select(0);
					});
				}
				break;
			}
			clearData();
		});
	}

	private void clearData() {
		FileHandler.getInstance().clear();
		modelService.clearModelList();

	}

}
