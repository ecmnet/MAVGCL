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

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * A PositionLayer is used to show an Image or a Node at the position that is
 * maintained by this layer. Once a PositionLayer is created, it should be added
 * to a LayeredMap. This LayeredMap is further responsible for doing the
 * required calculations.
 *
 * @see LayeredMap#getLayers()
 * @author johan
 */
public class PositionLayer extends Parent implements MapLayer {

    private double lat;
    private double lon;
    private final Node icon;
    private final double iconTranslateX, iconTranslateY;
    private LayeredMap layeredMap;

    /**
     * Create a PositionLayer with an image that will be shown on the map at the
     * position of this PositionLayer. The center of the image will be used for
     * the position of the PositionLayer. Once created, a PositionLayer should
     * be added to a LayeredMap. The LayeredMap is responsible for doing the
     * required calculations.
     *
     * @param image the image that will be shown at the position of this layer
     */
    public PositionLayer(Image image) {
        this(new ImageView(image), image.getWidth() / -2.0, image.getHeight() / -2.0);
    }

    /**
     * Create a PositionLayer with a specific Node that serves as the icon for
     * the location. The node can be styled using css.
     *
     * @param icon the icon Node.
     */
    public PositionLayer(Node icon) {
        this(icon, 0.0, 0.0);
    }

    /**
     * Create a PositionLayer with a specific Node that serves as the icon for
     * the location. The node can be styled using css. When the position
     * changes, the provided translateX and translateY will be added to the new
     * x and y translations.
     *
     * @param icon the icon Node.
     * @param translateX extra x translation to add to the icon
     * @param translateY extra y translation to add to the icon
     */
    public PositionLayer(Node icon, double translateX, double translateY) {
        this.icon = icon;
        this.icon.setVisible(false);
        this.iconTranslateX = translateX;
        this.iconTranslateY = translateY;
        getChildren().add(icon);
    }

    @Override
    public Node getView() {
        return this;
    }

    public void updatePosition(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        refreshLayer();
    }

    protected void refreshLayer() {
        Point2D cartPoint = this.layeredMap.getMapPoint(lat, lon);
        if (cartPoint == null) {
            System.out.println("[JVDBG] Null cartpoint, probably no scene, dont show.");
            return;
        }
        icon.setVisible(true);
        icon.setTranslateX(cartPoint.getX() + iconTranslateX);
        icon.setTranslateY(cartPoint.getY() + iconTranslateY);
    }

    @Override
    public void gotLayeredMap(LayeredMap map) {
        this.layeredMap = map;
        this.layeredMap.zoomProperty().addListener(e -> refreshLayer());
        this.layeredMap.centerLatitudeProperty().addListener(e -> refreshLayer());
        this.layeredMap.centerLongitudeProperty().addListener(e -> refreshLayer());
        this.layeredMap.xShiftProperty().addListener(e -> refreshLayer());
        this.layeredMap.yShiftProperty().addListener(e -> refreshLayer());
        refreshLayer();
    }

}
