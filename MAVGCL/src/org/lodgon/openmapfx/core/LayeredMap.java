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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

/**
 *
 * @author johan
 */
public class LayeredMap extends Region {
final int MAXZOOM=16;
    private BaseMap mapArea;
    private double x0,y0;
    private final ObservableList<MapLayer> layers = FXCollections.observableArrayList();
    private ObjectProperty<BaseMapProvider> provider = new SimpleObjectProperty<>();
    
    public LayeredMap(BaseMapProvider provider) {
        this.provider.set(provider);
        this.mapArea = provider.getBaseMap();
        this.mapArea.install();
        
        this.getChildren().add(mapArea.getView());
        this.layers.addListener(new ListChangeListener<MapLayer>(){

            @Override
            public void onChanged(ListChangeListener.Change<? extends MapLayer> c) {
               while (c.next()) {
                   for (MapLayer candidate : c.getAddedSubList()) {
                       Node view = candidate.getView();
                       getChildren().add(view);
                       candidate.gotLayeredMap(LayeredMap.this);
                   }
                   for (MapLayer target : c.getRemoved()){
                       getChildren().remove(target.getView());
                   }
               }
            }
        });
        setOnMousePressed(t -> {
            x0 = t.getSceneX();
            y0 = t.getSceneY();
        });
        setOnMouseDragged(t -> {
            mapArea.moveX(x0-t.getSceneX());
            mapArea.moveY(y0-t.getSceneY());
            x0 = t.getSceneX();
            y0 = t.getSceneY();
        });
        setOnZoom(t -> mapArea.zoom(t.getZoomFactor() > 1 ? .1 : -.1, (x0 + t.getSceneX()) / 2.0, (y0 + t.getSceneY()) / 2.0));
        boolean zoomGestureEnabled = Boolean.valueOf(System.getProperty("com.sun.javafx.gestures.zoom", "false"));
        if (!zoomGestureEnabled) {
            setOnScroll(t -> mapArea.zoom(t.getDeltaY() > 1? .1: -.1, t.getSceneX(), t.getSceneY()));
        }
    }

    public void setBaseMapProvider(BaseMapProvider provider) {
        this.provider.set(provider);
        resetBaseMap();
    }
    
    private void resetBaseMap() {
        
        double zm = zoomProperty().get();
        double lat = centerLatitudeProperty().get();
        double lng = centerLongitudeProperty().get();
        
        mapArea.uninstall();
        this.getChildren().remove(mapArea.getView());
        this.mapArea = provider.get().getBaseMap();
        this.getChildren().add(0, mapArea.getView());
        this.mapArea.install();
        
        this.mapArea.setZoom(zm);
        this.mapArea.setCenter(lat, lng);
        
        synchronized(layers) {
            for (MapLayer ml : layers) {
                ml.gotLayeredMap(this);
            }
        }
        
//        this.mapArea.minHeightProperty().bind(map.heightProperty());
//		this.mapArea.minWidthProperty().bind(map.widthProperty());
        
    }
    
    /**
     * Explicitly set the zoom level for this map. The map will be zoomed
     * with the center of the map as pivot
     * @param z the zoom level
     */
    public void setZoom (double z) {
        Scene s = this.getScene();
        double x = (s == null)? 0 : s.getWidth()/2;
        double y = (s == null)? 0 : s.getWidth()/2;
        setZoom(z, x, y);
    }
    
   /**
     * Explicitly set the zoom level for this map. The map will be zoomed
     * around the supplied x-y coordinates 
     * @param z the zoom level
     * @param x the pivot point, in pixels from the origin of the map
     * @param y the pivot point, in pixels from the origin of the map
     */
    public void setZoom (double z, double x, double y) {
        double delta =  z - this.mapArea.zoomProperty().get() ;
        this.mapArea.zoom(delta, x,y);
    }
    
    /**
     * Explicitly center the map around this location
     * @param lat latitude
     * @param lon longitude
     */
    public void setCenter (double lat, double lon) {
        this.mapArea.setCenter(lat, lon);
    }
    
    /**
     * Explicitly show the rectangular viewport on the map. The center of the viewport will
     * be the center of the map. The map is scaled as big as possible,
     * ensuring though that the viewport is visible.
     * Viewport is provided by the north-east and south-west corners.
     * @param lat1 latitude north-east
     * @param lon1 longitude north-east
     * @param lat2 latitude south-west
     * @param lon2 longitude south-west
     */
    public void setViewport(double lat1, double lon1, double lat2, double lon2) {
        double latdiff = lat1 - lat2;
        double londiff = lon1 - lon2;
        double log2 = Math.log(2.);
        Scene scene = this.mapArea.getView().getScene();
        double tileX = scene.getWidth() / 256;
        double tileY = scene.getHeight() / 256;
        double latzoom = Math.log(180 * tileY / latdiff) / log2;
        double lonzoom = Math.log(360 * tileX / londiff) / log2;
        double centerX = lat2 + latdiff / 2;
        double centerY = lon2 + londiff / 2;
        double z= Math.min(MAXZOOM, Math.min(latzoom, lonzoom));
        this.mapArea.setZoom(z);
        this.mapArea.setCenter(centerX, centerY);
    }
    
    /**
     * Return the MapArea that is backing this map 
     * @return the MapArea used as the geomap for this layeredmap
     */
    public BaseMap getMapArea () {
        return this.mapArea;
    }
    
    /**
     * Return a mutable list of all layers that are handled by this LayeredMap
     * The MapArea backing the map is not part of this list
     * @return the list containing all layers
     */
    public ObservableList<MapLayer> getLayers() {
        return layers;
    }
    
    /** 
     * Return the (x,y) coordinates for the provides (lat, lon) point as it
     * would appear on the current map, talking into account the zoom and
     * translate properties
     * @param lat
     * @param lon
     * @return 
     */
    public Point2D getMapPoint (double lat, double lon) {
        return this.mapArea.getMapPoint(lat, lon);
    }
    
    /**
     * Return the geolocation (lat/lon) for a given point on the screen
     * 
     * @param sceneX
     * @param sceneY
     * @return 
     */
    public Position getMapPosition(double sceneX, double sceneY) {
       return this.mapArea.getMapPosition(sceneX, sceneY);
    }
    
    /**
     * Return the zoom property for the backing map
     * @return the zoom property for the backing map
     */
    public DoubleProperty zoomProperty() {
        return this.mapArea.zoomProperty();
    }
    
    /**
     * Return the horizontal translation of the backing map
     * @return the horizontal translation of the backing map
     */
    public DoubleProperty xShiftProperty () {
        return this.mapArea.getView().translateXProperty();
    }
    
    /**
     * Return the vertical translation of the backing map
     * @return the vertical translation of the backing map
     */
    public DoubleProperty yShiftProperty () {
        return this.mapArea.getView().translateYProperty();
    }
    
    public DoubleProperty centerLongitudeProperty() {
        return this.mapArea.centerLon();
    }
    
    public DoubleProperty centerLatitudeProperty() {
        return this.mapArea.centerLat();
    }
    
    public void addNode(Node n) {
        this.getChildren().add(n);
    }

}
