package com.comino.flight.model.service;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.observables.StateProperties;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;

public class VehicleHealthCheck {

	private static final long HEALTH_CHECK_DURATION = 5000;

	private StateProperties state = StateProperties.getInstance();

	private long  check_start_ms = 0;
	private boolean do_check = false;
	private boolean healthOk  = true;

	private float max_roll = -Float.MAX_VALUE, min_roll = Float.MAX_VALUE;
	private float max_pitch= -Float.MAX_VALUE, min_pitch= Float.MAX_VALUE;

	public VehicleHealthCheck() {

		state.getConnectedProperty().addListener((a,o,n) -> {
			if(n.booleanValue()) {
				System.out.println("Performing health check");
				do_check = true;
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
		if(do_check && (System.currentTimeMillis()-check_start_ms) < HEALTH_CHECK_DURATION ) {


			// Is power > 11V

			if(model.battery.a0 < 11.0f)
				healthOk = false;

			// Is IMU available ?

			if(!model.sys.isSensorAvailable(Status.MSP_IMU_AVAILABILITY))
				healthOk = false;

			// check pitch and roll

			if(healthOk) healthOk = model.attitude.r != Float.NaN;

			max_roll = model.attitude.r > max_roll ?  model.attitude.r : max_roll;
			min_roll = model.attitude.r < min_roll ?  model.attitude.r : min_roll;

			if(healthOk) healthOk = Math.abs(max_roll - min_roll) < 0.1f;

			if(healthOk) healthOk = model.attitude.p != Float.NaN;

			max_pitch = model.attitude.p > max_pitch ?  model.attitude.p : max_pitch;
			min_pitch = model.attitude.p < min_pitch ?  model.attitude.p : min_pitch;

			if(healthOk) healthOk = Math.abs(max_pitch - min_pitch) < 0.1f;



		} else {
			if(do_check) {
				do_check = false;
				analyse_results();
			}
		}
	}


	private void analyse_results() {
		if(!healthOk) {
			MSPLogger.getInstance().writeLocalMsg("MSP vehicle healthcheck failed", MAV_SEVERITY.MAV_SEVERITY_CRITICAL);
		} else
			MSPLogger.getInstance().writeLocalMsg("MSP vehicle healthcheck passed", MAV_SEVERITY.MAV_SEVERITY_NOTICE);

	}

}
