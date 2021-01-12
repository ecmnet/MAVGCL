package com.comino.flight.log.ulog;

import org.mavlink.messages.MAV_CMD;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.control.impl.MAVUdpController;
import com.comino.mavcom.log.MSPLogger;

public class UlogFromMavLinkTest implements Runnable {

	private IMAVController control = null;
	private ULogFromMAVLinkReader logger = null;

	public UlogFromMavLinkTest() {

	// control = new MAVUdpController("172.168.178.1",14555,14550, false);
		control = new MAVUdpController("127.0.0.1",14556,14550, true);
		MSPLogger.getInstance(control);

		logger = new ULogFromMAVLinkReader(control, false);

		while(!control.isConnected()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			control.connect();
		}

		new Thread(this).start();



	}

	public static void main(String[] args) {
		new UlogFromMavLinkTest();

	}

	@Override
	public void run() {
		control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_LOGGING_STOP);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {

		}
		 logger.enableLogging(true);
		 float val = 0;
		while(true) {
			try {
				Thread.sleep(10);
				if(control.isConnected()) {
        val = (float)logger.getData().get("sensor_combined_0.accelerometer_m_s2[2]");
			//	  System.out.println((int)(logger.lostPackageRatio()*100f)+"%");
                 if(val > -8 || val < -10)
					System.out.println(val);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		}

	}

}
