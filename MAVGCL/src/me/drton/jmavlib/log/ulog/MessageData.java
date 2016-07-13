package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.util.List;

import me.drton.jmavlib.log.FormatErrorException;

/**
 * User: ton Date: 03.06.13 Time: 16:18
 */
public class MessageData {
    public final MessageFormat format;
    public final Long timestamp;
    private final List<Object> data;
    public final int multiID;

    public MessageData(MessageFormat format, ByteBuffer buffer, int multiID) throws FormatErrorException {
        this.format = format;
        this.data = format.parseBody(buffer);
        this.multiID = multiID;
        Object t = get("timestamp");
        if (t == null)
            throw new FormatErrorException("Message " + format.name + " has no timestamp field");

        //TODO: parse non-64bit timestamp (depending on field type) & handle wrap-arounds
        timestamp = ((Number) t).longValue();
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
        return String.format("DATA: t=%s multi_id=%s, name=%s, data=%s", timestamp, multiID, format.name, data);
    }
}
