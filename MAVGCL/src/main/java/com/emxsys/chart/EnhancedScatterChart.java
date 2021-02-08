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

import com.emxsys.chart.extension.AnnotationExtension;
import com.emxsys.chart.extension.MarkerExtension;
import com.emxsys.chart.extension.Subtitle;
import com.emxsys.chart.extension.SubtitleExtension;
import com.emxsys.chart.extension.XYAnnotations;
import com.emxsys.chart.extension.XYMarkers;
import java.util.List;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.ScatterChart;

/**
 * An enhanced version of a ScatterChart.
 *
 * @author Bruce Schubert
 * @param <X>
 * @param <Y>
 */
public class EnhancedScatterChart<X, Y> extends ScatterChart<X, Y>
        implements SubtitleExtension, MarkerExtension, AnnotationExtension {

    private Subtitle subtitle;
    private XYMarkers<X, Y> markers;
    private XYAnnotations annotations;

    public EnhancedScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }


    public EnhancedScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis,
            @NamedArg("data") ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
        subtitle = new Subtitle(this, getChildren(), getLegend());
        markers = new XYMarkers<>(this, getPlotChildren());
        annotations = new XYAnnotations(this, getChartChildren());
    }

    /**
     * Gets a copy of the subtitle strings.
     *
     * @return A list of subtitles.
     */
    @Override
    public List<String> getSubtitles() {
        return this.subtitle.getSubtitles();
    }

    /**
     *
     * @param subtitle Subtitle text; may be null.
     */
    @Override
    public void addSubtitle(String subtitle) {
        this.subtitle.addSubtitle(subtitle);
        this.requestLayout();
    }

    @Override
    public void clearSubtitles() {
        this.subtitle.clearSubtitles();
    }

    @Override
    public XYAnnotations getAnnotations() {
        return this.annotations;
    }

    @Override
    public XYMarkers getMarkers() {
        return this.markers;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        subtitle.layoutSubtitles();
    }

    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        this.annotations.layoutAnnotations();
        this.markers.layoutMarkers();
    }

}
