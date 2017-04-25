package com.comino.flight.log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProgressInputStream extends FilterInputStream {

	private final int size;
    private long bytesRead;
    private int percent;
    private List<Listener> listeners = new ArrayList<>();

    public ProgressInputStream(InputStream in) {
        super(in);
        try {
            size = available();
            if (size == 0) throw new IOException();
        } catch (IOException e) {
            throw new RuntimeException("This reader can only be used on InputStreams with a known size", e);
        }
        bytesRead = 0;
        percent = 0;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        updateProgress(1);
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return updateProgress(super.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return updateProgress(super.read(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        return updateProgress(super.skip(n));
    }

    @Override
    public void mark(int readLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private <T extends Number> T updateProgress(T numBytesRead) {
        if (numBytesRead.longValue() > 0) {
            bytesRead += numBytesRead.longValue();
            if (bytesRead * 100 / size > percent) {
                percent = (int) (bytesRead * 100 / size);
                for (Listener listener : listeners) {
                    listener.onProgressChanged(percent);
                }
            }
        }
        return numBytesRead;
    }

    public interface Listener {
        void onProgressChanged(int percentage);
    }
}