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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.comino.openmapfx.ext;

import java.util.ArrayList;
import java.util.List;

import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.MapLayer;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Geoff Capper
 */
public class CanvasLayer extends AnchorPane implements MapLayer {

    private ResizableCanvas canvas;

    private List<CanvasLayerPaintListener> paintListener;

    public CanvasLayer() {

    	paintListener = new ArrayList<CanvasLayerPaintListener>();

        canvas = new ResizableCanvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
        getChildren().add(canvas);

    }

    public void addPaintListener(CanvasLayerPaintListener listener) {
    	paintListener.add(listener);
    }

    public void redraw(boolean refresh) {
    	canvas.draw(refresh);
    }

    @Override
    public Node getView() {
        return this;
    }

    @Override
    public void gotLayeredMap(LayeredMap map) {
        this.minWidthProperty().bind(map.widthProperty());
        this.minHeightProperty().bind(map.heightProperty());
        this.maxWidthProperty().bind(map.widthProperty());
        this.maxHeightProperty().bind(map.heightProperty());
    }

    class ResizableCanvas extends Canvas {

        public ResizableCanvas() {
            widthProperty().addListener(evt -> draw(true));
            heightProperty().addListener(evt -> draw(true));
        }

        public void draw(boolean refresh) {

        	GraphicsContext gc = getGraphicsContext2D();
        	double width = getWidth();
            double height = getHeight();

            if(refresh)
              gc.clearRect(0, 0, width, height);

        	for(CanvasLayerPaintListener listener : paintListener)
        		listener.redraw(gc, width, height, refresh);
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double prefWidth(double height) {
            return getWidth();
        }

        @Override
        public double prefHeight(double width) {
            return getHeight();
        }
    }

}
