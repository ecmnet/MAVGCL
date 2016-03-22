package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * User: ton Date: 03.06.13 Time: 16:18
 */
public class MessageData {
    public final MessageFormat format;
    public final Long timestamp;
    private final List<Object> data;
    public final int multiID;
    public final boolean isActive;

    public MessageData(MessageFormat format, ByteBuffer buffer) {
        this.format = format;
        int multiIDRaw = buffer.get() & 0xFF;
        this.multiID = multiIDRaw & 0x7F;
        this.isActive = (multiIDRaw & 0x80) != 0;
        this.timestamp = buffer.getLong();
        this.data = format.parseBody(buffer);
    }

    public Object get(int idx) {
        return data.get(idx);
    }

    public Object get(String field) {
        Integer idx = format.fieldsMap.get(field);
        return idx == null ? null : data.get(idx);
    }

    @Override
    public String toString() {
        return String.format("DATA: t=%s msg_id=%s, multi_id=%s, name=%s, data=%s", timestamp, format.msgID, multiID, format.name, data);
    }
}
