package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageAddLogged {
    public static Charset charset = Charset.forName("latin1");
    public final String name;
    public final int msgID;
    public final int multiID;

    public static String getString(ByteBuffer buffer, int len) {
        byte[] strBuf = new byte[len];
        buffer.get(strBuf);
        String[] p = new String(strBuf, charset).split("\0");
        return p.length > 0 ? p[0] : "";
    }

    public MessageAddLogged(ByteBuffer buffer, int messageLen) {
        multiID = buffer.get() & 0xFF;
        int s1 = buffer.get() & 0xFF;
        int s2 = buffer.get() & 0xFF;
        msgID = s1 + (256 * s2);
        name = getString(buffer, messageLen - 3);
    }

    @Override
    public String toString() {
        return String.format("AddLoggingMsg: name=%s, ID=%i, multiID=%i", name, msgID, multiID);
    }
}
