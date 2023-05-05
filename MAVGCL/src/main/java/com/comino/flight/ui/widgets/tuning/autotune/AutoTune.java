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
import org.mavlink.messages.MAV_RESULT;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_command_ack;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.file.KeyFigurePreset;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.param.MAVGCLPX4Parameters;
import com.comino.flight.ui.widgets.charts.IChartControl;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavutils.workqueue.WorkQueue;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;


public class AutoTune extends VBox {

	private static final int TUNE_STATE_DISABLED = 0;
	private static final int TUNE_STATE_INIT     = 1;
	private static final int TUNE_STATE_ROLL     = 2;
	private static final int TUNE_STATE_PITCH    = 3;
	private static final int TUNE_STATE_YAW      = 4;
	private static final int TUNE_STATE_DISARM   = 5;


	private StateProperties state = null;
	private IMAVController control;
	private AnalysisDataModelMetaData metadata;
	private MAVGCLPX4Parameters parameters;


	private final WorkQueue wq = WorkQueue.getInstance();

	private int tune_worker;
	private int tune_state = TUNE_STATE_DISABLED;
	private final BooleanProperty isTuning = new SimpleBooleanProperty();

	@FXML
	private Button     autotune;


	public AutoTune() {
		FXMLLoadHelper.load(this, "AutoTune.fxml");
	}


	@FXML
	private void initialize() {
		state = StateProperties.getInstance();
	}

	public void setup(IMAVController control) {

		this.control = control;
		this.metadata = AnalysisDataModelMetaData.getInstance();
		this.parameters = MAVGCLPX4Parameters.getInstance();
		this.disableProperty().bind(state.getConnectedProperty().not());
		autotune.disableProperty().bind(state.getHoldProperty().not().or(state.getLandedProperty().or(isTuning)));

		autotune.setOnAction((ActionEvent event)-> {
			startAutotune();
		});


		// Polling the command acknowledge
		control.addMAVLinkListener(msg_command_ack.class,(o) -> {
			msg_command_ack ack = (msg_command_ack) o;
			if(ack.command == MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE) {

				// Determine progress of tuning and stop autotuning procedure if completed

				switch(ack.result) {

				case MAV_RESULT.MAV_RESULT_IN_PROGRESS:
				case MAV_RESULT.MAV_RESULT_ACCEPTED:

					isTuning.set(true);

					if(ack.result_param2 > 0)
						state.getProgressProperty().set(ack.result_param2/100f);
					else
						state.getProgressProperty().set(0.01f);


					if(ack.result_param2 < 20 ) {
						if(tune_state != TUNE_STATE_INIT) {
							control.writeLogMessage(new LogMessage("[mgc] AutoTune initializing", MAV_SEVERITY.MAV_SEVERITY_INFO));
						}
						tune_state = TUNE_STATE_INIT;
					}

					else if(ack.result_param2 < 40 ) {
						if(tune_state != TUNE_STATE_ROLL)
							control.writeLogMessage(new LogMessage("[mgc] AutoTune tune roll", MAV_SEVERITY.MAV_SEVERITY_INFO));
						tune_state = TUNE_STATE_ROLL;
					}

					else if(ack.result_param2 < 60 ) {
						if(tune_state != TUNE_STATE_PITCH)
							control.writeLogMessage(new LogMessage("[mgc] AutoTune tune pitch", MAV_SEVERITY.MAV_SEVERITY_INFO));
						tune_state = TUNE_STATE_PITCH;
					}

					else if(ack.result_param2 <= 80 ) {
						if(tune_state != TUNE_STATE_YAW)
							control.writeLogMessage(new LogMessage("[mgc] AutoTune tune yaw", MAV_SEVERITY.MAV_SEVERITY_INFO));
						tune_state = TUNE_STATE_YAW;
					}

					else if(ack.result_param2 == 95) {
						if(tune_state != TUNE_STATE_DISARM)
							control.writeLogMessage(new LogMessage("[mgc] Land and disarm to apply tuning", MAV_SEVERITY.MAV_SEVERITY_INFO));
						tune_state = TUNE_STATE_DISARM;
					}

					else {

						stopAutoTune();
						if(ack.result_param2 == 100) {
							control.writeLogMessage(new LogMessage("[mgc] Autotuning successfull", MAV_SEVERITY.MAV_SEVERITY_INFO));
							parameters.refreshParameterList(false);
						} else {
							control.writeLogMessage(new LogMessage("[mgc] Autotuning unknown error ("+ack.result_param2+")", MAV_SEVERITY.MAV_SEVERITY_INFO));
						}
					}
					break;

				case MAV_RESULT.MAV_RESULT_FAILED:
				case MAV_RESULT.MAV_RESULT_CANCELLED:
					control.writeLogMessage(new LogMessage("[mgc] Autotuning aborted/timeout", MAV_SEVERITY.MAV_SEVERITY_INFO));
					stopAutoTune() ;
					break;

				}
			}
		});

	}

	private void startAutotune() {


		IChartControl chart = ChartControlPane.getChart(ChartControlPane.XT_TUNING_CHART);
		KeyFigurePreset preset = new KeyFigurePreset();
		preset.set(0, 0, 0,metadata.getMetaData("ROLLR").hash, 
				metadata.getMetaData("PITCHR").hash,
				metadata.getMetaData("YAWR").hash);
		chart.setKeyFigureSelection(preset);

		tune_worker = wq.addCyclicTask("LP", 1000, () -> {
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_AUTOTUNE_ENABLE,1,0);

		});
	}


	private void stopAutoTune() {
		isTuning.set(false);
		wq.removeTask("LP", tune_worker);
		state.getProgressProperty().set(-1);
		tune_state = TUNE_STATE_DISABLED;
	}


}
