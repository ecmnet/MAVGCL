package me.drton.jmavlib.mavlink;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkStream {
    private final MAVLinkSchema schema;
    private final ByteChannel channel;
    private byte txSeq = 0;
    private ByteBuffer buffer = ByteBuffer.allocate(8192);
    private boolean debug = false;

    public MAVLinkStream(MAVLinkSchema schema, ByteChannel channel) {
        this.schema = schema;
        this.channel = channel;
        buffer.flip();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Write message.
     *
     * @param msg Message
     * @throws IOException on IO error
     */
    public void write(MAVLinkMessage msg) throws IOException {
        channel.write(msg.encode(txSeq++));
    }

    /**
     * Read message.
     *
     * @return MAVLink message or null if no more messages available at the moment
     * @throws java.io.IOException on IO error
     */
    public MAVLinkMessage read() throws IOException {
        while (true) {
            try {
                return new MAVLinkMessage(schema, buffer);
            } catch (MAVLinkProtocolException e) {
                // Message is corrupted, try to sync on the next byte
                if (debug) {
                    System.err.println(String.format("%s: %s", channel, e));
                }
            } catch (MAVLinkUnknownMessage e) {
                // Message looks ok but with another protocol, skip it
                if (debug) {
                    System.err.println(String.format("%s: %s", channel, e));
                }
            } catch (BufferUnderflowException e) {
                // Try to refill buffer
                buffer.compact();
                int n = 0;
                try {
                    n = channel.read(buffer);
                } catch (IOException ioe) {
                    // In case of exception don't forget to flip the buffer
                    buffer.flip();
                    throw ioe;
                }
                buffer.flip();
                if (n <= 0) {
                    return null;
                }
            }
        }
    }

    public long position() throws IOException {
        if (channel instanceof FileChannel) {
            return ((FileChannel) channel).position() + buffer.position() - buffer.limit();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void position(long pos) throws IOException {
        if (channel instanceof FileChannel) {
            ((FileChannel) channel).position(pos);
            buffer.clear();
            buffer.flip();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
