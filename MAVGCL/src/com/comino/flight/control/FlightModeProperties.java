package com.comino.flight.control;

import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.segment.Status;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class FlightModeProperties implements IMSPModeChangedListener{

	private static FlightModeProperties instance = null;


	private BooleanProperty connectedProperty = new SimpleBooleanProperty();
	private BooleanProperty armedProperty = new SimpleBooleanProperty();
	private BooleanProperty landedProperty = new SimpleBooleanProperty();
	private BooleanProperty altholdProperty = new SimpleBooleanProperty();
	private BooleanProperty posholdProperty = new SimpleBooleanProperty();

	public static FlightModeProperties getInstance() {
		return instance;
	}

	public static FlightModeProperties getInstance(IMAVController control) {
		if(instance == null)
			instance = new FlightModeProperties(control);
		return instance;
	}

	private FlightModeProperties(IMAVController control) {
		 control.addModeChangeListener(this);
	}

	@Override
	public void update(Status oldStatus, Status newStatus) {
		armedProperty.set(newStatus.isStatus(Status.MSP_ARMED));
		connectedProperty.set(newStatus.isStatus(Status.MSP_CONNECTED));
		landedProperty.set(newStatus.isStatus(Status.MSP_LANDED));
		altholdProperty.set(newStatus.isStatus(Status.MSP_MODE_ALTITUDE));
		posholdProperty.set(newStatus.isStatus(Status.MSP_MODE_POSITION));
	}


	public BooleanProperty getArmedProperty() {
		return armedProperty;
	}

	public BooleanProperty getConnectedProperty() {
		return connectedProperty;
	}

	public BooleanProperty getLandedProperty() {
		return connectedProperty;
	}

	public BooleanProperty getAltHoldProperty() {
		return altholdProperty;
	}

	public BooleanProperty getPosHoldProperty() {
		return posholdProperty;
	}






}
