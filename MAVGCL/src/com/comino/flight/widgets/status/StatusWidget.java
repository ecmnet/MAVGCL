package com.comino.flight.widgets.status;

import java.io.IOException;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class StatusWidget extends Pane implements IMSPModeChangedListener {

	@FXML
	private Circle armed;

	@FXML
	private Circle connected;

	@FXML
	private Circle rcavailable;

	@FXML
	private Circle althold;

	@FXML
	private Circle poshold;

	private IMAVController control;
	private DataModel model;

	public StatusWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	public void setup(IMAVController control) {
		this.model = control.getCurrentModel();
		this.control = control;
        this.control.addModeChangeListener(this);
        update(model.sys,model.sys);
	}

	@Override
	public void update(Status arg0, Status newStat) {
		if(newStat.isStatus(Status.MSP_CONNECTED))
			connected.setFill(Color.LIGHTGREEN);
		else
			connected.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_ARMED) && newStat.isStatus(Status.MSP_CONNECTED))
			armed.setFill(Color.LIGHTGREEN);
		else
			armed.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_RC_ATTACHED) && newStat.isStatus(Status.MSP_CONNECTED))
			rcavailable.setFill(Color.LIGHTGREEN);
		else
			rcavailable.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_ALTITUDE) && newStat.isStatus(Status.MSP_CONNECTED))
			althold.setFill(Color.LIGHTGREEN);
		else
			althold.setFill(Color.LIGHTGRAY);

		if(newStat.isStatus(Status.MSP_MODE_POSITION) && newStat.isStatus(Status.MSP_CONNECTED))
			poshold.setFill(Color.LIGHTGREEN);
		else
			poshold.setFill(Color.LIGHTGRAY);

	}

}
