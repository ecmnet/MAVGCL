/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.video.src.impl.http;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Hashtable;

public class StreamSplit {
    public static final String BOUNDARY_MARKER_PREFIX  = "--";
    public static final String BOUNDARY_MARKER_TERM    = BOUNDARY_MARKER_PREFIX;

	protected DataInputStream m_dis;
	private boolean m_streamEnd;

	private static ByteBuffer buf = ByteBuffer.allocate(1000000);;


	public StreamSplit(DataInputStream dis)
	{
		m_dis = dis;
		m_streamEnd = false;

	}


	public Hashtable readHeaders() throws IOException
	{
		Hashtable ht = new Hashtable();
		String response;
		boolean satisfied = false;

		do {
            //noinspection deprecation
            response = m_dis.readLine();
			if (response == null) {
				m_streamEnd = true;
				break;
			} else if (response.equals("")) {
                if (satisfied) {
				    break;
                } else {
                    // Carry on...
                }
			} else {
                satisfied = true;
            }
            addPropValue(response, ht);
        } while (true);
		return ht;
	}

    protected  void addPropValue(String response, Hashtable ht)
    {
        int idx = response.indexOf(":");
        if (idx == -1) {
            return;
        }
        String tag = response.substring(0, idx);
        String val = response.substring(idx + 1).trim();
        ht.put(tag.toLowerCase(), val);
    }


    public  Hashtable readHeaders(URLConnection conn)
    {
        Hashtable ht = new Hashtable();
        int i = 0;
        do {
            String key = conn.getHeaderFieldKey(i);
            if (key == null) {
                if (i == 0) {
                    i++;
                    continue;
                } else {
                    break;
                }
            }
            String val = conn.getHeaderField(i);
            ht.put(key.toLowerCase(), val);
            i++;
        } while (true);
        return ht;
    }


	public void skipToBoundary(String boundary) throws IOException
	{
		readToBoundary(boundary);
	}


	public byte[] readToBoundary(String boundary) throws IOException
	{

		buf.position(0);
		StringBuffer lastLine = new StringBuffer();
		int lineidx = 0;
		int chidx = 0;
		byte ch;
		do {
			
			try {
				if(m_dis.available()<1) {
					Thread.sleep(100);
					continue;
				}
					
				//m_dis.readFully(buffer);
				ch = m_dis.readByte();
			} catch (Exception e) {
				m_streamEnd = true;
				break;
			}
//			for(int k=0;k<buffer.length;k++) {
//				ch = buffer[k];

			if (ch == '\n' || ch == '\r') {
				//
				// End of line... Note, this will now look for the boundary
                // within the line - more flexible as it can habdle
                // arfle--boundary\n  as well as
                // arfle\n--boundary\n
				//
				String lls = lastLine.toString();
                int idx = lls.indexOf(BOUNDARY_MARKER_PREFIX);
                if (idx != -1) {
                    lls = lastLine.substring(idx);
                    if (lls.startsWith(boundary)) {
                        //
                        // Boundary found - check for termination
                        //
                        String btest = lls.substring(boundary.length());
                        if (btest.equals(BOUNDARY_MARKER_TERM)) {
                            m_streamEnd = true;
                        }
                        chidx = lineidx+idx;
                        break;
                    }
				}
				lastLine = new StringBuffer();
				lineidx = chidx + 1;
			} else {
				lastLine.append((char) ch);
			}

			chidx++;
			buf.put(ch);
		} while (true);
		return buf.array();
	}


	public boolean isAtStreamEnd()
	{
		return m_streamEnd;
	}
}


class ResizableByteArrayOutputStream extends ByteArrayOutputStream {
	public ResizableByteArrayOutputStream()
	{
		super();
	}


	public void resize(int size)
	{
		count = size;
	}
}