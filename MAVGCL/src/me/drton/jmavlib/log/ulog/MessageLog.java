package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

public class MessageLog {
    public final String message;
    public final long timestamp;
    public final char logLevel;

    public MessageLog(ByteBuffer buffer, int msgSize) {
        logLevel = (char) (buffer.get() & 0xFF);
        timestamp = buffer.getLong();
        message = MessageFormat.getString(buffer, msgSize - 9);
    }

    public String getLevelStr() {
        switch (logLevel) {
        case '0':
            return "EMERG";
        case '1':
            return "ALERT";
        case '2':
            return "CRIT";
        case '3':
            return "ERROR";
        case '4':
            return "WARNING";
        case '5':
            return "NOTICE";
        case '6':
            return "INFO";
        case '7':
            return "DEBUG";
        }
        return "(unknown)";
    }

    @Override
    public String toString() {
        return String.format("LOG: time=%s, level=%s, message=%s, value=%s", timestamp, getLevelStr(), message);
    }
}
