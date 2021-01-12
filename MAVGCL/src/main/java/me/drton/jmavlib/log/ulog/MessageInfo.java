package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

/**
 * Created by ton on 29.09.15.
 */
public class MessageInfo {
    public final FieldFormat format;
    public final Object value;

    public MessageInfo(ByteBuffer buffer) {
        int keyLen = buffer.get() & 0xFF;
        format = new FieldFormat(MessageFormat.getString(buffer, keyLen));
        value = format.getValue(buffer);
    }

    public String getKey() {
        return format.name;
    }

    @Override
    public String toString() {
        return String.format("INFO: key=%s, value_type=%s, value=%s", format.name, format.getFullTypeString(), value);
    }
}
