package com.comino.flight.experimental;

import org.mavlink.messages.lquac.msg_vision_position_estimate;

import com.comino.mav.control.IMAVController;

public class VisionPositionSimulationUpdater implements Runnable {

	private IMAVController control = null;
	private boolean isRunning = false;

	public VisionPositionSimulationUpdater(IMAVController control) {
		this.control = control;
	}

	public void start() {
		System.out.println("Vision updater started");
		isRunning = true;
		new Thread(this).start();

	}

	public void stop() {
		System.out.println("Vision updater stopped");
        isRunning = false;
	}


	@Override
	public void run() {

		while(isRunning) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }

			msg_vision_position_estimate cmd = new msg_vision_position_estimate(255,1);
			cmd.x = (float)Math.random();
			cmd.y = (float)Math.random();
			cmd.z = -5;
			if(!control.sendMAVLinkMessage(cmd))
				stop();


		}



	}

}
