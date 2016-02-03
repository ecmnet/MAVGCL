/*
 * Copyright (c) 2014, 2015, OpenMapFX and LodgON
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of LodgON, OpenMapFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL LODGON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lodgon.openmapfx.providers;

import java.util.LinkedList;
import java.util.List;
import org.lodgon.openmapfx.core.TileProvider;
import org.lodgon.openmapfx.core.TileType;

/**
 *
 * Provider for Bing tiles
 */
public class BingTileProvider implements TileProvider {
    
    private static final List<TileType> tileTypes = new LinkedList<>();
    final String server;

    public BingTileProvider(String server, String fileStorage) {
        this.server = server;
        TileType tileType = new TileType("BingMap", "http://tile.openstreetmap.org/") {
            @Override
            protected String calculateURL(int zoom, long i, long j) {
                return server + getQuadKey(zoom, i, j)+"?g=1";
            }
            
        };
        if (fileStorage != null) {
            tileType.setFileStorageBase(fileStorage);
        }
        tileTypes.add(tileType);
    }
    
    @Override
    public String getProviderName() {
        return "Microsoft";
    }

    @Override
    public List<TileType> getTileTypes() {
        return tileTypes;
    }

    @Override
    public TileType getDefaultType() {
        return tileTypes.get(0);
    }

    @Override
    public String getAttributionNotice() {
        return "";
    }
 
    private String getQuadKey(int zoom, long x, long y) {
        final StringBuilder quadKey = new StringBuilder();
        for (int i = zoom; i > 0; i--) {
            int digit = 0;
            final int mask = 1 << (i - 1);
            if ((x & mask) != 0) {
                digit += 1;
            }
            if ((y & mask) != 0) {
                digit += 2;
            }
            quadKey.append(digit);
        }

        return quadKey.toString();
    }
    
}
