package com.comino.flight.tabs;

import com.comino.flight.MainApp;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class FlightTabs extends Pane {


	@FXML
	private FlightXtAnalysisTab xtanalysistab;

	@FXML
	private FlightXYAnalysisTab xyanalysistab;

	public void setup(ChartControlWidget recordControl, IMAVController control) {


		xtanalysistab.setup(recordControl,control);
		xyanalysistab.setup(recordControl,control);
	}




}
