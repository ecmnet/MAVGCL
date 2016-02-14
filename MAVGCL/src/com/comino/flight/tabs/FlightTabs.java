package com.comino.flight.tabs;

import com.comino.flight.tabs.inspector.MAVInspectorTab;
import com.comino.flight.tabs.openmap.MAVOpenMapTab;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class FlightTabs extends Pane {


	@FXML
	private FlightXtAnalysisTab xtanalysistab;

	@FXML
	private FlightXYAnalysisTab xyanalysistab;

	@FXML
	private MAVInspectorTab mavinspectortab;

	@FXML
	private MAVOpenMapTab mavmaptab;

	public void setup(ChartControlWidget recordControl, IMAVController control) {

		mavmaptab.setup(control);
        mavinspectortab.setup(control);
		xtanalysistab.setup(recordControl,control);
		xyanalysistab.setup(recordControl,control);
	}




}
