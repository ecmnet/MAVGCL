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

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Primary map provider, this is the lowest level on the layer stack. The 
 * default implementation uses OpenStreetMap.
 *
 * @author Geoff Capper
 * @author johan
 */
public interface BaseMap {
    
    /** Moves the center of the map to the specified location.
     * 
     * @param lat The latitude in degrees.
     * @param lon The longitude in degrees.
     */
    public void setCenter(double lat, double lon);
    
    /**
     * Move the center of the map horizontally by a number of pixels.
     *
     * @param dx the number of pixels
     */
    public void moveX(double dx);
    
    /**
     * Move the center of the map vertically by a number of pixels.
     *
     * @param dy the number of pixels
     */
    public void moveY(double dy);
    
    /** Sets the map zoom level to the specified value.
     * 
     * @param z 
     */
    public void setZoom(double z);
    
    /**
     * @param delta
     * @param pivotX
     * @param pivotY 
     */
    public void zoom(double delta, double pivotX, double pivotY);
    
    /**
     * @return 
     */
    public DoubleProperty zoomProperty();
    
    /**
     * @param lat The latitude in degrees.
     * @param lon The longitude in degrees.
     * @return The coordinate on the map that equates to the specified location.
     */
    public Point2D getMapPoint (double lat, double lon);
    
    /**
     * Return the geolocation (lat/lon) for a given point on the screen
     * 
     * @param sceneX
     * @param sceneY
     * @return 
     */
    public Position getMapPosition(double sceneX, double sceneY);
     
    /** The current center longitude.
     * 
     * @return 
     */
    public DoubleProperty centerLon();
    
    /** The current center latitude.
     * 
     * @return 
     */
    public DoubleProperty centerLat();
    
    /**
     * The UI component for this BaseMap. This is the component that does the
     * actual visualization
     * @return  the visual component of the BaseMap
     */
    public Node getView();
    
    /** Called by the LayerMap to allow the BaseMap to install any needed 
     * bindings and listeners.
     */
    public void install();
    
    /** Called by the LayerMap to allow the BaseMap to uninstall any 
     * bindings and listeners.
     */
    public void uninstall();
    
}
