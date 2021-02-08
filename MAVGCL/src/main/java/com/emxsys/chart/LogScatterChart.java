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
package com.emxsys.chart;

import com.emxsys.chart.extension.MajorLogGridlines;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;


/**
 * A JavaFX Chart Extensions' logarithmic scatter chart with subtitle, markers, annotations and
 * major logarithmic grid lines.
 *
 * @author Bruce Schubert
 * @param <X>
 * @param <Y>
 */
public class LogScatterChart<X, Y> extends EnhancedScatterChart<X, Y> {

    private MajorLogGridlines<X, Y> gridlines;


    /**
     * Constructs a logarithmic scatter chart.
     *
     * @param xAxis
     * @param yAxis
     */
    public LogScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }


    /**
     * Constructs a logarithmic scatter chart.
     *
     * @param xAxis
     * @param yAxis
     * @param data
     */

    public LogScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis,
        @NamedArg("data") ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
        this.gridlines = new MajorLogGridlines<>(this, this.getChartChildren());
        this.requestChartLayout();
    }


    /**
     * Performs a layout that includes the major grid lines.
     */
    @Override
    protected void layoutChildren() {
        // Invoked during the layout pass to layout this chart and all its content.
        super.layoutChildren();
        gridlines.layoutGridlines();
    }

}
