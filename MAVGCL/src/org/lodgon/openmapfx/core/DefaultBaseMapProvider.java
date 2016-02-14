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
package org.lodgon.openmapfx.core;

import java.util.LinkedList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.lodgon.openmapfx.providers.FileProvider;
import org.lodgon.openmapfx.providers.MapQuestTileProvider;
import org.lodgon.openmapfx.providers.OSMTileProvider;
import org.lodgon.openmapfx.providers.StamenTileProvider;

/**
 *
 * @author johan
 */
public class DefaultBaseMapProvider implements BaseMapProvider {
    
    private static final String mapName = "OpenMapFX Tiled Map";
    
    private MapArea baseMap;
    
    private static final List<TileProvider> tileProviders = new LinkedList<>();
    static {
        tileProviders.add(new OSMTileProvider());
        tileProviders.add(new MapQuestTileProvider());
	tileProviders.add(new StamenTileProvider());
        if (System.getProperty("fileProvider")!= null) {
            FileProvider fp = new FileProvider("OSM local", System.getProperty("fileProvider"));
            tileProviders.add(fp);           
        }
    }
    
    private final ObjectProperty<TileProvider> tileProvider = new SimpleObjectProperty<>();
    private final ObjectProperty<MapTileType> selectedTileType = new SimpleObjectProperty<>();
    
    public DefaultBaseMapProvider() {
        tileProvider.set(tileProviders.get(0));
        selectedTileType.set(tileProvider.get().getDefaultType());
    }
    
    public DefaultBaseMapProvider (TileProvider tp) {
        tileProvider.set(tp);
        selectedTileType.set(tp.getDefaultType());
    }
    
    @Override
    public String getMapName() {
        return mapName;
    }
    
    
    @Override
    public BaseMap getBaseMap() {
        if (baseMap == null) {
            baseMap = new MapArea(selectedTileType);
        }
        
        return baseMap;
    }

//    @Override
//    public List<TileType> getSupportedMapStyles() {
//        return tileProvider.get().getTileTypes();
//    }
    
	@Override
    public List<TileProvider> getTileProviders() {
        return tileProviders;
    }
    
    @Override
    public ObjectProperty<MapTileType> tileTypeProperty() {
        return selectedTileType;
    }

    @Override
    public List<TileType> getTileTypes() {
        return tileProvider.get().getTileTypes();
    }
    
    @Override
    public String toString() {
        return getMapName();
    }

    @Override
    public ObjectProperty<TileProvider> tileProviderProperty() {
        return tileProvider;
    }
    
}