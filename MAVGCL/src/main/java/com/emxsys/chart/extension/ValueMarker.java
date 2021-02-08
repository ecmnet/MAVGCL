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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;

/**
 * A JavaFX Chart Extension class that highlights a value on an XYChart's X
 * (domain) or Y (range) axis. The value is highlighted with a line through the
 * value with an optional label. ValueMarkers are added to an XYMarkers object.
 * For example:
 * <pre>{@code
 * EnhancedScatterChart chart = new EnhancedScatterChart(xAxis, yAxis, dataset);
 * double maxX;
 * double maxY;
 * // TODO: compute maxX, and maxY
 * chart.getMarkers().addDomainMarker(new ValueMarker(maxX, String.format("Max: %1$.1f", maxX), Pos.BOTTOM_RIGHT));
 * chart.getMarkers().addRangeMarker(new ValueMarker(maxY, String.format("Max: %1$.1f", maxY), Pos.TOP_LEFT));
 * }</pre>
 *
 * @author Bruce Schubert
 */

public class ValueMarker {

    private final DoubleProperty value = new SimpleDoubleProperty();
    private final Group group = new Group();
    private final Line line = new Line();
    private final Label label = new Label();
    private Pos textAnchor = Pos.TOP_LEFT;

    private enum MarkerType {
        DOMAIN, RANGE
    };
    private MarkerType markerType = MarkerType.RANGE;

    /**
     * Constructs a marker line drawn at the given value.
     *
     * @param value The value to be highlighted.
     */
    public ValueMarker(double value) {
        this(value, null);
    }

    /**
     * Constructs a marker line and label drawn at the given value. The label
     * will be anchored at its top left corner.
     *
     * @param value The value to be highlighted.
     * @param text The text to display on the line.
     */
    public ValueMarker(double value, String text) {
        this(value, text, Pos.TOP_LEFT);
    }

    /**
     * Constructs a marker line and label drawn at the given value .
     *
     * @param value The value to be highlighted.
     * @param text The label text to be displayed on the line.
     * @param textAnchor The anchor point defining where the label should be
     * placed relative to the value. Valid values include: TOP_LEFT, TOP_RIGHT,
     * TOP_CENTER, CENTER_LEFT, CENTER_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT,
     * BOTTOM_CENTER
     */
    public ValueMarker(double value, String text, Pos textAnchor) {
        this.value.set(value);
        this.label.setText(text);
        this.textAnchor = textAnchor;
        this.line.getStyleClass().add("chart-marker-line");
        this.label.getStyleClass().add("chart-marker-label");
        this.label.applyCss();

        // Note: these listeners are essential for the correct layout of the label.
        // During the first layout pass, the width and height of the label are not
        // set which causes some of the size based placements to be incorrectly set. 
        this.label.widthProperty().addListener((observable) -> layoutText());
        this.label.heightProperty().addListener((observable) -> layoutText());

        this.group.getChildren().addAll(line, label);
    }

    /**
     * Gets the marker value.
     *
     * @return The current value.
     */
    public double getValue() {
        return value.get();
    }

