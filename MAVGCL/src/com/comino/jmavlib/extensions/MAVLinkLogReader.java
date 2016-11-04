package com.comino.jmavlib.extensions;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import com.comino.msp.main.control.listener.IMAVLinkListener;

import me.drton.jmavlib.log.LogReader;


/**
 * User: ton Date: 03.06.13 Time: 14:51
 */
public abstract class MAVLinkLogReader implements LogReader {
    protected ByteBuffer buffer;
    protected SeekableByteChannel channel = null;
    protected long channelPosition = 0;

    public MAVLinkLogReader()  {
        buffer = ByteBuffer.allocate(8192);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.flip();
        channel = new SeekableInMemoryByteChannel();
    }

    public void clear() {
    	 channel = new SeekableInMemoryByteChannel();
    }

    public long size() throws IOException {
   	  return channel.size();
   }

    public void addDataPacket(int[] data, int len) throws IOException {
    	byte[] in = new byte[len];
    	for(int i=0;i<len;i++)
			in[i]= (byte)(data[i] & 0x00FF);
    	channel.write(ByteBuffer.wrap(in));
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
