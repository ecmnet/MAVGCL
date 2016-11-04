package com.comino.flight.log.ulog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.mavlink.messages.lquac.msg_logging_ack;
import org.mavlink.messages.lquac.msg_logging_data;
import org.mavlink.messages.lquac.msg_logging_data_acked;

import com.comino.jmavlib.extensions.ULogReaderMAVLink;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import me.drton.jmavlib.log.FormatErrorException;



public class ULogFromMAVLinkReader implements IMAVLinkListener {



	private IMAVController control   = null;
	private ULogReaderMAVLink reader = null;
	private Map<String,Object> data =null;

	private long last_tms = 0;
	private boolean initialized=false;

	public ULogFromMAVLinkReader(IMAVController control)  {

		this.control = control;
		this.control.addMAVLinkListener(this);
		this.reader = new ULogReaderMAVLink();
		data = new HashMap<String,Object>();
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_logging_data_acked) {
			if(initialized)
				reader.clear();
			initialized = false;
			msg_logging_data_acked log = (msg_logging_data_acked)o;
			msg_logging_ack ack = new msg_logging_ack(255,1);
			ack.target_component=1;
			ack.target_system=1;
			ack.sequence = log.sequence;
			control.sendMAVLinkMessage(ack);
			try {
				reader.addDataPacket(log.data, log.length);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if( o instanceof msg_logging_data) {
			if(!initialized) {
				try {
					if(reader.size()>0) {
					   reader.updateStatistics();
					initialized = true; }
				} catch (Exception  e) {
					e.printStackTrace();
				}
			}
			if(initialized) {
				msg_logging_data log = (msg_logging_data)o;
				try {
					reader.addDataPacket(log.data, log.length);
					long tms = reader.readUpdate(data);
					if(tms > last_tms) {
					   System.out.println((tms/1000)+":"+data.get("vehicle_local_position_0.z"));
					   last_tms = tms;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}





	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
		char[] hexChars = new char[len * 2];
		for ( int j = 0; j <len; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String intsToHex(int[] bytes, int len) {
		char[] hexChars = new char[len * 2];
		for ( int j = 0; j <len; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}



}
