package com.comino.jmavlib.extensions;

import java.nio.ByteBuffer;

public class DummyMessage {

	public DummyMessage(ByteBuffer buffer, int msgSize,int offset) {
		buffer.position(buffer.position() + msgSize - offset -3);
	}
}
