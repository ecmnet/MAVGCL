package com.comino.flight.control;

import org.mavlink.messages.lquac.msg_rc_channels_override;

import com.comino.mav.control.IMAVController;

public class SITLController implements Runnable {

	private IMAVController control;
	private msg_rc_channels_override rc = new msg_rc_channels_override(1,1);

	public SITLController(IMAVController control) {
		this.control = control;

		rc.chan1_raw = 1500;
		rc.chan2_raw = 1500;
		rc.chan3_raw = 1500;
		rc.chan4_raw = 1500;

		new Thread(this).start();
	}


	@Override
	public void run() {

		while(true) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {	}

			if(control.isConnected())
			   control.sendMAVLinkMessage(rc);

		}

	}

}
