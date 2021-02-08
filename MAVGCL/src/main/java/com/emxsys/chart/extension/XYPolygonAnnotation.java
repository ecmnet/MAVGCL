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
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;


/**
 * The XYPolygonAnnotation class draws polygon annotations on the foreground or background of an
 * XYChart.
 *
 * @author Bruce Schubert
 */

public class XYPolygonAnnotation implements XYAnnotation {

    private final Polygon polygon = new Polygon();

    double[] xyValues;


    /**
     * Constructs a polygon annotation with specific stroke and colors that override CSS styles.
     *
     * @param xyValues
     * @param strokeWidth
     * @param outlinePaint
     * @param fillPaint
     */
    public XYPolygonAnnotation(double[] xyValues, Double strokeWidth,
        Paint outlinePaint, Paint fillPaint) {

        if (xyValues == null || xyValues.length == 0) {
            throw new IllegalArgumentException("xyValues cannot be null or empty.");
        }
        if (xyValues.length % 2 != 0) {
            throw new IllegalArgumentException("xyValues does not appear to contain an equal number of x and y values.");
        }
        this.xyValues = xyValues;

        polygon.getStyleClass().add("chart-annotation-polygon");

        polygon.setStrokeWidth(strokeWidth == null ? 0.0 : strokeWidth);
        polygon.setStroke(outlinePaint);
        polygon.setFill(fillPaint);
    }


    /**
     * Constructs a polygon annotation that uses CSS styles for stroke and colors.
     *
     * @param xyValues
     */
    public XYPolygonAnnotation(double[] xyValues) {

        if (xyValues == null || xyValues.length == 0) {
            throw new IllegalArgumentException("xyValues cannot be null or empty.");
        }
        if (xyValues.length % 2 != 0) {
            throw new IllegalArgumentException("xyValues does not appear to contain an equal number of x and y values.");
        }
        this.xyValues = xyValues;

        polygon.getStyleClass().add("chart-annotation-polygon");
    }


    /**
     * Gets the node representation.
     * @return A Polygon object.
     */
    @Override
    public Node getNode() {
        return polygon;
    }


    /**
     * Performs the layout for the polygon.
     *
     * @param xAxis
     * @param yAxis
     */
    @Override
    public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
        polygon.getPoints().clear();
        int i = 0;
        for (double xyValue : xyValues) {
            // Plot the values
            if (i % 2 == 0) {
                polygon.getPoints().add(xAxis.getDisplayPosition(xyValue));
            }
            else {
                polygon.getPoints().add(yAxis.getDisplayPosition(xyValue));
            }
            ++i;
        }
    }


    /**
     * Assigns a Tooltip to the polygon.
     *
     * @param text The tooltip text to be displayed.
     */
    public void setTooltipText(String text) {
        Tooltip.install(polygon, new Tooltip(text));
    }

}
