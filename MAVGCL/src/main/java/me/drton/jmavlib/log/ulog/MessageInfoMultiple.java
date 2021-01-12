package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

public class MessageInfoMultiple {
    public final FieldFormat format;
    public final Object value;
    public final boolean isContinued;

    public MessageInfoMultiple(ByteBuffer buffer) {
        int isContinuedInt = buffer.get() & 0xFF;
        isContinued = isContinuedInt == 1;
        int keyLen = buffer.get() & 0xFF;
        format = new FieldFormat(MessageFormat.getString(buffer, keyLen));
        value = format.getValue(buffer);
    }

    public String getKey() {
        return format.name;
    }

    @Override
    public String toString() {
        return String.format("INFO_MULTI: key=%s, value_type=%s, value=%s", format.name, format.getFullTypeString(), value);
    }
}