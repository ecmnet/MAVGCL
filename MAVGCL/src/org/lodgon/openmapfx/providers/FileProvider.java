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
 * @author johan
 */
public class FileProvider implements TileProvider {
    
    private String providerName = "FileProvider";
    private String copy;
    private String baseUrl;
    
    private List<TileType> tileTypes = new LinkedList<>();

    /**
     * Create a FileProvider without a copyright. It is assumed the tiles are open.
     * @param name the name of the provider
     * @param base the base url for the tiles. Tiles will be retrieved by adding zoom/x/y.png to the base url
     */
    public FileProvider (String name, String base) {
        this (name, base, "open");
    }
    
    public FileProvider(String name, String base, String copy) {
        this.providerName = name;
        this.copy = copy;
        this.baseUrl = base;
        tileTypes.add(new TileType(name, base, copy));
    }
    
    @Override
    public String getProviderName() {
        return providerName;
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
        return copy;
    }
    
    @Override
    public String toString() {
        return getProviderName();
    }
    
}
