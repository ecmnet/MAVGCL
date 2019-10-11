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

package com.comino.flight.ui.panel.control;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.ui.sidebar.DetailsWidget;
import com.comino.flight.ui.widgets.panel.AirWidget;
import com.comino.flight.ui.widgets.panel.BatteryWidget;
import com.comino.flight.ui.widgets.panel.ChartControlWidget;
import com.comino.flight.ui.widgets.panel.CommanderWidget;
import com.comino.flight.ui.widgets.panel.ControlWidget;
import com.comino.flight.ui.widgets.panel.InfoWidget;
import com.comino.flight.ui.widgets.panel.RecordControlWidget;
import com.comino.flight.ui.widgets.panel.StatusWidget;
import com.comino.mavcom.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class FlightControlPanel extends Pane  {

	@FXML
	private VBox     box;

	@FXML
	private ControlWidget control;

	@FXML
	private StatusWidget status;

	@FXML
	private RecordControlWidget recordcontrol;

	@FXML
	private ChartControlWidget  chartcontrol;

	@FXML
	private BatteryWidget battery;

	@FXML
	private AirWidget air;

	@FXML
	private DetailsWidget details;

	@FXML
	private CommanderWidget commander;

	@FXML
	private InfoWidget info;

	public FlightControlPanel() {
		FXMLLoadHelper.load(this, "FlightControlPanel.fxml");
	}

	public RecordControlWidget getRecordControl() {
		return recordcontrol;
	}

	public ChartControlWidget getChartControl() {
		return chartcontrol;
	}

	public ControlWidget getControl() {
		return control;
	}

	public void setup(IMAVController control) {

		box.prefHeightProperty().bind(this.heightProperty().subtract(2));

		status.setup(control);
		recordcontrol.setup(control, chartcontrol, info, status);

		if(battery!=null)
		  battery.setup(control);

		if(details!=null)
			details.setup(control);

		if(commander!=null)
		  commander.setup(control);

		info.setup(control);

	}

}