    /**
     * Sets the marker value and hence placement on the chart.
     *
     * @param value The new value.
     */
    public void setValue(double value) {
        this.value.set(value);
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    /**
     * Gets the optional label text.
     *
     * @return The current label text.
     */
    public String getLabel() {
        return label.getText();
    }

    /**
     * Sets the optional label text.
     *
     * @param text The new text value.
     */
    public void setLabel(String text) {
        label.setText(text);
    }

    /**
     * Gets the current node.
     *
     * @return A Group containing a line and label.
     */
    public Group getNode() {
        return group;
    }

    /**
     * Gets the anchor that defines where the text will be placed.
     *
     * @return The current text placement anchor.
     */
    public Pos getTextAnchor() {
        return textAnchor;
    }

    /**
     * Sets the anchor that defines where the text will be placed.
     *
     * @param textAnchor The new text placement anchor.
     */
    public void setTextAnchor(Pos textAnchor) {
        this.textAnchor = textAnchor;
    }

    /**
     * Lays out this marker on the X (domain) axis.
     *
     * @param xAxis The chart's X axis.
     * @param yAxis The chart's Y axis.
     */
    public void layoutDomainMarker(ValueAxis xAxis, ValueAxis yAxis) {
        // Determine the line height
        double lower = yAxis.getLowerBound();
        Number lowerY = yAxis.toRealValue(lower);
        double upper = yAxis.getUpperBound();
        Number upperY = yAxis.toRealValue(upper);
        // Establish the placement of the line
        line.setStartY(yAxis.getDisplayPosition(lowerY));
        line.setEndY(yAxis.getDisplayPosition(upperY));
        line.setStartX(xAxis.getDisplayPosition(getValue()));
        line.setEndX(line.getStartX());
        // Layout the text base on the line
        markerType = MarkerType.DOMAIN;
        layoutText();
    }

    /**
     * Lays out this marker on the Y (range) axis.
     *
     * @param xAxis The chart's X axis.
     * @param yAxis The chart's Y axis.
     */
    public void layoutRangeMarker(ValueAxis xAxis, ValueAxis yAxis) {
        // Determine the line width
        double lower = xAxis.getLowerBound();
        Number lowerX = xAxis.toRealValue(lower);
        double upper = xAxis.getUpperBound();
        Number upperX = xAxis.toRealValue(upper);
        // Establish the line placement
        line.setStartX(xAxis.getDisplayPosition(lowerX));
        line.setEndX(xAxis.getDisplayPosition(upperX));
        line.setStartY(yAxis.getDisplayPosition(getValue()));
        line.setEndY(line.getStartY());
        // Layout the text based on the line placement
        markerType = MarkerType.RANGE;
        layoutText();
    }

    protected void layoutText() {
        // Determine X coordinate
        switch (textAnchor) {
            case TOP_CENTER:
            case CENTER:
            case BOTTOM_CENTER:
                label.setLayoutX(line.getStartX() + ((line.getEndX() - line.getStartX()) / 2) - (label.getWidth() / 2));
                break;
            case TOP_LEFT:
            case CENTER_LEFT:
            case BOTTOM_LEFT:
                if (markerType == MarkerType.RANGE) {
                    // Place the range marker text at the left side of the chart
                    label.setLayoutX(line.getStartX());
                } else {
                    // Place the domain marker text on the left side of the line
                    label.setLayoutX(line.getStartX() - label.getWidth());
                }
                break;
            case TOP_RIGHT:
            case CENTER_RIGHT:
            case BOTTOM_RIGHT:
                if (markerType == MarkerType.RANGE) {
                    // Place the range marker text at the right side of the chart
                    label.setLayoutX(line.getEndX() - label.getWidth());
                } else {
                    // Place the domain marker text on the right side of the line
                    label.setLayoutX(line.getStartX());
                }
                break;
            default:
                throw new IllegalStateException(getClass().getSimpleName() + ": " + textAnchor.name() + " is not supported.");
        }
        // Determine Y coordinate
        switch (textAnchor) {
            case CENTER:
            case CENTER_LEFT:
            case CENTER_RIGHT:
                label.setLayoutY(line.getStartY() + ((line.getEndY() - line.getStartY()) / 2) - (label.getHeight() / 2));
                break;
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                if (markerType == MarkerType.RANGE) {
                    // Place the range marker text above the line
                    label.setLayoutY(line.getStartY() - label.getHeight());
                } else {
                    // Place the domain marker text at the top of the chart
                    label.setLayoutY(line.getEndY());
                }
                break;
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                if (markerType == MarkerType.RANGE) {
                    // Place the range marker text below the line
                    label.setLayoutY(line.getEndY());
                } else {
                    // Place the range marker text at the bottom of the chart
                    label.setLayoutY(line.getStartY() - label.getHeight());
                }
                break;
            default:
                throw new IllegalStateException(getClass().getSimpleName() + ": " + textAnchor.name() + " is not supported.");

        }
    }

}
