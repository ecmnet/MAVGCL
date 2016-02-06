package com.comino.flight.tabs;

import com.comino.flight.MainApp;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;

public class FlightTabs {


	@FXML
	private FlightXtAnalysisTab xtanalysistab;

	@FXML
	private FlightXYAnalysisTab xyanalysistab;

	public void start(MainApp mainApp, IMAVController control) {
		xtanalysistab.start(control);
		xyanalysistab.start(control);
	}




}
