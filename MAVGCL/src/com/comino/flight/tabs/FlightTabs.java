package com.comino.flight.tabs;

import com.comino.flight.MainApp;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.flight.widgets.analysiscontrol.AnalysisControlWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class FlightTabs {


	@FXML
	private FlightXtAnalysisTab xtanalysistab;

	@FXML
	private FlightXYAnalysisTab xyanalysistab;

	public void setup(AnalysisControlWidget recordControl, IMAVController control) {

		xtanalysistab.setup(recordControl,control);
		xyanalysistab.setup(recordControl,control);
	}




}
