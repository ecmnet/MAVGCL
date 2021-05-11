/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.log.MSPLogger;
import com.comino.mavcom.model.segment.LogMessage;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.status.StatusManager;
import com.comino.mavutils.workqueue.WorkQueue;
import com.comino.speech.VoiceTTS;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class StateProperties {

	public static final int NO_PROGRESS = -1;

	private static StateProperties instance = null;

	private BooleanProperty connectedProperty = new SimpleBooleanProperty();
	private BooleanProperty simulationProperty = new SimpleBooleanProperty();

	private BooleanProperty rcProperty = new SimpleBooleanProperty();


	private BooleanProperty armedProperty 					= new SimpleBooleanProperty();
	private BooleanProperty landedProperty 					= new SimpleBooleanProperty();
	private BooleanProperty altholdProperty 				= new SimpleBooleanProperty();
	private BooleanProperty posholdProperty 				= new SimpleBooleanProperty();
	private BooleanProperty offboardProperty 			    = new SimpleBooleanProperty();
	private BooleanProperty holdProperty 			        = new SimpleBooleanProperty();

	private IntegerProperty recordingProperty     			= new SimpleIntegerProperty();
	private IntegerProperty streamProperty     		    	= new SimpleIntegerProperty();
	private BooleanProperty isAutoRecordingProperty    		= new SimpleBooleanProperty();
	private BooleanProperty isLogLoadedProperty   			= new SimpleBooleanProperty();
	private BooleanProperty isParamLoadedProperty 			= new SimpleBooleanProperty();
	private BooleanProperty isRecordingAvailableProperty	  = new SimpleBooleanProperty();
	private BooleanProperty isReplayingProperty	              = new SimpleBooleanProperty();

	private BooleanProperty isGPOSAvailable                  = new SimpleBooleanProperty();
	private BooleanProperty isLPOSAvailable                  = new SimpleBooleanProperty();
	private BooleanProperty isBaseAvailable                  = new SimpleBooleanProperty();
	private BooleanProperty isMSPAvailable                   = new SimpleBooleanProperty();
	private BooleanProperty isIMUAvailable                   = new SimpleBooleanProperty();
	private BooleanProperty isGPSAvailable                   = new SimpleBooleanProperty();
	private BooleanProperty isCVAvailable                    = new SimpleBooleanProperty();
	private BooleanProperty isSLAMAvailable                  = new SimpleBooleanProperty();
	private BooleanProperty isFiducialLocked                 = new SimpleBooleanProperty();

	private BooleanProperty isCurrentUpToDate                = new SimpleBooleanProperty(true);
	private BooleanProperty isInitializedProperty            = new SimpleBooleanProperty();
	private BooleanProperty isControllerConnectedProperty    = new SimpleBooleanProperty();
	private BooleanProperty preferencesChangedProperty       = new SimpleBooleanProperty();

	private FloatProperty  progress 						 = new SimpleFloatProperty(-1);

	private IMAVController control;

	private MSPLogger logger;
	
	private final WorkQueue wq = WorkQueue.getInstance();
	


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
		this.logger = MSPLogger.getInstance();

		simulationProperty.set(control.isSimulation());
		
		wq.addSingleTask("LP", 5000, () ->  isInitializedProperty.set(true) );

		control.getStatusManager().addListener(Status.MSP_ACTIVE, (n) -> {
			Platform.runLater(()-> {
			isMSPAvailable.set(n.isStatus(Status.MSP_ACTIVE));
			});
		});

		control.getStatusManager().addListener(Status.MSP_ARMED, (n) -> {
			Platform.runLater(()-> {
			  armedProperty.set(n.isStatus(Status.MSP_ARMED));
			});

		});

		control.getStatusManager().addListener(Status.MSP_CONNECTED, (n) -> {
			
			wq.addSingleTask("LP", 2000,() -> {
				
				connectedProperty.set(n.isStatus(Status.MSP_CONNECTED));
				
				isGPSAvailable.set(true);
				isGPSAvailable.set(n.isSensorAvailable(Status.MSP_GPS_AVAILABILITY));
				isCVAvailable.set(true);
				isCVAvailable.set(n.isSensorAvailable(Status.MSP_OPCV_AVAILABILITY));
				isSLAMAvailable.set(true);
				isSLAMAvailable.set(n.isSensorAvailable(Status.MSP_SLAM_AVAILABILITY));
				
			});
			
			if(!n.isStatus(Status.MSP_CONNECTED)) {
				control.writeLogMessage(new LogMessage("[mgc] Connection to vehicle lost..",MAV_SEVERITY.MAV_SEVERITY_CRITICAL));

			} else {
				  control.getStatusManager().reset();
			}
		});

		control.getStatusManager().addListener(Status.MSP_LANDED, (n) -> {
			Platform.runLater(()-> {
		    	landedProperty.set(n.isStatus(Status.MSP_LANDED));
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE, Status.NAVIGATION_STATE_ALTCTL,  (n) -> {
			Platform.runLater(()-> {
			altholdProperty.set(n.nav_state == Status.NAVIGATION_STATE_ALTCTL);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE, Status.NAVIGATION_STATE_POSCTL, (n) -> {
			Platform.runLater(()-> {
			posholdProperty.set(n.nav_state == Status.NAVIGATION_STATE_POSCTL);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE, Status.NAVIGATION_STATE_OFFBOARD, (n) -> {
			Platform.runLater(()-> {
			offboardProperty.set(n.nav_state == Status.NAVIGATION_STATE_OFFBOARD);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_PX4_NAVSTATE, Status.NAVIGATION_STATE_AUTO_LOITER, (n) -> {
			Platform.runLater(()-> {
			holdProperty.set(n.nav_state == Status.NAVIGATION_STATE_AUTO_LOITER);
			});
		});

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_SERVICES,Status.MSP_GPS_AVAILABILITY, (n) -> {
			Platform.runLater(()-> {
			isGPSAvailable.set(n.isSensorAvailable(Status.MSP_GPS_AVAILABILITY));
			});
		});
		
		control.getStatusManager().addListener(StatusManager.TYPE_MSP_SERVICES,Status.MSP_OPCV_AVAILABILITY, (n) -> {
			Platform.runLater(()-> {
			isCVAvailable.set(n.isSensorAvailable(Status.MSP_OPCV_AVAILABILITY));
			});
		});
		
		control.getStatusManager().addListener(StatusManager.TYPE_MSP_SERVICES,Status.MSP_SLAM_AVAILABILITY, (n) -> {
			Platform.runLater(()-> {
			isSLAMAvailable.set(n.isSensorAvailable(Status.MSP_SLAM_AVAILABILITY));
			});
		});
		
		control.getStatusManager().addListener(StatusManager.TYPE_MSP_SERVICES,Status.MSP_FIDUCIAL_LOCKED, (n) -> {
			Platform.runLater(()-> {
			isFiducialLocked.set(n.isSensorAvailable(Status.MSP_FIDUCIAL_LOCKED));
			});
		});
		
		control.getStatusManager().addListener(Status.MSP_RC_ATTACHED, (n) -> {
			Platform.runLater(()-> {
			rcProperty.set(n.isStatus(Status.MSP_RC_ATTACHED));
			});
		});

		control.getStatusManager().addListener(Status.MSP_IMU_AVAILABILITY, (n) -> {
			Platform.runLater(()-> {
			isIMUAvailable.set(n.isStatus(Status.MSP_IMU_AVAILABILITY));
			});
		});

		control.getStatusManager().addListener(Status.MSP_GPOS_VALID, (n) -> {
			Platform.runLater(()-> {
			isGPOSAvailable.set(n.isStatus(Status.MSP_GPOS_VALID));
			});
		});

		control.getStatusManager().addListener(Status.MSP_LPOS_VALID, (n) -> {
			Platform.runLater(()-> {
			isLPOSAvailable.set(n.isStatus(Status.MSP_LPOS_VALID));
			});
		});

		if(MAVPreferences.getInstance().getBoolean("SPEECH", false)) {
			control.addMAVMessageListener(msg -> {
				if(msg.severity != MAV_SEVERITY.MAV_SEVERITY_EMERGENCY && msg.severity != MAV_SEVERITY.MAV_SEVERITY_CRITICAL)
					return;
				if(msg.text.contains("]"))
					VoiceTTS.getInstance().talk(msg.text.substring(msg.text.indexOf(']')));
//				else
//					VoiceTTS.getInstance().talk(msg.text);
			});
		}
	}

	public BooleanProperty getMSPProperty() {
		return isMSPAvailable;
	}

	public BooleanProperty getIMUProperty() {
		return isIMUAvailable;
	}

	public BooleanProperty getControllerConnectedProperty() {
		return isControllerConnectedProperty;
	}

	public BooleanProperty getArmedProperty() {
		return armedProperty;
	}
	
	public IntegerProperty getStreamProperty() {
		return streamProperty;
	}

	public BooleanProperty getConnectedProperty() {
		return connectedProperty;
	}

	public BooleanProperty getLandedProperty() {
		return landedProperty;
	}

	public BooleanProperty getHoldProperty() {
		return holdProperty;
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

	public BooleanProperty getOffboardProperty() {
		return offboardProperty;
	}

	public BooleanProperty getRCProperty() {
		return rcProperty;
	}

	public IntegerProperty getRecordingProperty() {
		return recordingProperty;
	}

	public BooleanProperty getLogLoadedProperty() {
		return isLogLoadedProperty;
	}

	public BooleanProperty getGPOSAvailableProperty() {
		return isGPOSAvailable;
	}
	
	public BooleanProperty getFiducialLockedProperty() {
		return isFiducialLocked;
	}

	public BooleanProperty getLPOSAvailableProperty() {
		return isLPOSAvailable;
	}

	public BooleanProperty getBaseAvailableProperty() {
		return isBaseAvailable;
	}
	
	public BooleanProperty getGPSAvailableProperty() {
		return isGPSAvailable;
	}
	
	public BooleanProperty getCVAvailableProperty() {
		return isCVAvailable;
	}
	
	public BooleanProperty getSLAMAvailableProperty() {
		return isSLAMAvailable;
	}

	public BooleanProperty getParamLoadedProperty() {
		return isParamLoadedProperty;
	}

	public BooleanProperty getReplayingProperty() {
		return isReplayingProperty;
	}

	public BooleanProperty isAutoRecording() {
		return isAutoRecordingProperty;
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

	public BooleanProperty getInitializedProperty() {
		return isInitializedProperty;
	}

	public BooleanProperty preferencesChangedProperty() {
		return preferencesChangedProperty;
	}
}
