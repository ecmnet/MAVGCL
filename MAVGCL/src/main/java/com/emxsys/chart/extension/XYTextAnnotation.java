/*
 * Copyright (c) 2015, Bruce Schubert <bruce@emxsys.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Bruce Schubert,  nor the names of its 
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.emxsys.chart.extension;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;


/**
 * The XYTextAnnotation class draws text annotations on the foreground or background of an XYChart.
 *
 * @author Bruce Schubert
 */

public class XYTextAnnotation implements XYAnnotation {

    private final Label label = new Label();
    private double x;
    private double y;
    private double displayX;
    private double displayY;
    private Pos textAnchor = Pos.CENTER;


    /**
     * Constructs a text annotation centered on the given x,y.
     *
     * @param text
     * @param x
     * @param y
     */
    public XYTextAnnotation(String text, double x, double y) {
        this(text, x, y, null);
    }


    /**
     * Constructs a text annotation.
     *
     * @param text
     * @param x
     * @param y
     * @param textAnchor
     */
    public XYTextAnnotation(String text, double x, double y, Pos textAnchor) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + ": 'text' arg cannot be null or empty.");
        }
        this.label.setText(text);
        this.label.getStyleClass().add("chart-annotation-text");

        // Note: these listeners are essential for the correct layout of the label.
        // During the initial layout pass, the width and height of the label are not
        // set which causes some of the size based placements to be computed incorrectly. 
        this.label.widthProperty().addListener((observable) -> layoutText());
        this.label.heightProperty().addListener((observable) -> layoutText());

        this.x = x;
        this.y = y;
        this.displayX = x;  // initial dummy value, will be set by layoutAnnotation
        this.displayY = y;  // initail dummy value, will be set by layoutAnnotation

        if (textAnchor != null) {
            this.textAnchor = textAnchor;
        }
    }


    /**
     * Gets the node representation.
     *
     * @return A Label object.
     */
    @Override
    public Node getNode() {
        return label;
    }


    /**
     * Performs the layout for the label.
     *
     * @param xAxis
     * @param yAxis
     */
    @Override
    public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
        displayX = xAxis.getDisplayPosition(x);
        displayY = yAxis.getDisplayPosition(y);
        layoutText();
    }


    protected void layoutText() {
        // Note: initially, the label width and height are 0 so we have to recompute
        // the layout after the first rendering.  See the width and height property listeners.
        switch (textAnchor) {
            case TOP_CENTER:
            case CENTER:
            case BOTTOM_CENTER:
                label.setLayoutX(displayX - (label.getWidth() / 2));
                break;
            case TOP_LEFT:
            case CENTER_LEFT:
            case BOTTOM_LEFT:
                label.setLayoutX(displayX);
                break;
            case TOP_RIGHT:
            case CENTER_RIGHT:
            case BOTTOM_RIGHT:
                label.setLayoutX(displayX - label.getWidth());
                break;
            default:
                throw new IllegalStateException(getClass().getSimpleName() + ": " + textAnchor.name() + " is not supported.");
        }
        switch (textAnchor) {
            case CENTER:
            case CENTER_LEFT:
            case CENTER_RIGHT:
                label.setLayoutY(displayY - (label.getHeight() / 2));
                break;
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                label.setLayoutY(displayY);
                break;
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                label.setLayoutY(displayY - label.getHeight());
                break;
            default:
                throw new IllegalStateException(getClass().getSimpleName() + ": " + textAnchor.name() + " is not supported.");
        }
    }


    /**
     * Sets the text for the label.
     *
     * @param text The new text.
     */
    public void setText(String text) {
        this.label.setText(text);
    }


    /**
     * Gets the anchor point for the label.
     *
     * @return The current anchor position.
     */
    public Pos getTextAnchor() {
        return textAnchor;
    }


    /**
     * Sets the anchor point for the label, which establishes the part of the label that will be
     * anchored to the annotation's x,y.
     *
     * @param textAnchor The new anchor position.
     */
    public void setTextAnchor(Pos textAnchor) {
        this.textAnchor = textAnchor;
    }

}
