package com.comino.flight.model.service;

import java.util.Set;

import org.mavlink.messages.MAV_SEVERITY;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.parameter.ParameterAttributes;
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

	private BooleanProperty healthProperty = new SimpleBooleanProperty();

	private PX4Parameters parameters = null;

	public VehicleHealthCheck() {

		state.getParamLoadedProperty().addListener((a,o,n) -> {
			if(n.booleanValue()) {
				this.parameters = PX4Parameters.getInstance();
				System.out.println("Performing health check");
				do_check = true;
				checkParameters();
				check_start_ms = System.currentTimeMillis();
			}
		});

		state.getArmedProperty().addListener((a,o,n) -> {
			if(!n.booleanValue()) {
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
		if(do_check && (System.currentTimeMillis()-check_start_ms) < HEALTH_CHECK_DURATION
				&& (System.currentTimeMillis()-check_start_ms) > WAIT_DURATION ) {


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
				healthProperty.set(healthOk);
				if(!healthOk)
					MSPLogger.getInstance().writeLocalMsg("MSP vehicle healthcheck failed", MAV_SEVERITY.MAV_SEVERITY_CRITICAL);
				 else
					MSPLogger.getInstance().writeLocalMsg("MSP vehicle healthcheck passed", MAV_SEVERITY.MAV_SEVERITY_NOTICE);

			}
		}
	}

	private void checkParameters() {

		if(parameters==null)
			return;

		// Check if kill switch is disabled

		if(parameters.get("CBRK_IO_SAFETY").value != 0) {
			healthOk = false;
		}

	}

	public BooleanProperty getHealthProperty() {
		return healthProperty;
	}

}
