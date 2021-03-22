package me.drton.jmavlib.log;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * User: ton Date: 03.06.13 Time: 14:51
 */
public abstract class BinaryLogReader implements LogReader {
    protected ByteBuffer buffer;
    protected FileChannel channel = null;
    protected long channelPosition = 0;

    public BinaryLogReader(String fileName) throws IOException {
        buffer = ByteBuffer.allocate(65536);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.flip();
        channel = new RandomAccessFile(fileName, "r").getChannel();
    }

    @Override
    public void close() throws IOException {
        channel.close();
        channel = null;
    }

    public int fillBuffer() throws IOException {
        buffer.compact();
        int n = channel.read(buffer);
        buffer.flip();
        if (n < 0) {
            throw new EOFException();
        }
        channelPosition += n;
        return n;
    }

    public void fillBuffer(int required) throws IOException {
        if (buffer.remaining() < required) {
            buffer.compact();
            int n = channel.read(buffer);
            buffer.flip();
            if (n < 0 || buffer.remaining() < required) {
                throw new EOFException();
            }
            channelPosition += n;
        }
    }

    protected long position() throws IOException {
        return channelPosition - buffer.remaining();
    }

    protected int position(long pos) throws IOException {
        buffer.clear();
        channel.position(pos);
        channelPosition = pos;
        int n = channel.read(buffer);
        buffer.flip();
        if (n < 0) {
            throw new EOFException();
        }
        channelPosition += n;
        return n;
    }
}
