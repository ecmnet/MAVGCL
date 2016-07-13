package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * User: ton Date: 03.06.13 Time: 16:18
 */
public class MessageData {
    public final MessageFormat format;
    private final List<Object> data;

    public MessageData(MessageFormat format, ByteBuffer buffer) {
        this.format = format;
        this.data  = format.parseBody(buffer);
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
        return String.format("DATA: name=%s, data=%s", format.name, data);
    }
}
