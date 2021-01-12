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
 *     - Neither the name of Bruce Schubert, Emxsys nor the names of its 
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

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;


/**
 * Major grid line support for logarithmic charts.
 *
 * @author Bruce Schubert
 * @param <X>
 * @param <Y>
 */
public class MajorLogGridlines<X, Y> {
    
    private Group plotArea = null;
    
    // Major log grid lines added to XYChart
    private final Path majorHorzGridlines = new Path();
    private final Path majorVertGridlines = new Path();
    private final XYChart<X, Y> chart;


    /**
     * Constructs the 'major' logarithmic grid lines for an XYChart.
     *
     * @param chart The chart desiring major grid lines.
     * @param chartChildren The children returned by chart.getChartChildren().
     */
    public MajorLogGridlines(XYChart<X, Y> chart, ObservableList<Node> chartChildren) {
        this.chart = chart;
        
        majorHorzGridlines.getStyleClass().setAll("chart-major-horizontal-grid-lines");
        majorVertGridlines.getStyleClass().setAll("chart-major-vertical-grid-lines");

        // Find the gridlines contained in the plotArea of the XYChart.
        // The chart children include in the following order:
        //  0: plotBackground
        //  1: plotArea
        //  2: xAxis
        //  3: yAxis
        plotArea = (Group) chartChildren.get(1);
        
        // Monitor the plotArea's clip rectangle to detect changes that require a layout.
        // This is essential for drawing the gridlines during the scene's initial layout.
        Rectangle plotAreaClip = (Rectangle) plotArea.getClip();
        plotAreaClip.widthProperty().addListener((observable) -> layoutGridlines());
        plotAreaClip.heightProperty().addListener((observable) -> layoutGridlines());
        
        // Insert our custom background annotations before the normal grid liens (indices 2 and 3).
        // The normal gridlines will draw on top of the major grid lines.
        ObservableList<Node> plotAreaChildren = plotArea.getChildren();
        plotAreaChildren.add(2, majorHorzGridlines);
        plotAreaChildren.add(2, majorVertGridlines);

    }


    /**
     * Performs a layout of the grid lines.
     */
    public void layoutGridlines() {
        
        final Rectangle clip = (Rectangle) plotArea.getClip();
        final Axis<X> xa = chart.getXAxis();
        final Axis<Y> ya = chart.getYAxis();
        double top = clip.getY();
        double left = clip.getX();
        double xAxisWidth = xa.getWidth();
        double yAxisHeight = ya.getHeight();
        double epsilon = 0.0001;
        majorVertGridlines.getElements().clear();
        if (chart.getVerticalGridLinesVisible()) {
            if (xa instanceof LogarithmicAxis) {
                final ObservableList<Axis.TickMark<X>> tickMarks = xa.getTickMarks();
                for (int i = 0; i < tickMarks.size(); i++) {
                    Axis.TickMark<X> tick = tickMarks.get(i);
                    Double value = (Double) tick.getValue();
                    double log = ((LogarithmicAxis) xa).calculateLog(value);
                    double delta = Math.abs(log - Math.round(log));
                    if (delta < epsilon) {
                        double pixelOffset = (i == (tickMarks.size() - 1)) ? -0.5 : 0.5;
                        double x = xa.getDisplayPosition(tick.getValue());
                        //if ((x != xAxisZero || !isVerticalZeroLineVisible()) && x > 0 && x <= xAxisWidth) {
                        if (x > 0 && x <= xAxisWidth) {
                            majorVertGridlines.getElements().add(new MoveTo(left + x + pixelOffset, top));
                            majorVertGridlines.getElements().add(new LineTo(left + x + pixelOffset, top + yAxisHeight));
                        }
                    }
                }
            }
        }
        majorHorzGridlines.getElements().clear();
        if (chart.isHorizontalGridLinesVisible()) {
            if (ya instanceof LogarithmicAxis) {
                final ObservableList<Axis.TickMark<Y>> tickMarks = ya.getTickMarks();
                for (int i = 0; i < tickMarks.size(); i++) {
                    Axis.TickMark<Y> tick = tickMarks.get(i);
                    Double value = (Double) tick.getValue();
                    double log = ((LogarithmicAxis) xa).calculateLog(value);
                    if ((log % 1) < epsilon) {
                        double pixelOffset = (i == (tickMarks.size() - 1)) ? -0.5 : 0.5;
                        double y = ya.getDisplayPosition(tick.getValue());
                        if (y > 0 && y <= yAxisHeight) {
                            majorHorzGridlines.getElements().add(new MoveTo(left, top + y + pixelOffset));
                            majorHorzGridlines.getElements().add(new LineTo(left + xAxisWidth, top + y + pixelOffset));
                        }
                    }
                }
            }
        }
    }
    
}
