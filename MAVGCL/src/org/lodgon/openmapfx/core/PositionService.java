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

import java.util.Iterator;
import java.util.ServiceLoader;
import javafx.beans.property.ObjectProperty;

/**
 *
 * @author johan
 */
public class PositionService {
    
    private static PositionService instance;
    private ServiceLoader<PositionProvider> loader;
    private PositionProvider positionProvider;
    
    private ObjectProperty<Position> positionProperty;
    
    private PositionService() {
        System.out.println("Loading PositionService");
        loader = ServiceLoader.load(PositionProvider.class);
        Iterator<PositionProvider> iterator = loader.iterator();
        while (iterator.hasNext()) {
            if (positionProvider == null) {
                positionProvider = iterator.next();
                System.out.println("We will use this positionprovider: "+positionProvider);
                positionProperty = positionProvider.getPositionProperty();
            } else {
                System.out.println("Ignoring positionprovider "+iterator.next());
            }
        }
        System.out.println("Loading PositionService done");
    }
    
    public static PositionService getInstance() {
        if (instance == null) {
            instance = new PositionService();
        }
        return instance;
    }
    
    public ObjectProperty<Position> positionProperty () {
        return positionProperty;
    }
}
