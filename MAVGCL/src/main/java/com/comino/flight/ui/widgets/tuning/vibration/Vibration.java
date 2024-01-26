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

package com.comino.flight.ui.widgets.tuning.vibration;

import java.util.Arrays;

import com.comino.analysis.FFT;
import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.model.service.ICollectorRecordingListener;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.flight.ui.widgets.charts.utils.XYDataPool;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;


public class Vibration extends VBox implements IChartControl  {

	private static final int      POINTS = 512;
	private static final float VIB_SCALE = 0.2f;


	private final static String[] SOURCES = { "Acc.X+Acc.Y ", "Acc.Z", "Raw Gyro Data" , "Act.Roll+Act.Pitch", "Angular Rates"};


	@FXML
	private HBox hbox;


	@FXML
	private ProgressBar vz;


	@FXML
	private LineChart<Number, Number> fft;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> source;


	private AnimationTimer task;


	private FloatProperty   scroll       = new SimpleFloatProperty(0);
	private FloatProperty   replay       = new SimpleFloatProperty(0);

	private  XYChart.Series<Number,Number> series1;
	private  XYChart.Series<Number,Number> series2;
	private  XYChart.Series<Number,Number> series3;

	private float[]  data1 = new float[POINTS];
	private float[]  data2 = new float[POINTS];
	private float[]  data3 = new float[POINTS];


	private FFT fft1 = null;
	private FFT fft2 = null;
	private FFT fft3 = null;

	private XYDataPool pool = null;

	private AnalysisModelService      dataService = AnalysisModelService.getInstance();

	private int max_pt = 0;
	private int sample_rate = 0;
	private int source_id = 2;
	private long tms_old=0;

	private StateProperties state = null;


