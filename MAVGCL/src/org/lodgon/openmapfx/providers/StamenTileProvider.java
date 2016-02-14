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
 * @author Geoff Capper
 */
public class StamenTileProvider implements TileProvider {
    
    private static final String providerName = "Stamen";
    
    private static final List<TileType> tileTypes = new LinkedList<>();;
    static {
        tileTypes.add(new TileType("Watercolor", "http://tile.stamen.com/watercolor/", "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA."));
        tileTypes.add(new TileType("Toner", "http://tile.stamen.com/toner/", "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA."));
        tileTypes.add(new TileType("Terrain", "http://tile.stamen.com/terrain/", "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA."));
    }
    
    public StamenTileProvider() {
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
        //return "Map tiles by <a href=\"http://stamen.com\">Stamen Design</a>, under <a href=\"http://creativecommons.org/licenses/by/3.0\">CC BY 3.0</a>. Data by <a href=\"http://openstreetmap.org\">OpenStreetMap</a>, under <a href=\"http://creativecommons.org/licenses/by-sa/3.0\">CC BY SA</a>.";
        return "Map tiles by Stamen Design (http://stamen.com/), under CC BY 3.0. Data by OpenStreetMap (http://openstreetmap.org/), under CC BY SA.";
    }
    
    @Override
    public String toString() {
        return getProviderName();
    }
    
}
