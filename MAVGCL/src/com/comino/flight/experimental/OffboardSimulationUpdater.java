package com.comino.flight.experimental;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.lquac.msg_set_position_target_local_ned;
import org.mavlink.messages.lquac.msg_vision_position_estimate;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.MAV_CUST_MODE;

public class OffboardSimulationUpdater implements Runnable {

	private IMAVController control = null;
	private boolean isRunning = false;

	public OffboardSimulationUpdater(IMAVController control) {
		this.control = control;
	}

	public void start() {

		if(!control.isSimulation()) {
			System.out.println("OFFBOARD only for SITL !");
			return;
		}


		System.out.println("Offboard updater started");
		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
				MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
				MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_OFFBOARD, 0 );
		isRunning = true;
		new Thread(this).start();

	}

	public void stop() {
		System.out.println("Offboard updater stopped");
        isRunning = false;
	}


	@Override
	public void run() {

		while(isRunning) {

			msg_set_position_target_local_ned cmd = new msg_set_position_target_local_ned(255,1);
			cmd.target_component = 1;
			cmd.target_system = 1;
			cmd.x = 5;
			cmd.y = 5;
			cmd.z = -2;
			if(!control.sendMAVLinkMessage(cmd))
				stop();

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) { }

		}

		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_DO_SET_MODE,
				MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED,
				MAV_CUST_MODE.PX4_CUSTOM_MAIN_MODE_ALTCTL, 0 );



	}

}
