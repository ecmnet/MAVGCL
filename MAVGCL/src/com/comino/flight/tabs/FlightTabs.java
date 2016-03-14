package com.comino.flight.tabs;

import java.util.ArrayList;
import java.util.List;

import com.comino.flight.control.FlightControlPanel;
import com.comino.flight.tabs.analysis3d.MAVAnalysis3DTab;
import com.comino.flight.tabs.inspector.MAVInspectorTab;
import com.comino.flight.tabs.openmap.MAVOpenMapTab;
import com.comino.flight.tabs.xtanalysis.FlightXtAnalysisTab;
import com.comino.flight.tabs.xyanalysis.FlightXYAnalysisTab;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.messages.MessagesWidget;
import com.comino.flight.widgets.statusline.StatusLineWidget;
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
	private MessagesWidget messages;

	@FXML
	private MAVAnalysis3DTab mavanalysis3Dtab;


	private List<Node> tabs = new ArrayList<Node>();


	@FXML
	private void initialize() {

		tabs.add(xtanalysistab);
		tabs.add(xyanalysistab);
		tabs.add(mavanalysis3Dtab);
		tabs.add(mavinspectortab);
		tabs.add(mavmaptab);
		//		tabs.add(mavworldtab);

	}

	public void activateCurrentTab(boolean disable) {
		if(!disable) {
			int tab = tabpane.getSelectionModel().getSelectedIndex();
			tabs.get(tab).setDisable(false);
		}
	}

	public void setup(FlightControlPanel flightControl, StatusLineWidget statusline, IMAVController control) {

		tabpane.prefHeightProperty().bind(heightProperty());


		details.fadeProperty().bind(flightControl.getStatusControl().getDetailsProperty());
		details.setup(control);
		messages.setup(control);

		statusline.registerMessageWidget(messages);

		//		mavworldtab.setup(recordControl,control);
		mavmaptab.setup(flightControl,control);
		mavinspectortab.setup(control);
		xtanalysistab.setup(flightControl.getRecordControl(),control);
		xyanalysistab.setup(flightControl.getRecordControl(),control);
		mavanalysis3Dtab.setup(flightControl.getRecordControl(),control);

		xtanalysistab.setDisable(false);
		xyanalysistab.setDisable(true);
		mavinspectortab.setDisable(true);
		mavanalysis3Dtab.setDisable(true);
		mavmaptab.setDisable(true);
		//		mavworldtab.setDisable(true);

		tabpane.getSelectionModel().selectedIndexProperty().addListener((obs,ov,nv)->{
			for(int i =0; i<tabs.size();i++)
				tabs.get(i).setDisable(i!=nv.intValue());
		});
	}




}
