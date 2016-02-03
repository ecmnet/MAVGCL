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

import java.util.List;
import javafx.beans.property.ObjectProperty;

/** 
 * A BaseMapProvider implementation can return a BaseMap implementation that 
 * can be plugged in as the bottom layer of the LayeredMap. It is distinct 
 * from the BaseMap to keep all the additional setup and styling methods out 
 * of the actual map infrastructure.
 *
 * @author Geoff Capper
 */
public interface BaseMapProvider {

    /** A descriptive name to be used in any UI elements.
     * 
     * @return 
     */
    public String getMapName();

    /** The BaseMap implementation that will be shown in the LayeredMap.
     * 
     * @return 
     */
    public BaseMap getBaseMap();

    /** Supplies a potentially empty list of {@link TileProvider}s that can 
     * supply tiles for this map.
     * 
     * @return 
     */
    public List<? extends TileProvider> getTileProviders();

    /**  Supported {@link MapTileType}s that can be shown by this type of map.
     * 
     * @return 
     */
    public List<? extends MapTileType> getTileTypes();

    /** A property that can be bound to a UI control then used to switch the 
     * base map in the {@link LayeredMap}
     * 
     * @return 
     */
    public ObjectProperty<TileProvider> tileProviderProperty();

    /** A property that can be bound to a UI control and then used to switch 
     * the tile type in the {@link LayeredMap}
     * 
     * @return 
     */
    public ObjectProperty<MapTileType> tileTypeProperty();

}
