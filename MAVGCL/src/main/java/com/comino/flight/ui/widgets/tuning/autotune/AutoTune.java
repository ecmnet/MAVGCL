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

import org.mavlink.messages.AUTOTUNE_AXIS;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_CMD_ACK;
import org.mavlink.messages.MAV_SEVERITY;
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
import com.comino.mavcom.model.segment.LogMessage;
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

	private int current_axis;
	private int tune_worker;


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
			this.current_axis = AUTOTUNE_AXIS.AUTOTUNE_AXIS_ROLL;
			startAutotuneAxis();
		});

		pitch.setOnAction((ActionEvent event)-> {
			this.current_axis = AUTOTUNE_AXIS.AUTOTUNE_AXIS_PITCH;
			startAutotuneAxis();
		});

		yaw.setOnAction((ActionEvent event)-> {
			this.current_axis = AUTOTUNE_AXIS.AUTOTUNE_AXIS_YAW;
			startAutotuneAxis();
		});

		abort.setOnAction((ActionEvent event)-> {
			stopAutoTune(0);
		});

		// Polling the command acknowledge
		control.addMAVLinkListener(msg_command_ack.class,(o) -> {
			msg_command_ack ack = (msg_command_ack) o;
			if(ack.command == MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE) {
				// Determine progress of tuning and stop autotuning procedure if completed
				switch(ack.result) {
				case MAV_CMD_ACK.MAV_CMD_ACK_OK:

					if(ack.progress >= 100) {
						stopAutoTune(1) ;
					} else {
						Platform.runLater(() -> {
							roll.setDisable(true); pitch.setDisable(true); yaw.setDisable(true); abort.setDisable(false);
							progress.setProgress(ack.progress/100.0);
						});
					}
					break;
				case MAV_CMD_ACK.MAV_CMD_ACK_ERR_NOT_SUPPORTED:
					stopAutoTune(2) ;
					break;
				default:

				}
			}
		});

	}

	private void startAutotuneAxis() {

//		if(!control.isSimulation()) {
//			control.writeLogMessage(new LogMessage("[mgc] In development. Run in simulation only.", MAV_SEVERITY.MAV_SEVERITY_DEBUG));
//			return;
//		}

		IChartControl chart = ChartControlPane.getChart(ChartControlPane.XT_TUNING_CHART);
		KeyFigurePreset preset = new KeyFigurePreset();
		switch(current_axis) {
		case AUTOTUNE_AXIS.AUTOTUNE_AXIS_ROLL:
			preset.set(0, 0, metadata.getMetaData("ROLLR").hash,metadata.getMetaData("SPROLLR").hash );
			break;
		case AUTOTUNE_AXIS.AUTOTUNE_AXIS_PITCH:
			preset.set(0, 0, metadata.getMetaData("PITCHR").hash,metadata.getMetaData("SPPITCHR").hash );
			break;
		case AUTOTUNE_AXIS.AUTOTUNE_AXIS_YAW:
			preset.set(0, 0, metadata.getMetaData("YAWR").hash,metadata.getMetaData("SPYAWR").hash );
			break;
		}
		chart.setKeyFigureSelection(preset);
		System.out.println("[mgc] Start autotuning on selected axis. ");
		control.writeLogMessage(new LogMessage("[mgc] Start autotuning on selected axis.", MAV_SEVERITY.MAV_SEVERITY_INFO));
		tune_worker = wq.addCyclicTask("LP", 100, () -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE, 1,current_axis);
		});

	}


	private void stopAutoTune(int reason) {

		wq.removeTask("LP", tune_worker);

		Platform.runLater(() -> {		
			progress.setProgress(0);
			roll.setDisable(false); pitch.setDisable(false); yaw.setDisable(false); abort.setDisable(true);
		});

		switch(reason) {
		case 0:
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE, 0,current_axis);
			control.writeLogMessage(new LogMessage("[mgc] Autotuning aborted.", MAV_SEVERITY.MAV_SEVERITY_INFO));
			break;
		case 1:
			control.writeLogMessage(new LogMessage("[mgc] Autotuning finalized. Saving parameters.", MAV_SEVERITY.MAV_SEVERITY_INFO));
			break;
		case 2:
			control.writeLogMessage(new LogMessage("[mgc] Autotuning not supported.", MAV_SEVERITY.MAV_SEVERITY_WARNING));
			break;
		}

	}


}
