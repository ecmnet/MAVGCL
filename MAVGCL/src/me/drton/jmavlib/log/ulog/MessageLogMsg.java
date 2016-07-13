package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageLogMsg {

	public static Charset charset = Charset.forName("latin1");

	 public final int loglevel;
	 public final long tms;
	 public final String message;

	 public MessageLogMsg(ByteBuffer buffer, int msgSize) {
	        loglevel = buffer.get() & 0xFF;
	        tms = buffer.getLong();
	        message = getString(buffer, msgSize-9);
	    }

	 public static String getString(ByteBuffer buffer, int len) {
			byte[] strBuf = new byte[len];
			buffer.get(strBuf);
			String[] p = new String(strBuf, charset).split("\0");
			return p.length > 0 ? p[0] : "";
		}

}
