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

package com.comino.flight.ui.widgets.tuning.autotune;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.lquac.msg_command_ack;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;


public class AutoTune extends VBox {


	private StateProperties state = null;
	private MAVGCLPX4Parameters parameters = null;
	private IMAVController control;
	private AnalysisDataModelMetaData metadata;
	
	private final WorkQueue wq = WorkQueue.getInstance();
	
	private int tune_worker;
	private double completed;

	@FXML
	private ProgressBar progress;

	@FXML
	private Button     roll;

	@FXML
	private Button     pitch;

	@FXML
	private Button     yaw;
	
	@FXML
	private Button     abort;


	public AutoTune() {
		FXMLLoadHelper.load(this, "AutoTune.fxml");
	}


	@FXML
	private void initialize() {
		progress.prefWidthProperty().bind(this.widthProperty());
		state = StateProperties.getInstance();
		parameters = MAVGCLPX4Parameters.getInstance();
		progress.setProgress(0);
		abort.setDisable(true);
	}

	public void setup(IMAVController control) {

		this.control = control;
		this.metadata = AnalysisDataModelMetaData.getInstance();
        this.disableProperty().bind(state.getConnectedProperty().not());
        
		roll.setOnAction((ActionEvent event)-> {
			startAutotuneAxis(0);
		});

		pitch.setOnAction((ActionEvent event)-> {
			startAutotuneAxis(1);
		});

		yaw.setOnAction((ActionEvent event)-> {
			startAutotuneAxis(2);
		});
		
		abort.setOnAction((ActionEvent event)-> {
			stopAutoTune();
		});
	
		// Polling the command acknowledge
		control.addMAVLinkListener(msg_command_ack.class,(o) -> {
			msg_command_ack ack = (msg_command_ack) o;
			if(ack.command == MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE) {
				// Determine progress of tuning and stop autotuning procedure if completed
				Platform.runLater(() -> {
					progress.setProgress(completed);
				});
			}
		});

	}
	
	private void startAutotuneAxis(int axis) {
		
		IChartControl chart = ChartControlPane.getChart(ChartControlPane.XT_TUNING_CHART);
		KeyFigurePreset preset = new KeyFigurePreset();
		switch(axis) {
		case 0:
			preset.set(0, 0, metadata.getMetaData("ROLLR").hash,metadata.getMetaData("SPROLLR").hash );
			break;
		case 1:
			preset.set(0, 0, metadata.getMetaData("PITCHR").hash,metadata.getMetaData("SPPITCHR").hash );
			break;
		case 2:
			preset.set(0, 0, metadata.getMetaData("YAWR").hash,metadata.getMetaData("SPYAWR").hash );
			break;
		}
		chart.setKeyFigureSelection(preset);
		roll.setDisable(true); pitch.setDisable(true); yaw.setDisable(true); abort.setDisable(false);
		
		state.getRecordingProperty().set(AnalysisModelService.COLLECTING);
		
		completed = 0;
		tune_worker = wq.addCyclicTask("LP", 100, () -> {
			
			// Test only:
			Platform.runLater(() -> {
				completed = completed + 0.0025;
				progress.setProgress(completed);
				if(completed >= 1.0) {
					stopAutoTune();
				}	
			});
			
			// Instead:
			// Send MAVLINK command to enable and poll axis tuning
			
		});
				
	}
	
	private void stopAutoTune() {
		Platform.runLater(() -> {
		  progress.setProgress(0);
		  state.getRecordingProperty().set(AnalysisModelService.STOPPED);
		  roll.setDisable(false); pitch.setDisable(false); yaw.setDisable(false); abort.setDisable(true);
		  wq.removeTask("LP", tune_worker);
		});
	}


}
