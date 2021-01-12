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

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.shape.Path;

/**
 * A axis class that plots a logarithmic range of numbers with major tick marks
 * every "tickUnit".
 *
 * @author Bruce Schubert
 */
public class LogarithmicAxis extends NumericAxis {

    /**
     * The time of animation in ms
     */
    private static final double ANIMATION_TIME = 2000;
    private final Timeline lowerRangeTimeline = new Timeline();
    private final Timeline upperRangeTimeline = new Timeline();
    private final DoubleProperty logUpperBound = new SimpleDoubleProperty();
    private final DoubleProperty logLowerBound = new SimpleDoubleProperty();

    /**
     * The logarithm base.
     */
    private double base = 10.0;

    /**
     * Precomputed base logarithm
     */
    private double baseLog = Math.log(base);

    /**
     * Constructs an auto-ranging numeric axis.
     */
    public LogarithmicAxis() {
        super();
    }

    /**
     * Constructs an non-auto-ranging numeric axis.
     * @param lowerBound
     * @param upperBound
     */
    public LogarithmicAxis(double lowerBound, double upperBound) {
        this(lowerBound, upperBound, 1);
    }

    /**
     * Constructs a non-auto-ranging numeric with the given upper bound, lower
     * bound and tick unit.
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public LogarithmicAxis(double lowerBound, double upperBound, double tickUnit) {
        this(null, lowerBound, upperBound, 1);
    }

    /**
     * Constructs a non-auto-ranging NumberAxis with the given lablel, upper
     * bound, lower bound.
     *
     * @param axisLabel The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     */
    public LogarithmicAxis(String axisLabel, double lowerBound, double upperBound) {
        this(axisLabel, lowerBound, upperBound, 1);

    }
    /**
     * Constructs a non-auto-ranging NumberAxis with the given lablel, upper
     * bound, lower bound and tick unit.
     *
     * @param axisLabel The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public LogarithmicAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(axisLabel, lowerBound, upperBound, tickUnit);
        validateBounds(lowerBound, upperBound);
        bindLogBoundsToDefaultBounds();

    }

    /**
     * Binds our logarithmic bounds with the super class bounds. 
     */
    private void bindLogBoundsToDefaultBounds() {
        // TODO: consider other than the base 10 logarithmic scale.
        logLowerBound.bind(new DoubleBinding() {
            {
                super.bind(lowerBoundProperty());
            }

            @Override
            protected double computeValue() {
                return calculateLog(getLowerBound());
            }
        });
        logUpperBound.bind(new DoubleBinding() {
            {
                super.bind(upperBoundProperty());
            }

            @Override
            protected double computeValue() {
                return calculateLog(getUpperBound());
            }
        });
    }

    private void validateBounds(double lowerBound, double upperBound) {
        if (lowerBound >= upperBound) {
            throw new IllegalArgumentException(
                    "Invalid range: the lowerBound value (" + lowerBound + ") must be less that upperBound value (" + upperBound + ").");
        }
    }

    /**
     * Calculates a list of all the data values for each tick mark in range.
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given
     * length
     */
    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        final Object[] rangeProps = (Object[]) range;
        final double lowerBound = (Double) rangeProps[0];
        final double upperBound = (Double) rangeProps[1];
        final double tickUnit = (Double) rangeProps[2];
        List<Number> tickValues = new ArrayList<>();

        int minorTickCount = this.getTickUnitImpl().getMinorTickCount();

        double start = Math.floor(calculateLog(getLowerBound()));
        double end = Math.ceil(calculateLog(getUpperBound()));
        double current = start;
        boolean hasTicks = (this.getTickUnitImpl().getSize() > 0.0) && !Double.isInfinite(start);
        while (hasTicks && current <= end) {
            double v = calculateValue(current);
            tickValues.add(v);

            double next = Math.pow(this.base, current + this.getTickUnitImpl().getSize());
            for (int i = 1; i < minorTickCount; i++) {
                double minorV = v + i * ((next - v) / minorTickCount);
                if (minorV >= lowerBound && minorV <= upperBound) {
                    tickValues.add(minorV);
                }
            }
            current = current + this.getTickUnitImpl().getSize();
        }

        return tickValues;
    }

    // This logic is modeled after JFreeChart LogAxis class
    @Override
    protected List<Number> calculateMinorTickMarks() {
        final Object[] range = (Object[]) getRange();
        List<Number> minorTickMarksPositions = new ArrayList<>();
        
// JFree: This logic is modeled after JFreeChart LogAxis class:
//        
//        double lowerBound = (double) range[0];
//        double upperBound = (double) range[1];
//
//        int minorTickCount = this.tickUnit.getMinorTickCount();
//        double start = Math.floor(calculateLog(getLowerBound()));
//        double end = Math.ceil(calculateLog(getUpperBound()));
//        
//        double current = start;
//        boolean hasTicks = (this.tickUnit.getSize() > 0.0) && !Double.isInfinite(start);
//        while (hasTicks && current <= end) {
//            double v = calculateValue(current);
//
//            double next = Math.pow(this.base, current + this.tickUnit.getSize());
//            for (int i = 1; i < minorTickCount; i++) {
//                double minorV = v + i * ((next - v) / minorTickCount);
//                if (minorV >= lowerBound && minorV <= upperBound) {
//                    minorTickMarksPositions.add(minorV);
//                }
//            }
//            current = current + this.tickUnit.getSize();
//        }
        return minorTickMarksPositions;
    }

    @Override
    public Number getValueForDisplay(double displayPosition) {
        double delta = logUpperBound.get() - logLowerBound.get();
        if (getSide().isVertical()) {
            return calculateValue((((displayPosition - getHeight()) / -getHeight()) * delta) + logLowerBound.get());
        } else {
            return calculateValue((((displayPosition / getWidth()) * delta) + logLowerBound.get()));
        }
    }

    @Override
    public double getDisplayPosition(Number value) {
        double delta = logUpperBound.get() - logLowerBound.get();
        double deltaV = calculateLog(value.doubleValue()) - logLowerBound.get();
        if (getSide().isVertical()) {
            return (1. - ((deltaV) / delta)) * getHeight();
        } else {
            return ((deltaV) / delta) * getWidth();
        }
    }


    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        ObservableList<Axis.TickMark<Number>> tickMarks = getTickMarks();
        Path tickMarkPath = null;

        ObservableList<Node> children = super.getChildren();
        for (Node child : children) {
            // In Axis.java: tickMarkPath.getStyleClass().add("axis-tick-mark");
            if (child instanceof Path && child.getStyleClass().contains("axis-tick-mark")) {
                tickMarkPath = (Path) child;
                break;
            }
        }

    }

    public double getBase() {
        return this.base;
    }

    public void setBase(double base) {
        if (base <= 1.0) {
            throw new IllegalArgumentException("'base' must be > 1.0.");
        }
        this.base = base;
        this.baseLog = Math.log(base);

        // TODO: notify chart that axis has changed
        // TODO: this should be a property!
    }

    public double calculateLog(double value) {
        return Math.log(value) / this.baseLog;
    }

    public double calculateValue(double log) {
        return Math.pow(this.base, log);
    }

}
