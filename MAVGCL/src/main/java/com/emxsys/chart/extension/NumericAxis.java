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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.chart.ValueAxis;
import javafx.util.Duration;
import javafx.util.StringConverter;


/**
 * Extensible (non-final) NumericAxis compatible with a JFree-style TickUnitSource.
 *
 * @author Bruce Schubert
 */
public class NumericAxis extends ValueAxis<Number> {

    private Object currentAnimationID;
//    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);
    private TickUnitSource standardTickUnits;
    
    private NumberTickUnit tickUnit;


    /**
     * Constructs an auto-ranging number axis.
     */
    public NumericAxis() {
        this.tickUnit = new NumberTickUnit();
    }


    /**
     * Constructs a non-auto-ranging number access with the given upper bound, lower bound,
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     */
    public NumericAxis(double lowerBound, double upperBound) {
        super(lowerBound, upperBound);
        this.tickUnit = new NumberTickUnit();
    }


    /**
     * Constructs a non-auto-ranging NumberAxis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public NumericAxis(double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        this.tickUnit = new NumberTickUnit(tickUnit);
    }


    /**
     * Create a non-auto-ranging NumberAxis with the given upper bound, lower bound and tick unit
     *
     * @param axisLabel The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public NumericAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        this.tickUnit = new NumberTickUnit(tickUnit);
        setLabel(axisLabel);
    }


    public final double getTickUnit() {
        return tickUnit.getSize();
    }

    
    protected NumberTickUnit getTickUnitImpl() {
        return tickUnit;
    }

    public final void setNumberFormatter(NumberFormat formatter) {
        this.tickUnit.setNumberFormatter(formatter);
    }


    public final void setTickUnit(double value) {
        throw new UnsupportedOperationException("setTickUnit");
    }

    /**
     * When true zero is always included in the visible range. This only has effect if auto-ranging
     * is on.
     */
    private BooleanProperty forceZeroInRange = new BooleanPropertyBase(true) {
        @Override
        protected void invalidated() {
            // This will effect layout if we are auto ranging
            if (isAutoRanging()) {
                requestAxisLayout();
                invalidateRange();
            }
        }


        @Override
        public Object getBean() {
            return this;
        }


        @Override
        public String getName() {
            return "forceZeroInRange";
        }
    };


    public final boolean isForceZeroInRange() {
        return forceZeroInRange.getValue();
    }


    public final void setForceZeroInRange(boolean value) {
        forceZeroInRange.setValue(value);
    }


    public final BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    private Orientation effectiveOrientation;


    final Side getEffectiveSide() {
        final Side side = getSide();
        if (side == null || (side.isVertical() && effectiveOrientation == Orientation.HORIZONTAL)
            || side.isHorizontal() && effectiveOrientation == Orientation.VERTICAL) {
            // Means side == null && effectiveOrientation == null produces Side.BOTTOM
            return effectiveOrientation == Orientation.VERTICAL ? Side.LEFT : Side.BOTTOM;
        }
        return side;
    }


