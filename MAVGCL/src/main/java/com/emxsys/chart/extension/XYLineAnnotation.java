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

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;


/**
 * The XYLineAnnotation class draws line annotations on the foreground or background of an XYChart.
 *
 * @author Bruce Schubert
 */

public class XYLineAnnotation implements XYAnnotation {

    private final Line line = new Line();
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;


    /**
     * Constructs a line annotation from the given X,Y values.
     *
     * @param x1 Starting X value.
     * @param y1 Starting Y value.
     * @param x2 Ending X value.
     * @param y2 Ending Y value.
     * @param strokeWidth
     * @param color
     */
    public XYLineAnnotation(double x1, double y1, double x2, double y2, Double strokeWidth,
        Paint color) {

        this.line.getStyleClass().add("chart-annotation-line");

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        this.line.setStrokeWidth(strokeWidth);
        this.line.setStroke(color);
    }


    /**
     * Constructs a line annotation drawn with CSS styles.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public XYLineAnnotation(double x1, double y1, double x2, double y2) {
        this.line.getStyleClass().add("chart-annotation-line");

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }


    /**
     * Gets the Node representation.
     *
     * @return A Line object.
     */
    @Override
    public Node getNode() {
        return line;
    }


    /**
     * Performs the layout of the line.
     *
     * @param xAxis
     * @param yAxis
     */
    @Override
    public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
        line.setStartX(xAxis.getDisplayPosition(x1));
        line.setStartY(yAxis.getDisplayPosition(y1));
        line.setEndX(xAxis.getDisplayPosition(x2));
        line.setEndY(yAxis.getDisplayPosition(y2));
    }

}
