package com.comino.flight.log.ulog;

import org.mavlink.messages.MAV_CMD;

import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVUdpController;
import com.comino.msp.log.MSPLogger;

public class UlogFromMavLinkTest implements Runnable {

	private IMAVController control = null;
	private ULogFromMAVLinkReader logger = null;

	public UlogFromMavLinkTest() {

		control = new MAVUdpController("172.168.178.1",14555,14550, false);
		MSPLogger.getInstance(control);

		logger = new ULogFromMAVLinkReader(control);

		while(!control.isConnected()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			control.connect();
			control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
		}

		new Thread(this).start();

        logger.enableLogging(true);

	}

	public static void main(String[] args) {
		new UlogFromMavLinkTest();

	}

	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		}

	}

}
