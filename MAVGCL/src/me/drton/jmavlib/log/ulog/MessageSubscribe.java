package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;


public class MessageSubscribe {

	public static Charset charset = Charset.forName("latin1");

	public final String formatName;
	public final int multiID;
	public final int msgID;


	public MessageSubscribe(ByteBuffer buffer, int msgSize) {
		multiID = buffer.get();
		msgID = buffer.getShort();
		formatName = getString(buffer, msgSize-3);
	}



	@Override
	public String toString() {
		return String.format("INFO: msgID=%d, name=%s", msgID, formatName);
	}

	public static String getString(ByteBuffer buffer, int len) {
		byte[] strBuf = new byte[len];
		buffer.get(strBuf);
		String[] p = new String(strBuf, charset).split("\0");
		return p.length > 0 ? p[0] : "";
	}
}
