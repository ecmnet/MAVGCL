package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

/**
 * Created by ton on 29.09.15.
 */
public class MessageParameter extends MessageInfo {
    public MessageParameter(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public String toString() {
        return String.format("PARAMETER: key=%s, value_type=%s, value=%s", format.name, format.getFullTypeString(), value);
    }
}
