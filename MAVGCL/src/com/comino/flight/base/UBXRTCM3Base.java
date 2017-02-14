package com.comino.flight.base;

import java.util.Vector;

import org.mavlink.messages.lquac.msg_gps_rtcm_data;

import com.comino.mav.control.IMAVController;
import com.comino.mavbase.ublox.reader.StreamEventListener;
import com.comino.mavbase.ublox.reader.UBXSerialConnection;
import com.comino.msp.model.segment.GPS;

public class UBXRTCM3Base {

	private static UBXRTCM3Base instance = null;
	private final UBXSerialConnection ubx;
	private IMAVController control;


	public static UBXRTCM3Base getInstance(IMAVController control, String port) {
		if(instance == null) {
			instance = new UBXRTCM3Base(control, port);
			System.out.println("States initialized");
		}
		return instance;
	}

	public UBXRTCM3Base(IMAVController control, String port) {
		System.out.println("StartUp RTCM3 base...");
		this.control = control;
		this.ubx = new UBXSerialConnection(port, 9600);

		ubx.addStreamEventListener( new StreamEventListener() {

			@Override
			public void streamClosed() {
				System.out.println("RTCM3: lost");
			}

			@Override
			public void getPosition(double lat, double lon, double altitude, int fix, int sats) {
                 GPS base = control.getCurrentModel().base;
                 base.latitude   = (float)lat;
                 base.longitude  = (float)lon;
                 base.altitude   = (short)altitude;
                 base.numsat     = sats;
			}

			@Override
			public void getRTCM3(byte[] buffer, int len) {

				msg_gps_rtcm_data msg = new msg_gps_rtcm_data(2,1);
				if(len < msg.data.length) {
					msg.flags = 0;
					msg.len   = len;
					for(int i = 0;i<len;i++)
						msg.data[i] = buffer[i];
					control.sendMAVLinkMessage(msg);
				} else {
					int start = 0;
					while (start < len) {
						int length = Math.min(len - start, msg.data.length);
						msg.flags = 1;
						msg.len   = length;
						for(int i = start;i<length;i++)
							msg.data[i] = buffer[i];
						control.sendMAVLinkMessage(msg);
						start += length;
					}
				}

			}

		});

	}

}
