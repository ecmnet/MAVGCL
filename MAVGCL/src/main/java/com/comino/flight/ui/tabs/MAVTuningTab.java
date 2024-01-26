/****************************************************************************
 *
 *   Copyright (c) 2017,2019 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.ui.tabs;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.ui.widgets.charts.line.LineChartWidget;
import com.comino.flight.ui.widgets.tuning.attctl.AttCtlTune;
import com.comino.flight.ui.widgets.tuning.autotune.AutoTune;
import com.comino.flight.ui.widgets.tuning.motor.MotorTest;
import com.comino.flight.ui.widgets.tuning.throttle.ThrottleTune;
import com.comino.flight.ui.widgets.tuning.vibration.Vibration;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MAVTuningTab extends Pane {

	@FXML
	private VBox vbox;
	
	@FXML
	private HBox hbox;


	@FXML
	private AutoTune autotune;

	@FXML
	private AttCtlTune attctl;
	
	@FXML
	private MotorTest motor;
	
	
	@FXML
	private Vibration vibration;
	
	@FXML
	private LineChartWidget chart1;

	private AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();


	public MAVTuningTab() {
		FXMLLoadHelper.load(this, "MAVTuningTab.fxml");
	}

	@FXML
	private void initialize() {
		
        vbox.prefWidthProperty().bind(widthProperty());  
        hbox.prefWidthProperty().bind(widthProperty());    
        chart1.prefWidthProperty().bind(widthProperty());
        chart1.prefHeightProperty().bind(heightProperty().divide(2));
        vibration.prefWidthProperty().bind(this.widthProperty());
        vibration.prefHeightProperty().bind(this.heightProperty().divide(2).subtract(70));
        vibration.disableProperty().bind(this.disabledProperty());
        chart1.disableProperty().bind(this.disabledProperty());
        
	}


	public void setup(IMAVController control) {
		autotune.setup(control);
		attctl.setup(control);
		motor.setup(control);
		vibration.setup(control);
		ChartControlPane.addChart(8,chart1.setup(control,8));
	}

}
