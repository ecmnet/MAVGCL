package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

public class MessageDropout {
    public final int duration; //dropout duration [ms]

    public MessageDropout(ByteBuffer buffer) {
        int s1 = buffer.get() & 0xFF;
        int s2 = buffer.get() & 0xFF;
        duration = s1 + (256 * s2);
        //System.out.println("dropout: " + String.valueOf(duration));
    }

    @Override
    public String toString() {
        return String.format("MessageDropout: duration=%i", duration);
    }
}
