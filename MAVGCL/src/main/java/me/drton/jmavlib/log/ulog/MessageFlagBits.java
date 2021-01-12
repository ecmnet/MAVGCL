package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

public class MessageFlagBits {
    public final int compatFlags[] = new int[8];
    public final int incompatFlags[] = new int[8];
    public final long appendedOffsets[] = new long[3];

    public MessageFlagBits(ByteBuffer buffer, int msgSize) {
        for (int i = 0; i < 8; ++i) {
            compatFlags[i] = buffer.get() & 0xFF;
        }
        for (int i = 0; i < 8; ++i) {
            incompatFlags[i] = buffer.get() & 0xFF;
        }
        for (int i = 0; i < 3; ++i) {
            appendedOffsets[i] = buffer.getLong();
        }
    }

    @Override
    public String toString() {
        return String.format("FlagBits: compat[0]=%d, incompat[0]=%d, offset[0]=%d", compatFlags[0], incompatFlags[0], appendedOffsets[0]);
    }
}
