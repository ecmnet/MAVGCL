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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * A MultiPositionLayer is an implementation of a MapLayer with support for
 * tracking multiple nodes. You must call {@link #addNode(javafx.scene.Node, double lat, double lon) addNode}
 * first for every node that should be tracked by this layer. Every Node is
 * uniquely identified by their {@link javafx.scene.Node#id}. If no id was set,
 * a random id will be used instead.
 *
 * @author joeri
 */
public class MultiPositionLayer extends Parent implements MapLayer {

    private final Map<String, Position> nodePositions = new HashMap<>();
    private double imageWidth, imageHeight;
    private LayeredMap layeredMap;

    @Override
    public Node getView() {
        return this;
    }

    /**
     * Adds a node to the layer. The id of the node is used internally to
     * uniquely identify it later when {@link #updatePosition(javafx.scene.Node, double lat, double lon) updatePosition}
     * is called. A random unique id will be set when no id was defined for the
     * node.
     * @param node the node to add
     * @param latitude the latitude coordinates of the initial position
     * @param longitude the longitude coordinates of the initial position
     */
    public void addNode(Node node, double latitude, double longitude) {
        if (node.getId() == null) {
            node.setId(UUID.randomUUID().toString());
        }

        nodePositions.put(node.getId(), new Position(latitude, longitude));
        getChildren().add(node);
    }

    /**
     * Removes the node from the layer.
     *
     * @param node the node to remove
     */
    public void removeNode(Node node) {
        nodePositions.remove(node.getId());
        getChildren().remove(node);
    }

    /**
     * Removes all nodes that were added to this layer.
     */
    public void removeAllNodes() {
        nodePositions.clear();
        getChildren().clear();
    }

    /**
     * Updates the position of the node.
     *
     * @param node the node to update
     * @param latitude the new latitude coordinates
     * @param longitude the new longitude coordinates
     */
    public void updatePosition(Node node, double latitude, double longitude) {
        nodePositions.put(node.getId(), new Position(latitude, longitude));
        refreshSingleLayer(node);
    }

    protected void refreshEntireLayer() {
        for (Node node : getChildren()) {
            refreshSingleLayer(node);
        }
    }

    protected void refreshSingleLayer(Node node) {
        Position nodePosition = nodePositions.get(node.getId());
        if (nodePosition != null) {
            Point2D cartPoint = this.layeredMap.getMapPoint(nodePosition.getLatitude(), nodePosition.getLongitude());
            if (cartPoint == null) {
                System.out.println("[JVDBG] Null cartpoint, probably no scene, dont show.");
            } else {
                node.setVisible(true);
                node.setTranslateX(cartPoint.getX() - imageWidth / 2);
                node.setTranslateY(cartPoint.getY() - imageHeight / 2);
            }
        } else {
            System.out.println("[JVDBG] Null nodePosition, probably updated a node position that was not added (node id: " + node.getId() + ".");
        }
    }

    @Override
    public void gotLayeredMap(LayeredMap map) {
        this.layeredMap = map;
        this.layeredMap.zoomProperty().addListener(e -> refreshEntireLayer());
        this.layeredMap.centerLatitudeProperty().addListener(e -> refreshEntireLayer());
        this.layeredMap.centerLongitudeProperty().addListener(e -> refreshEntireLayer());
        this.layeredMap.xShiftProperty().addListener(e -> refreshEntireLayer());
        this.layeredMap.yShiftProperty().addListener(e -> refreshEntireLayer());
        refreshEntireLayer();
    }
}
