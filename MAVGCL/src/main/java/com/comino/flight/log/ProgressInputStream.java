/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProgressInputStream extends FilterInputStream {

	private final float size;
    private long bytesRead;
    private float percent;
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
                percent = bytesRead  / size;
                for (Listener listener : listeners) {
                    listener.onProgressChanged(percent);
                }
            }
        }
        return numBytesRead;
    }

    public interface Listener {
        void onProgressChanged(float percentage);
    }
}