    final void setEffectiveOrientation(Orientation orientation) {
        effectiveOrientation = orientation;
    }


    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for
     * the tick marks
     *
     * @param value tick mark value
     * @param rotation The text rotation
     * @param numFormatter The number formatter
     * @return size of tick mark label for given value
     */
    private Dimension2D measureTickMarkSize(Number value, double rotation, String numFormatter) {
        String labelText;
        StringConverter<Number> formatter = getTickLabelFormatter();
        //if (formatter == null) formatter = defaultFormatter;
//        if(formatter instanceof NumberAxis.DefaultFormatter) {
//            labelText = ((NumberAxis.DefaultFormatter)formatter).toString(value, numFormatter);
//        } else {
//            labelText = formatter.toString(value);
//        }
        labelText = formatter.toString(value);
        return measureTickMarkLabelSize(labelText, rotation);
    }


    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param length The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    @Override
    protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        final Side side = getEffectiveSide();
        // check if we need to force zero into range
        if (isForceZeroInRange()) {
            if (maxValue < 0) {
                maxValue = 0;
            }
            else if (minValue > 0) {
                minValue = 0;
            }
        }
        final double range = maxValue - minValue;
        // pad min and max by 2%, checking if the range is zero
        final double paddedRange = (range == 0) ? 2 : Math.abs(range) * 1.02;
        final double padding = (paddedRange - range) / 2;
        // if min and max are not zero then add padding to them
        double paddedMin = minValue - padding;
        double paddedMax = maxValue + padding;
        // check padding has not pushed min or max over zero line
        if ((paddedMin < 0 && minValue >= 0) || (paddedMin > 0 && minValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMin = 0;
        }
        if ((paddedMax < 0 && maxValue >= 0) || (paddedMax > 0 && maxValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMax = 0;
        }
        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int) Math.floor(length / labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        double tickUnit = paddedRange / (double) numOfTickMarks;
        // search for the best tick unit that fits
        double tickUnitRounded = 0;
        double minRounded = 0;
        double maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        String formatter = "0.00000000";

        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        while (reqLength > length || count > 20) {
            int exp = (int) Math.floor(Math.log10(tickUnit));
            final double mant = tickUnit / Math.pow(10, exp);
            double ratio = mant;
            if (mant > 5d) {
                exp++;
                ratio = 1;
            }
            else if (mant > 1d) {
                ratio = mant > 2.5 ? 5 : 2.5;
            }
            if (exp > 1) {
                formatter = "#,##0";
            }
            else if (exp == 1) {
                formatter = "0";
            }
            else {
                final boolean ratioHasFrac = Math.rint(ratio) != ratio;
                final StringBuilder formatterB = new StringBuilder("0");
                int n = ratioHasFrac ? Math.abs(exp) + 1 : Math.abs(exp);
                if (n > 0) {
                    formatterB.append(".");
                }
                for (int i = 0; i < n; ++i) {
                    formatterB.append("0");
                }
                formatter = formatterB.toString();

            }
            tickUnitRounded = ratio * Math.pow(10, exp);
            // move min and max to nearest tick mark
            minRounded = Math.floor(paddedMin / tickUnitRounded) * tickUnitRounded;
            maxRounded = Math.ceil(paddedMax / tickUnitRounded) * tickUnitRounded;
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            double last = 0;
            count = 0;
            for (double major = minRounded; major <= maxRounded; major += tickUnitRounded, count++) {
                double size = side.isVertical() ? measureTickMarkSize(major, getTickLabelRotation(), formatter).getHeight()
                    : measureTickMarkSize(major, getTickLabelRotation(), formatter).getWidth();
                if (major == minRounded) { // first
                    last = size / 2;
                }
                else {
                    maxReqTickGap = Math.max(maxReqTickGap, last + 6 + (size / 2));
                }
            }
            reqLength = (count - 1) * maxReqTickGap;
            tickUnit = tickUnitRounded;

            // fix for RT-35600 where a massive tick unit was being selected
            // unnecessarily. There is probably a better solution, but this works
            // well enough for now.
            if (numOfTickMarks == 2 && reqLength > length) {
                break;
            }
            if (reqLength > length || count > 20) {
                tickUnit *= 2; // This is just for the while loop, if there are still too many ticks
            }
        }
        // calculate new scale
        final double newScale = calculateNewScale(length, minRounded, maxRounded);
        // return new range
        return new Object[]{minRounded, maxRounded, tickUnitRounded, newScale, formatter};
    }


    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    @Override
    protected Object getRange() {
        return new Object[]{
            getLowerBound(),
            getUpperBound(),
            getTickUnit(),
            //this.tickUnit,  // TODO: Select from TickUnitSource
            getScale(), //currentFormatterProperty.get()
        };
    }


    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this
     * method should animate the range to the new range.
     *
     * @param range A range object returned from autoRange()
     * @param animate If true animate the change in range
     */
    @Override
    protected void setRange(Object range, boolean animate) {
        final Object[] rangeProps = (Object[]) range;
        final double newLowerBound = (Double) rangeProps[0];
        final double newUpperBound = (Double) rangeProps[1];
        final double newTickUnit = (Double) rangeProps[2];
        final double newScale = (Double) rangeProps[3];
        //final String formatter = (String) rangeProps[4];
        //currentFormatterProperty.set(formatter);

        // TODO: determine the TickUnit this axis to it
        final int minorTickUnits = this.tickUnit.getMinorTickCount() + 1;
        this.setMinorTickCount(minorTickUnits);

        final double oldLowerBound = getLowerBound();
        setLowerBound(newLowerBound);
        setUpperBound(newUpperBound);
        setTickUnit(newTickUnit);
        
            currentLowerBound.set(newLowerBound);
            setScale(newScale);

    }


    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        final Object[] rangeProps = (Object[]) range;
        final double lowerBound = (Double) rangeProps[0];
        final double upperBound = (Double) rangeProps[1];
        final double tickUnit = (Double) rangeProps[2];
        List<Number> tickValues = new ArrayList<>();
        if (lowerBound == upperBound) {
            tickValues.add(lowerBound);
        }
        else if (tickUnit <= 0) {
            tickValues.add(lowerBound);
            tickValues.add(upperBound);
        }
        else if (tickUnit > 0) {
            tickValues.add(lowerBound);
            if (((upperBound - lowerBound) / tickUnit) > 2000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 2000 major tick marks on a NumberAxis. "
                    + "Lower Bound=" + lowerBound + ", Upper Bound=" + upperBound + ", Tick Unit=" + tickUnit);
            }
            else {
                if (lowerBound + tickUnit < upperBound) {
                    // If tickUnit is integer, start with the nearest integer
                    double first = Math.rint(tickUnit) == tickUnit ? Math.ceil(lowerBound) : lowerBound + tickUnit;
                    for (double major = first; major < upperBound; major += tickUnit) {
                        tickValues.add(major);
                    }
                }
            }
            tickValues.add(upperBound);
        }
        return tickValues;
    }


    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */
    @Override
    protected List<Number> calculateMinorTickMarks() {
        final List<Number> minorTickMarks = new ArrayList<>();
        final double lowerBound = this.getLowerBound();
        final double upperBound = this.getUpperBound();
        final double tickUnitSize = this.getTickUnit();
        final int minorTickUnits = this.tickUnit.getMinorTickCount() + 1;

        final double minorUnit = tickUnitSize / Math.max(1, minorTickUnits);

        // The following codeblock is from the JavaFX NumberAxix class
        if (tickUnitSize > 0) {
            if (((upperBound - lowerBound) / minorUnit) > 10000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 10000 minor tick marks on a NumberAxis. "
                    + "Lower Bound=" + this.getLowerBound() + ", Upper Bound=" + this.getUpperBound() + ", Tick Unit=" + tickUnitSize);
                return minorTickMarks;
            }
            final boolean tickUnitIsInteger = Math.rint(tickUnitSize) == tickUnitSize;
            if (tickUnitIsInteger) {
                for (double minor = Math.floor(lowerBound) + minorUnit;
                    minor < Math.ceil(lowerBound);
                    minor += minorUnit) {
                    if (minor > lowerBound) {
                        minorTickMarks.add(minor);
                    }
                }
            }
            double major = tickUnitIsInteger ? Math.ceil(lowerBound) : lowerBound;
            for (; major < upperBound; major += tickUnitSize) {
                final double next = Math.min(major + tickUnitSize, upperBound);
                for (double minor = major + minorUnit; minor < next; minor += minorUnit) {
                    minorTickMarks.add(minor);
                }
            }
        }
        return minorTickMarks;
    }


    @Override
    protected String getTickMarkLabel(Number t) {
        return this.tickUnit.getTickMarkLabel(t);
    }

}
