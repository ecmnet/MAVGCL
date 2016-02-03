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

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author johan
 */
public class MapArea extends Group implements BaseMap {

    /**
     * When the zoom-factor is less than TIPPING below an integer, we will use
     * the higher-level zoom and scale down.
     */
    public static final double TIPPING = 0.2;

    /**
     * The maximum zoom level this map supports.
     */
    public static final int MAX_ZOOM = 20;
    private final Map<Long, SoftReference<MapTile>>[] tiles = new HashMap[MAX_ZOOM];

    private int nearestZoom;

    private final DoubleProperty zoomProperty = new SimpleDoubleProperty();
	
    private double lat;
    private double lon;
    private boolean abortedTileLoad;
	
    private boolean debug = false;
    private final Rectangle area;
    private DoubleProperty centerLon = new SimpleDoubleProperty();
    private DoubleProperty centerLat = new SimpleDoubleProperty();
    
    private final ObjectProperty<MapTileType> tileType = new SimpleObjectProperty<>();
    
    private InvalidationListener sceneListener;
    
    public MapArea(ObjectProperty<MapTileType> tileType) {
        this.tileType.bind(tileType);
        
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new HashMap<>();
        }
        area = new Rectangle(-10, -10, 810, 610);
        area.setVisible(false);
        
//        area.translateXProperty().bind(translateXProperty().multiply(-1));
//        area.translateYProperty().bind(translateYProperty().multiply(-1));
//        this.sceneProperty().addListener(i -> {
//            if (getScene() != null) {
//                area.widthProperty().bind(getScene().widthProperty().add(20));
//                area.heightProperty().bind(getScene().heightProperty().add(20));
//                if (abortedTileLoad) {
//                    abortedTileLoad = false;
//                    setCenter(lat, lon);
//                }
//            }
//        });
        zoomProperty.addListener((ov, t, t1)
                -> nearestZoom = (Math.min((int) floor(t1.doubleValue() + TIPPING), MAX_ZOOM - 1)));
        
