/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.observables;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMSPStatusChangedListener;
import com.comino.msp.model.segment.Status;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;

public class StateProperties implements IMSPStatusChangedListener{

	public static final int NO_PROGRESS = -1;

	private static StateProperties instance = null;

	private BooleanProperty connectedProperty = new SimpleBooleanProperty();
	private BooleanProperty simulationProperty = new SimpleBooleanProperty();

	private BooleanProperty rcProperty = new SimpleBooleanProperty();


	private BooleanProperty armedProperty 					= new SimpleBooleanProperty();
	private BooleanProperty landedProperty 					= new SimpleBooleanProperty(true);
	private BooleanProperty altholdProperty 				= new SimpleBooleanProperty();
	private BooleanProperty posholdProperty 				= new SimpleBooleanProperty();

	private BooleanProperty recordingProperty     			= new SimpleBooleanProperty();
	private BooleanProperty isLogLoadedProperty   			= new SimpleBooleanProperty();
	private BooleanProperty isParamLoadedProperty 			= new SimpleBooleanProperty();
	private BooleanProperty isRecordingAvailableProperty	= new SimpleBooleanProperty();

	private BooleanProperty isGPOSAvailable                 = new SimpleBooleanProperty();
	private BooleanProperty isLPOSAvailable                 = new SimpleBooleanProperty();
	private BooleanProperty isBaseAvailable                 = new SimpleBooleanProperty();

	private BooleanProperty isCurrentUpToDate               = new SimpleBooleanProperty(true);

	private FloatProperty progress = new SimpleFloatProperty(-1);

	private IMAVController control;

	private MSPLogger logger;


	public static StateProperties getInstance() {
		return instance;
	}

	public static StateProperties getInstance(IMAVController control) {
		if(instance == null) {
			instance = new StateProperties(control);
			System.out.println("States initialized");
		}
		return instance;
	}

	private StateProperties(IMAVController control) {
		this.control = control;
		this.control.addStatusChangeListener(this);
        this.logger = MSPLogger.getInstance();
		simulationProperty.set(control.isSimulation());
	}

	@Override
	public void update(Status oldStatus, Status newStatus) {

		if(oldStatus==null)
			return;

  //      System.out.println(oldStatus+" => "+newStatus);


		armedProperty.set(newStatus.isStatus(Status.MSP_ARMED));
		connectedProperty.set(newStatus.isStatus(Status.MSP_CONNECTED));
		landedProperty.set(newStatus.isStatus(Status.MSP_LANDED));
		altholdProperty.set(newStatus.isStatus(Status.MSP_MODE_ALTITUDE));
		posholdProperty.set(newStatus.isStatus(Status.MSP_MODE_POSITION));
		rcProperty.set(newStatus.isStatus(Status.MSP_RC_ATTACHED));

		isGPOSAvailable.set(newStatus.isStatus(Status.MSP_GPOS_AVAILABILITY));
		isLPOSAvailable.set(newStatus.isStatus(Status.MSP_LPOS_AVAILABILITY));

		if(newStatus.isStatusChanged(oldStatus,Status.MSP_MODE_ALTITUDE))
			logger.writeLocalMsg("Altitude hold enabled", MAV_SEVERITY.MAV_SEVERITY_INFO);
		if(newStatus.isStatusChanged(oldStatus,Status.MSP_MODE_POSITION))
			logger.writeLocalMsg("Position hold enabled", MAV_SEVERITY.MAV_SEVERITY_INFO);
		if(newStatus.isStatusChanged(oldStatus,Status.MSP_MODE_OFFBOARD))
			logger.writeLocalMsg("Offboard enabled", MAV_SEVERITY.MAV_SEVERITY_INFO);

		if(!newStatus.isStatus(Status.MSP_CONNECTED))
				control.getCurrentModel().clear();
	}


	public BooleanProperty getArmedProperty() {
		return armedProperty;
	}

	public BooleanProperty getConnectedProperty() {
		return connectedProperty;
	}

	public BooleanProperty getLandedProperty() {
		return landedProperty;
	}

	public BooleanProperty getSimulationProperty() {
		return simulationProperty;
	}

	public BooleanProperty getAltHoldProperty() {
		return altholdProperty;
	}

	public BooleanProperty getPosHoldProperty() {
		return posholdProperty;
	}

	public BooleanProperty getRCProperty() {
		return rcProperty;
	}

	public BooleanProperty getRecordingProperty() {
		return recordingProperty;
	}

	public BooleanProperty getLogLoadedProperty() {
		return isLogLoadedProperty;
	}

	public BooleanProperty getGPOSAvailableProperty() {
		return isGPOSAvailable;
	}

	public BooleanProperty getLPOSAvailableProperty() {
		return isLPOSAvailable;
	}

	public BooleanProperty getBaseAvailableProperty() {
		return isBaseAvailable;
	}

	public BooleanProperty getParamLoadedProperty() {
		return isParamLoadedProperty;
	}

	public BooleanProperty getCurrentUpToDate() {
		return isCurrentUpToDate;
	}

	public FloatProperty getProgressProperty() {
		return progress;
	}

	public BooleanProperty getRecordingAvailableProperty() {
		return isRecordingAvailableProperty;
	}

}