	public Vibration() {

		FXMLLoadHelper.load(this, "Vibration.fxml");
		
		task = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if((now - tms_old)<20000000)
					return;
				tms_old = now;
				if(dataService.isCollecting() && !isDisabled()) {
					max_pt = dataService.getModelList().size() - 1;
					updateGraph();
				}
			}		
		};
	

		pool = new XYDataPool();

	}


	@FXML
	private void initialize() {

		source.getItems().addAll(SOURCES);
		source.getSelectionModel().select(2);

		vz.setProgress(0);

		series1 = new XYChart.Series<Number,Number>();
		series2 = new XYChart.Series<Number,Number>();
		series3 = new XYChart.Series<Number,Number>();

		sample_rate = 1000 / dataService.getCollectorInterval_ms();

		xAxis.setAutoRanging(false);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(sample_rate/2);
		xAxis.setLabel("Hz");

		yAxis.setAutoRanging(true);
		yAxis.setPrefWidth(30);

		fft1 = new FFT( POINTS, sample_rate );
		fft2 = new FFT( POINTS, sample_rate );
		fft3 = new FFT( POINTS, sample_rate );




		//		yAxis.setAutoRanging(false);
		//		yAxis.setLowerBound(0);
		//		yAxis.setUpperBound(50);

		source.getSelectionModel().selectedIndexProperty().addListener((observable, ov, nv) -> {
			source_id = nv.intValue();
			Platform.runLater(() -> {
				fft.getData().clear();
				fft.getData().add(series1);
				fft.getData().add(series2);
				fft.getData().add(series3);
				updateGraph();
			});
		});

		fft.setLegendVisible(false);
		

	}

	public void setup(IMAVController control) {
 
		state = StateProperties.getInstance();
		
		this.disabledProperty().addListener((v,o,n) -> {
			if(!n.booleanValue()) {
				updateGraph();
			}
		});

		state.getRecordingProperty().addListener((p,o,n) -> {
			if(n.intValue()>0)
				task.start();
			else {
				task.stop();
				vz.setProgress(0);
			}
		});

		state.getLogLoadedProperty().addListener((p,o,n) -> {
			if(n.booleanValue()) {
				getTimeFrameProperty();
			}
		});

		ChartControlPane.addChart(9,this);

		scroll.addListener((v, ov, nv) -> {
			Platform.runLater(() -> {
				max_pt =  dataService.calculateIndexByFactor(nv.floatValue()) - 1;
				updateGraph();
			});
		});

		replay.addListener((v, ov, nv) -> {		
			Platform.runLater(() -> {
				max_pt =  nv.intValue();
				updateGraph();
			});

		});

		state.getRecordingProperty().addListener((p,o,n) -> {
			if(n.intValue()> 0)
				refreshChart();

		});

		state.getReplayingProperty().addListener((p,o,n) -> {
			if(n.booleanValue())
				refresh(0);
		});

	}

	private void updateGraph() {

		AnalysisDataModel m =null; float vib = 0;

		if(isDisabled() || !isVisible() || max_pt < 0)
			return;


		max_pt = max_pt >= dataService.getModelList().size() ? dataService.getModelList().size() -1 : max_pt;

		series1.getData().clear();
		series2.getData().clear();
		series3.getData().clear();

		if(dataService.getModelList().size()==0) {
			vz.setProgress(0);
			return;
		}

		m = dataService.getModelList().get(max_pt);

		vib = (float)m.getValue("VIBMETA");

		if(vib > 4.0)
			vz.setStyle("-fx-accent: #ed3118;");
		else if ( vib > 3.0)
			vz.setStyle("-fx-accent: #e3b34b;");
		else
			vz.setStyle("-fx-accent: #1b8233;");
		vz.setProgress(vib * VIB_SCALE);


		if(max_pt <= POINTS) {
			return;
		}


		for(int i = 0; i < POINTS; i++ ) {
			m = dataService.getModelList().get(max_pt - POINTS + i);
			switch(source_id) {
			case 0:
				data1[i] = (float)m.getValue("ACCX");	
				data2[i] = (float)m.getValue("ACCY");	
				break;
			case 1:
				data3[i] = (float)m.getValue("ACCZ")+ 9.81f;	
				break;
			case 2:
				data1[i] = (float)m.getValue("GYROY");	
				data2[i] = (float)m.getValue("GYROX");	
				data3[i] = (float)m.getValue("GYROZ");	
				break;
			case 3:
				data1[i] = (float)m.getValue("ACTROLL");	
				data2[i] = (float)m.getValue("ACTPITCH");	
				break;
			case 4:
				data1[i] = (float)m.getValue("VEHVX");	
				data2[i] = (float)m.getValue("VEHVY");	
				data3[i] = (float)m.getValue("VEHVZ");	
				break;

			}
			//				data3[i] = (float)Math.sqrt(m.getValue("ACCX") * m.getValue("ACCX") + m.getValue("ACCY") * m.getValue("ACCY") );	
		}


		switch(source_id) {

		case 0:
		case 3:

			fft1.forward(data1); 
			series1.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft1.specSize(); i++ ) {
				series1.getData().add(pool.checkOut(i * fft1.getBandWidth(),fft1.getSpectrum()[i]));
			}

			fft2.forward(data2); 
			series2.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft2.specSize(); i++ ) {
				series2.getData().add(pool.checkOut(i * fft2.getBandWidth(),fft2.getSpectrum()[i]));
			}
			
			series3.getData().clear();

			break;

		case 1:

			fft3.forward(data3);
			series3.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft3.specSize(); i++ ) {
				series3.getData().add(pool.checkOut(i * fft3.getBandWidth(),fft3.getSpectrum()[i]));
			}
			
			series2.getData().clear();
			series1.getData().clear();
			
			break;
			
		case 2:
		case 4:
			
			fft1.forward(data1); 
			series1.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft1.specSize(); i++ ) {
				series1.getData().add(pool.checkOut(i * fft1.getBandWidth(),fft1.getSpectrum()[i]));
			}

			fft2.forward(data2); 
			series2.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft2.specSize(); i++ ) {
				series2.getData().add(pool.checkOut(i * fft2.getBandWidth(),fft2.getSpectrum()[i]));
			}
			
			fft3.forward(data3);
			series3.getData().add(new Data<Number, Number>(0.0,0.0));
			for(int i = 1; i < fft3.specSize(); i++ ) {
				series3.getData().add(pool.checkOut(i * fft3.getBandWidth(),fft3.getSpectrum()[i]));
			}


			break;

		}

	}

	private void refresh(int max) {
		Platform.runLater(() -> {
			max_pt = max;
			fft.getData().clear();
			fft.getData().add(series1);
			fft.getData().add(series2);
			fft.getData().add(series3);
			updateGraph();
		});
	}


	@Override
	public IntegerProperty getTimeFrameProperty() {

		sample_rate = 1000 / dataService.getCollectorInterval_ms();

		xAxis.setAutoRanging(false);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(sample_rate/2);
		xAxis.setLabel("Hz");

		yAxis.setAutoRanging(true);

		fft1 = new FFT( POINTS, sample_rate );
		fft2 = new FFT( POINTS, sample_rate );
		fft3 = new FFT( POINTS, sample_rate );

		refresh(dataService.getModelList().size() - 1);

		return null;
	}


	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}


	@Override
	public FloatProperty getReplayProperty() {
		return replay;
	}


	@Override
	public BooleanProperty getIsScrollingProperty() {
		return null;
	}


	@Override
	public void refreshChart() {
		if(dataService.getModelList().isEmpty()) {
			Arrays.fill(data1, 0f); Arrays.fill(data2, 0f); Arrays.fill(data3, 0f);
			Platform.runLater(() -> {
				getTimeFrameProperty();
				series1.getData().clear();
				series2.getData().clear();
				series3.getData().clear();
				fft.getData().clear();
				fft.getData().add(series1);
				fft.getData().add(series2);
				fft.getData().add(series3);
				updateGraph();
			});
		} else
			refresh(dataService.getModelList().size() - 1);
	}


	@Override
	public KeyFigurePreset getKeyFigureSelection() {
		refresh(dataService.getModelList().size() - 1);
		return null;
	}


	@Override
	public void setKeyFigureSelection(KeyFigurePreset preset) {

	}

}
