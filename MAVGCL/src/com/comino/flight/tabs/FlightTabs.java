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

package com.comino.flight.tabs;

import java.util.ArrayList;
import java.util.List;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.panel.control.FlightControlPanel;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.tabs.inspector.MAVInspectorTab;
import com.comino.flight.tabs.openmap.MAVOpenMapTab;
import com.comino.flight.tabs.parameter.MAVParameterTab;
import com.comino.flight.tabs.shell.MavLinkShellTab;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.flight.widgets.camera.CameraWidget;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.experimental.ExperimentalWidget;
import com.comino.flight.widgets.messages.MessagesWidget;
import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.flight.widgets.tuning.TuningWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

public class FlightTabs extends Pane {

	@FXML
	private TabPane tabpane;

	@FXML
	private FlightXtAnalysisTab xtanalysistab;

	@FXML
	private FlightXYAnalysisTab xyanalysistab;

	@FXML
	private MAVInspectorTab mavinspectortab;

	@FXML
	private MAVOpenMapTab mavmaptab;

	@FXML
	private DetailsWidget details;

	@FXML
	private TuningWidget tuning;

	@FXML
	private ExperimentalWidget experimental;

	@FXML
	private MessagesWidget messages;

	@FXML
	private CameraWidget camera;

	@FXML
	private MAVParameterTab mavparametertab;

	@FXML
	private MavLinkShellTab mavlinkshelltab;

	private List<Node> tabs = new ArrayList<Node>();


	@FXML
	private void initialize() {

		tabs.add(xtanalysistab);
		tabs.add(xyanalysistab);
		tabs.add(mavmaptab);
		tabs.add(mavinspectortab);
		tabs.add(mavparametertab);
		tabs.add(mavlinkshelltab);

	}

	public void activateCurrentTab(boolean disable) {
		if(!disable) {
			int tab = tabpane.getSelectionModel().getSelectedIndex();
			tabs.get(tab).setDisable(false);
		}
	}

	public void setup(FlightControlPanel flightControl, StatusLineWidget statusline, IMAVController control) {

		tabpane.prefHeightProperty().bind(heightProperty());

		xtanalysistab.setDisable(true);
		xyanalysistab.setDisable(true);
		mavinspectortab.setDisable(true);
		mavmaptab.setDisable(true);
	    mavlinkshelltab.setDisable(true);


		if(camera!=null) {
			camera.setup(control);
			camera.fadeProperty().bind(flightControl.getControl().getVideoVisibility());
		}

		details.fadeProperty().bind(flightControl.getControl().getDetailVisibility());
		details.setup(control);

		tuning.fadeProperty().bind(flightControl.getControl().getTuningVisibility());
		tuning.setup(control);

		messages.disableProperty().bind(flightControl.getControl().getMessageVisibility().not());
		messages.setup(control);


		experimental.fadeProperty().bind(flightControl.getControl().getExperimentalVisibility());
		experimental.setup(control);

		statusline.registerMessageWidget(messages);

		mavmaptab.setup(flightControl.getChartControl(),control);
		mavinspectortab.setup(control);

		xtanalysistab.setup(flightControl.getChartControl(),control);
		xtanalysistab.setWidthBinding(0);

		xyanalysistab.setup(flightControl.getChartControl(),control);
		mavparametertab.setup(control);
		mavlinkshelltab.setup(control);

		this.tabpane.getTabs().get(3).setDisable(true);
		this.tabpane.getTabs().get(4).setDisable(true);
		this.tabpane.getTabs().get(5).setDisable(true);

		StateProperties.getInstance().getConnectedProperty().addListener((observable, oldvalue, newvalue) -> {
			this.tabpane.getTabs().get(3).setDisable(!newvalue.booleanValue());
			this.tabpane.getTabs().get(5).setDisable(!newvalue.booleanValue() || control.isSimulation());
			flightControl.getControl().getDetailVisibility().setValue(newvalue.booleanValue());

		});

		StateProperties.getInstance().getLogLoadedProperty().addListener((observable, oldvalue, newvalue) -> {
			if(control.isConnected()) {
			    this.tabpane.getTabs().get(3).setDisable(newvalue.booleanValue());
			    this.tabpane.getTabs().get(5).setDisable(newvalue.booleanValue());
			    flightControl.getControl().getDetailVisibility().set(false);
			}
		});

		PX4Parameters.getInstance().loadedProperty().addListener((observable, oldvalue, newvalue) -> {
			this.tabpane.getTabs().get(4).setDisable(!newvalue.booleanValue());
		});

		flightControl.getControl().getDetailVisibility().addListener((observable, oldvalue, newvalue) -> {

			if(tuning.isVisible())
				return;

			if(newvalue.booleanValue()) {
				xtanalysistab.setWidthBinding(details.getWidth()+3);
				xyanalysistab.setWidthBinding(details.getWidth()+3);
				mavlinkshelltab.setWidthBinding(details.getWidth()+3);
			}
			else {
				xtanalysistab.setWidthBinding(0);
				xyanalysistab.setWidthBinding(0);
				mavlinkshelltab.setWidthBinding(0);
			}
		});

		flightControl.getControl().getTuningVisibility().addListener((observable, oldvalue, newvalue) -> {
			if(newvalue.booleanValue()) {
				xtanalysistab.setWidthBinding(tuning.getWidth()+3);
				xyanalysistab.setWidthBinding(tuning.getWidth()+3);
				mavlinkshelltab.setWidthBinding(tuning.getWidth()+3);
			}
			else {
				if(details.isVisible() && control.isConnected()) {
					xtanalysistab.setWidthBinding(details.getWidth()+3);
					xyanalysistab.setWidthBinding(details.getWidth()+3);
					mavlinkshelltab.setWidthBinding(details.getWidth()+3);
				}
				else {
					xtanalysistab.setWidthBinding(0);
					xyanalysistab.setWidthBinding(0);
					mavlinkshelltab.setWidthBinding(0);
				}
			}
		});

		tabpane.getSelectionModel().selectedIndexProperty().addListener((obs,ov,nv)->{
			for(int i =0; i<tabs.size();i++)
				tabs.get(i).setDisable(i!=nv.intValue());
		});

		xtanalysistab.setDisable(false);
		control.getCollector().clearModelList();
	}

}
