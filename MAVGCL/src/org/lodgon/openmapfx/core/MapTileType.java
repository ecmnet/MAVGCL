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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javafx.concurrent.Worker;
import javafx.scene.image.Image;

/** Interface that describes a type of map tile.
 *
 * @author Geoff Capper
 */
public interface MapTileType {

    /**
     * The display name for this style of map, for use in the user interface.
     *
     * @return
     */
    String getTypeName();

    /**
     * Returns the base URL for obtaining this type of tile from the tile provider.
     * For implementations that don't use a tile provider this can return null.
     *
     * @return The base URL, ending in a forward slash so that zoom and location
     * can be appended directly, or null.
     */
    String getBaseURL();

    /**
     * An attribution for the tiles, to cover situations where copyright
     * requires displaying a notice based on the type of tile displayed.
     *
     * @return
     */
    String getAttributionNotice();

    Worker<Image> getImage(int zoom, long x, long y);

    void setFileStorageBase(String store);

}