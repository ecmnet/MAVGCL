package com.comino.flight.control;

import java.io.IOException;

import com.comino.flight.widgets.battery.BatteryWidget;
import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.commander.CommanderWidget;
import com.comino.flight.widgets.details.DetailsWidget;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class FlightControlPanel extends Pane  {

	@FXML
	private StatusWidget status;

	@FXML
	private ChartControlWidget xyanalysiscontrol;

	@FXML
	private BatteryWidget battery;

	@FXML
	private DetailsWidget details;

	@FXML
	private CommanderWidget commander;

	public FlightControlPanel() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FlightControlPanel.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	public ChartControlWidget getRecordControl() {
		return xyanalysiscontrol;
	}

	public void setup(IMAVController control) {
		xyanalysiscontrol.setup(control);
		status.setup(control);
		battery.setup(control);

		if(details!=null)
			details.setup(control);

		if(commander!=null)
		  commander.setup(control);
	}

}