        this.tileType.addListener((ObservableValue<? extends MapTileType> obs, MapTileType o, MapTileType n) -> {
            System.out.println("TileType was changed: " + n);
            if (n != null) {
                reloadTiles();
            } else {
                clearTiles();
            }
        });

    }
    
    public ObjectProperty<MapTileType> tileTypeProperty() {
        return tileType;
    }
    
    public void setCenter(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        if (getScene() == null) {
            abortedTileLoad = true;
            if (debug) {
                System.out.println("Ignore setting center since scene is null.");
            }
            return;
        }
        double activeZoom = zoomProperty.get();
        double n = Math.pow(2, activeZoom);
        double lat_rad = Math.PI * lat / 180;
        double id = n / 360. * (180 + lon);
        double jd = n * (1 - (Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI)) / 2;
        double mex = (double) id * 256;
        double mey = (double) jd * 256;
        double ttx = mex - this.getScene().getWidth() / 2;
        double tty = mey - this.getScene().getHeight() / 2;
        setTranslateX(-1 * ttx);
        setTranslateY(-1 * tty);
        if (debug) {
            System.out.println("setCenter, tx = " + this.getTranslateX() + ", with = " + this.getScene().getWidth() / 2 + ", mex = " + mex);
        }
        loadTiles();
    }

    /**
     * Move the center of the map horizontally by a number of pixels. After this
     * operation, it will be checked if new tiles need to be downloaded
     *
     * @param dx the number of pixels
     */
    public void moveX(double dx) {
        setTranslateX(getTranslateX() - dx);
        loadTiles();
    }

    /**
     * Move the center of the map vertically by a number of pixels. After this
     * operation, it will be checked if new tiles need to be downloaded
     *
     * @param dy the number of pixels
     */
    public void moveY(double dy) {
        double zoom = zoomProperty.get();
        double maxty = 256 * Math.pow(2, zoom) - this.getScene().getHeight();
        if (debug) {
            System.out.println("ty = " + getTranslateY() + " and dy = " + dy);
        }
        if (getTranslateY() <= 0) {
            if (getTranslateY() + maxty >= 0) {
                setTranslateY(Math.min(0, getTranslateY() - dy));
            } else {
                setTranslateY(-maxty + 1);
            }
        } else {
            setTranslateY(0);
        }
        loadTiles();
    }

    public void setZoom(double z) {
        if (debug) {
            System.out.println("setZoom called");
        }
        zoomProperty.set(z);
        setCenter(this.lat, this.lon);
    }

    public void zoom(double delta, double pivotX, double pivotY) {
        double dz = delta;// > 0 ? .1 : -.1;
        double zp = zoomProperty.get();
        if (debug) {
            System.out.println("Zoom called, zp = " + zp + ", delta = " + delta + ", px = " + pivotX + ", py = " + pivotY);
        }
        double txold = getTranslateX();
        double t1x = pivotX - getTranslateX();
        double t2x = 1. - Math.pow(2, dz);
        double totX = t1x * t2x;
        double tyold = getTranslateY();
        double t1y = pivotY - tyold;
        double t2y = 1. - Math.pow(2, dz);
        double totY = t1y * t2y;
        if (debug) {
            System.out.println("zp = " + zp + ", txold = " + txold + ", totx = " + totX + ", tyold = " + tyold + ", toty = " + totY);
        }
        if ((delta > 0)) {
            if (zp < MAX_ZOOM) {
                setTranslateX(txold + totX);
                setTranslateY(tyold + totY);
                zoomProperty.set(zp + delta);
                loadTiles();
            }
        } else {
            if (zp > 1) {
                double nz = zp +delta;
                if (Math.pow(2, nz) * 256 > this.getScene().getHeight()) {
                    // also, we need to fit on the current screen
                    setTranslateX(txold + totX);
                    setTranslateY(tyold + totY);
                    zoomProperty.set(zp +delta);
                    loadTiles();
                } else {
                    System.out.println("sorry, would be too small");
                }
            }
        }
        if (debug) {
            System.out.println("after, zp = " + zoomProperty.get());
        }
    }

    public DoubleProperty zoomProperty() {
        return zoomProperty;
    }
    
    public Point2D getMapPoint (double lat, double lon) {
        return getMapPoint (zoomProperty.get(), lat, lon);
    }
    
    private Point2D getMapPoint(double zoom, double lat, double lon) {
        if (this.getScene() == null) return null;
        double n = Math.pow(2,zoom);
        double lat_rad = Math.PI * lat / 180;
        double id = n / 360. * (180 + lon);
        double jd = n * (1 - (Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI)) / 2;
        double mex = (double) id * 256;
        double mey = (double) jd * 256;
        double ttx = mex -this.getScene().getWidth()/2;
        double tty = mey - this.getScene().getHeight()/2;
        double x = this.getTranslateX() + mex;
        double y = this.getTranslateY() + mey;
        Point2D answer = new  Point2D(x,y);
        return answer;
    }
    
    public Position getMapPosition(double sceneX, double sceneY) {
        double x = sceneX-this.getTranslateX();
        double y = sceneY-this.getTranslateY();
        double zoom =zoomProperty().get();
        double latrad = Math.PI - (2.0 * Math.PI * y) / (Math.pow(2, zoom)*256.);
        double mlat = Math.toDegrees(Math.atan(Math.sinh(latrad)));
        double mlon = x / (256*Math.pow(2, zoom)) * 360 - 180;
        return new Position(mlat, mlon);
    }
    
    private void calculateCenterCoords() {
        double x = this.getScene().getWidth()/2-this.getTranslateX();
        double y = this.getScene().getHeight()/2 - this.getTranslateY();
        double zoom = zoomProperty.get();
        double latrad = Math.PI - (2.0 * Math.PI * y) / (Math.pow(2, zoom)*256.);
        double mlat = Math.toDegrees(Math.atan(Math.sinh(latrad)));
        double mlon = x / (256*Math.pow(2, zoom)) * 360 - 180;
        centerLon.set(mlon);
        centerLat.set(mlat);
    }
    
    public DoubleProperty centerLon() {
        return centerLon;
    }
    
    public DoubleProperty centerLat() {
        return centerLat;
    }
    
    private final void loadTiles() {
        if (getScene() == null) {
            return;
        }
        double activeZoom = zoomProperty.get();
        double deltaZ = nearestZoom - activeZoom;
        long i_max = 1 << nearestZoom;
        long j_max = 1 << nearestZoom;
        double tx = getTranslateX();
        double ty = getTranslateY();
        double width = getScene().getWidth();
        double height = getScene().getHeight();
        long imin = Math.max(0, (long) (-tx * Math.pow(2, deltaZ) / 256) - 1);
        long jmin = Math.max(0, (long) (-ty * Math.pow(2, deltaZ) / 256));
        long imax = Math.min(i_max, imin + (long) (width * Math.pow(2, deltaZ) / 256) + 3);
        long jmax = Math.min(j_max, jmin + (long) (height * Math.pow(2, deltaZ) / 256) + 3);
        if (debug) {
            System.out.println("zoom = " + nearestZoom + ", active = " + activeZoom +", tx = "+tx+ ", loadtiles, check i-range: " + imin + ", " + imax + " and j-range: " + jmin + ", " + jmax);
        }
        for (long i = imin; i < imax; i++) {
            for (long j = jmin; j < jmax; j++) {
                Long key = i * i_max + j;
                // LongTuple it = new LongTuple(i,j);
                SoftReference<MapTile> ref = tiles[nearestZoom].get(key);
                if ((ref == null) || (ref.get() == null)) {
                    if (ref != null) {
                        System.out.println("RECLAIMED: z=" + nearestZoom + ",i=" + i + ",j=" + j);
                    }
                    MapTile tile = new MapTile(this, nearestZoom, i, j);
                    tiles[nearestZoom].put(key, new SoftReference<>(tile));
                    MapTile covering = tile.getCoveringTile();
                    if (covering != null) {
                        if (!getChildren().contains(covering)) {
                            getChildren().add(covering);
                        }
                    }

                    getChildren().add(tile);
                } else {
                    MapTile tile = ref.get();
                    if (!getChildren().contains(tile)) {
                        getChildren().add(tile);
                    }
                }
            }
        }
        calculateCenterCoords();
        cleanupTiles();
    }

    /**
     * Find the "nearest" lower-zoom tile that covers a specific tile. This is
     * used to find out what tile we have to show while a new tile is still
     * loading
     *
     * @param zoom
     * @param i
     * @param j
     * @return the lower-zoom tile which covers the specified tile
     */
    protected MapTile findCovering(int zoom, long i, long j) {
        while (zoom > 0) {
            zoom--;
            i = i / 2;
            j = j / 2;
            MapTile candidate = findTile(zoom, i, j);
            if ((candidate != null) && (!candidate.loading())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Return a specific tile
     *
     * @param zoom the zoomlevel
     * @param i the x-index
     * @param j the y-index
     * @return the tile, only if it is still in the cache
     */
    private MapTile findTile(int zoom, long i, long j) {
        Long key = i * (1 << zoom) + j;
        SoftReference<MapTile> exists = tiles[zoom].get(key);
        return (exists == null) ? null : exists.get();
    }

    private void cleanupTiles() {
        if (debug) {
            System.out.println("START CLEANUP");
        }
        double zp = zoomProperty.get();
        List<MapTile> toRemove = new LinkedList<>();
        Parent parent = this.getParent();
        ObservableList<Node> children = this.getChildren();
        for (Node child : children) {
            if (child instanceof MapTile) {
                MapTile tile = (MapTile) child;
                boolean intersects = tile.getBoundsInParent().intersects(area.getBoundsInParent());
                if (debug) {
                    System.out.println("evaluate tile " + tile + ", is = " + intersects + ", tzoom = " + tile.getZoomLevel());
                }
                if (!intersects) {
                    if (debug) System.out.println("not shown");
                    boolean loading = tile.loading();
                    //    System.out.println("Reap "+tile+" loading? "+loading);
                    if (!loading) {
                        toRemove.add(tile);
                    }
                } else if (tile.getZoomLevel() > ceil(zp)) {
                    if (debug) System.out.println("too detailed");
                    toRemove.add(tile);
                } else if ((tile.getZoomLevel() < floor(zp + TIPPING)) && (!tile.isCovering()) && (!(ceil(zp) >= MAX_ZOOM))) {
                    if (debug) System.out.println("not enough detailed");
                    toRemove.add(tile);
                }
            }
        }

        getChildren().removeAll(toRemove);

        if (debug) {
            System.out.println("DONE CLEANUP");
        }
    }

    
    /** Reload all tiles on a change in provider. There could be a more 
     * efficient way?
     */
    private void reloadTiles() {
        
        System.out.println("TileType was changed, reloading tiles.");
        
        clearTiles();
        
        loadTiles();
        
    }
    
    private void clearTiles() {
        
        List<Node> toRemove = new ArrayList<>();
        ObservableList<Node> children = this.getChildren();
        for (Node child : children) {
            if (child instanceof MapTile) {
                toRemove.add(child);
            }
        }
        getChildren().removeAll(children);
        
        for (int i = 0; i < tiles.length; i++) {
            tiles[i].clear();
        }
        
    }

    @Override
    public Node getView() {
        return this;
    }
    
    public void install() {
        
        area.translateXProperty().bind(translateXProperty().multiply(-1));
        area.translateYProperty().bind(translateYProperty().multiply(-1));
        if (sceneListener == null) {
            sceneListener = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    if (getScene() != null) {
                        area.widthProperty().bind(getScene().widthProperty().add(20));
                        area.heightProperty().bind(getScene().heightProperty().add(20));
                    }
                    if (abortedTileLoad) {
                        abortedTileLoad = false;
                        setCenter(lat, lon);
                    }
                }
            };
        }
        this.sceneProperty().addListener(sceneListener);
        
    }
    
    @Override
    public void uninstall() {
        this.sceneProperty().removeListener(sceneListener);
        area.translateXProperty().unbind();
        area.translateYProperty().unbind();
        area.widthProperty().unbind();
        area.heightProperty().unbind();
        clearTiles();
    }
    
}
