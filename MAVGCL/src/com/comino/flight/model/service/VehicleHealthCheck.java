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

package com.comino.flight.model.service;

import java.text.DecimalFormat;

import org.mavlink.messages.MAV_SEVERITY;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class VehicleHealthCheck {

	private static final long HEALTH_CHECK_DURATION = 5000;
	private static final long WAIT_DURATION = 0;

	private StateProperties state = StateProperties.getInstance();

	private long  check_start_ms = 0;

	private boolean do_check = false;
	private boolean healthOk  = true;

	private float max_roll = -Float.MAX_VALUE, min_roll = Float.MAX_VALUE;
	private float max_pitch= -Float.MAX_VALUE, min_pitch= Float.MAX_VALUE;

	private float max_head= -Float.MAX_VALUE, min_head = Float.MAX_VALUE;

	private BooleanProperty healthProperty = new SimpleBooleanProperty();

	private PX4Parameters parameters = null;
	private String reason = null;

	private DecimalFormat f = new DecimalFormat("#0.000");

	public VehicleHealthCheck(IMAVController control) {

		if(!MAVPreferences.getInstance().getBoolean(MAVPreferences.HEALTHCHECK, true))
			return;

		state.getParamLoadedProperty().addListener((a,o,n) -> {
			if(n.booleanValue()) {
				this.parameters = PX4Parameters.getInstance();
				System.out.println("Performing health check");
				do_check = true; healthOk = true;
				checkParameters();
				check_start_ms = System.currentTimeMillis();
			}
		});

		state.getArmedProperty().addListener((a,o,n) -> {
			if(!n.booleanValue()) {
				System.out.println("Performing health check");
				do_check = true; healthOk = true;
				check_start_ms = System.currentTimeMillis();
			}
		});

		state.getArmedProperty().addListener((a,o,n) -> {
			if(n.booleanValue()) {
				do_check = false;
				check_start_ms = 0;
			}
		});


	}


	public void check(DataModel model) {
		long dt_ms = System.currentTimeMillis()-check_start_ms;

		if(do_check && healthOk && dt_ms < HEALTH_CHECK_DURATION
				&& (System.currentTimeMillis()-check_start_ms) > WAIT_DURATION && model.sys.isStatus(Status.MSP_LANDED) ) {

			reason = null;

			// Is power > 10V

			if(model.battery.a0 < 10.0f && dt_ms > HEALTH_CHECK_DURATION-1500) {
				checkFailed("Battery too low");
			}

			// Is IMU available ?

			if(!model.sys.isSensorAvailable(Status.MSP_IMU_AVAILABILITY)) {
				checkFailed("IMU not available");
			}

			// Is LIDAR available ?

			if(!model.sys.isSensorAvailable(Status.MSP_LIDAR_AVAILABILITY)) {
				checkFailed("LIDAR not available");
			}

			// check pitch and roll

			if(healthOk) healthOk = model.attitude.r != Float.NaN;

			max_roll = model.attitude.r > max_roll ?  model.attitude.r : max_roll;
			min_roll = model.attitude.r < min_roll ?  model.attitude.r : min_roll;

			if(healthOk) healthOk = Math.abs(max_roll - min_roll) < 0.1f;

			if(healthOk) healthOk = model.attitude.p != Float.NaN;

			max_pitch = model.attitude.p > max_pitch ?  model.attitude.p : max_pitch;
			min_pitch = model.attitude.p < min_pitch ?  model.attitude.p : min_pitch;

			if(healthOk) healthOk = Math.abs(max_pitch - min_pitch) < 0.1f;

			if(!healthOk)
				checkFailed("IMU: Pitch/Roll check failed: ("+f.format(max_roll - min_roll)+","+f.format(max_pitch - min_pitch)+")");

			// check heading

			max_head = model.hud.h > max_head ? model.hud.h : max_head;
			min_head = model.hud.h < min_head ? model.hud.h : min_head;


			if(healthOk) healthOk = Math.abs(max_head - min_head) < 2f;

			if(!healthOk)
				checkFailed("IMU: heading check failed: ("+f.format(Math.abs(max_head - min_head))+")");

			// check Alt.amsl

			if(healthOk && Float.isNaN(model.hud.ag))
				checkFailed("Altitude amsl not available");


			// TODO:...add more checks here




		} else {
			if(do_check) {
				do_check = false;
				healthProperty.set(healthOk);
				if(!healthOk) {
					MSPLogger.getInstance().writeLocalMsg("[mgc] "+reason, MAV_SEVERITY.MAV_SEVERITY_ALERT);
				}
				else {
					MSPLogger.getInstance().writeLocalMsg("[mgc] vehicle healthcheck passed", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
				}

			}
		}
	}

	private void checkParameters() {

		if(parameters==null)
			return;

		// Check if kill switch is disabled
		if(parameters.get("CBRK_IO_SAFETY")!=null && parameters.get("CBRK_IO_SAFETY").value != 0) {
			checkFailed("IO SafetyBreaker set");
		}

		// TODO:...add more checks here


	}

	private void checkFailed(String r) {
		if(reason == null)
			reason = r;
		healthOk = false;
	}

	public BooleanProperty getHealthProperty() {
		return healthProperty;
	}

}
