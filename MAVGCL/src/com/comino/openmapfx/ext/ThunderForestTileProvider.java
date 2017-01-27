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

package com.comino.openmapfx.ext;

import java.util.LinkedList;
import java.util.List;

import org.lodgon.openmapfx.core.TileProvider;
import org.lodgon.openmapfx.core.TileType;

/**
 *
 * @author Geoff Capper
 */
public class ThunderForestTileProvider implements TileProvider {

    private static final String providerName = "ThunderForest";

    private static final List<TileType> tileTypes = new LinkedList<>();
    static {
        tileTypes.add(new TileType("Outdoors", "http://tile.thunderforest.com/outdoors/", "© OpenStreetMap/ThunderForest contributors"));
        tileTypes.add(new TileType("Landscape", "http://tile.thunderforest.com/landscape/", "© OpenStreetMap/ThunderForest contributors"));
    }

    public ThunderForestTileProvider() {
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
        return "© OpenStreetMap/ThunderForest contributors";
    }

    @Override
    public String toString() {
        return getProviderName();
    }

}
