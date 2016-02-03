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

import static java.lang.Math.floor;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

/**
 *
 * @author johan
 */
public class MapTile extends Region {

    //static final String TILESERVER = "http://tile.openstreetmap.org/";//
    //static final String TILESERVER = "http://otile1.mqcdn.com/tiles/1.0.0/map/";
    private final MapArea mapArea;
    private final int myZoom;
    private final long i, j;
    private final List<MapTile> covering = new LinkedList<>();

    private boolean debug = false;

    private Label debugLabel = new Label();
    static AtomicInteger createcnt = new AtomicInteger(0);

    /**
     * In most cases, a tile will be shown scaled. The value for the scale
     * factor depends on the active zoom and the tile-specific myZoom
     */
    final Scale scale = new Scale();

    private static final Image temporaryImage;
    static {
        WritableImage writableImage = new WritableImage(256, 256);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                writableImage.getPixelWriter().setColor(x, y, Color.rgb(128, 128, 128));
            }
        }
        temporaryImage = writableImage;
    }

    private final InvalidationListener zl;
    private final InvalidationListener iwpl;
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final MapTile parentTile;
    private final Worker<Image> imageWorker;

    /**
     * Create a specific MapTile for a zoomlevel, x-index and y-index
     *
     * @param mapArea the mapArea that will hold this tile. We need a reference
     * to the MapArea as it contains the active zoom property
     * @param zoom the zoom level for this tile
     * @param i the x-index (between 0 and 2^zoom)
     * @param j the y-index (between 0 and 2^zoom)
     */
    public MapTile(final MapArea mapArea, final int zoom, final long i, final long j) {
        int ig = createcnt.incrementAndGet();
        if (debug) System.out.println("Create tile #" + ig);
        this.mapArea = mapArea;
        this.myZoom = zoom;
        this.i = i;
        this.j = j;
        scale.setPivotX(0);
        scale.setPivotY(0);
        getTransforms().add(scale);
        //String url = TILESERVER + zoom + "/" + i + "/" + j + ".png";
//        InputStream is = mapArea.tileTypeProperty().get().getInputStream(zoom, i, j);// , ig, ig).getBaseURL() + zoom + "/" + i + "/" + j + ".png";
//        if (debug) {
//            System.out.println("Creating maptile " + this + " with is = " + is);
//        }

        ImageView iv = new ImageView(temporaryImage);
        if (debug) debugLabel.setText("[" + zoom + "-" + i + "-" + j + "]");
        getChildren().addAll(iv, debugLabel);

        imageWorker = mapArea.tileTypeProperty().get().getImage(zoom, i, j);
        loading.bind(imageWorker.progressProperty().lessThan(1.));
        imageWorker.stateProperty().addListener((obs, ov, nv) -> {
            if (nv.equals(Worker.State.SUCCEEDED)) {
                iv.setImage(imageWorker.getValue());
            }
        });

        parentTile = mapArea.findCovering(zoom, i, j);
        if (parentTile != null) {
            if (debug) System.out.println("[JVDBG] ASK " + parentTile + " to cover for " + this);

            parentTile.addCovering(this);
        }

        iwpl = createImageWorkerProgressListener();
        imageWorker.progressProperty().addListener(new WeakInvalidationListener(iwpl));
        if (imageWorker.getProgress() >= 1) {
            if (debug) System.out.println("[JVDBG] ASK " + parentTile + " to NOWFORGET for " + this);
            if (parentTile != null) {
                parentTile.removeCovering(this);
            }
        }
        zl = recalculate();

        mapArea.zoomProperty().addListener(new WeakInvalidationListener(zl));
        mapArea.translateXProperty().addListener(new WeakInvalidationListener(zl));
        mapArea.translateYProperty().addListener(new WeakInvalidationListener(zl));
        calculatePosition();
    }

    /**
     * Return the zoomLevel of this tile. This can not be changed, it is a fixed
     * property of the tile.
     *
     * @return the zoomLevel of this tile.
     */
    public int getZoomLevel() {
        return myZoom;
    }

    /**
     * Check if the image in this tile is still loading
     *
     * @return true in case the image is still loading, false in case the image
     * is loaded
     */
    public boolean loading() {
        return loading.get();
    }

    /**
     * Indicate that we are used to cover the loading tile. As soon as we are
     * covering for at least 1 tile, we are visible.
     *
     * @param me a (new) tile which image is still loading
     */
    public void addCovering(MapTile me) {
        covering.add(me);
        setVisible(true);
    }

    /**
     * Remove the supplied tile from the covering list, as its image has been
     * loaded.
     *
     * @param me
     */
    public void removeCovering(MapTile me) {
        covering.remove(me);
        calculatePosition();
    }

    /**
     * Return the tile that will cover us while loading
     *
     * @return the lower-level zoom tile that covers this tile.
     */
    public MapTile getCoveringTile() {
        return parentTile;
    }

    /**
     * Check if the current tile is covering more detailed tiles that are
     * currently being loaded.
     *
     * @return
     */
    public boolean isCovering() {
        return covering.size() > 0;
    }

    @Override
    public String toString() {
        return "Tile[" + myZoom + "]" + " " + i + ", " + j;
    }

    private InvalidationListener recalculate() {
        return o -> calculatePosition();
    }

    private InvalidationListener createImageWorkerProgressListener() {
        InvalidationListener answer = o -> {
            double progress = imageWorker.getProgress();
//            System.out.println("IPL, p = "+progress+" for "+this);
            if (progress >= 1.) {
                if (parentTile != null) {
                    if (debug) System.out.println("[JVDBG] ASK " + parentTile + " to FORGET cover for " + this);

                    parentTile.removeCovering(MapTile.this);
                }
            }
        };
        return answer;
    }

    private void calculatePosition() {
        double currentZoom = mapArea.zoomProperty().get();
        int visibleWindow = (int) floor(currentZoom + MapArea.TIPPING);
        if ((visibleWindow == myZoom) || isCovering() || ((visibleWindow >= MapArea.MAX_ZOOM) && (myZoom == MapArea.MAX_ZOOM - 1))) {
            this.setVisible(true);

        } else {
            this.setVisible(false);
        }
        if (debug) {
            System.out.println("visible tile " + this + "? " + this.isVisible() + (this.isVisible() ? " covering? " + isCovering() : ""));
            if (this.isVisible() && this.isCovering()) {
                System.out.println("covering for " + this.covering);
            }
        }
        double sf = Math.pow(2, currentZoom - myZoom);
        scale.setX(sf);
        scale.setY(sf);
        setTranslateX(256 * i * sf);
        setTranslateY(256 * j * sf);
    }

}
