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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.Chart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * The Subtitle class adds the subtitle capability to a JavaFX chart.
 *
 * @author Bruce Schubert
 */
public class Subtitle {

    private final Chart chart;
    private final ObservableList<Node> children;

    private String subtitle;
    private final LinkedHashMap<String, Label> subtitles = new LinkedHashMap<>();
    private final Label subtitleLabel = new Label();
    private final Label titleLabel;
    private final Pane chartContent;
    private final Node legend;

    /**
     * Constructs a Subtitle for a JavaFX chart.
     *
     * @param chart The chart to have a subtitle.
     * @param children A modifiable list of the chart's children.
     * @param legend The chart's legend object.
     */
    public Subtitle(Chart chart, ObservableList<Node> children, Node legend) {
        this.chart = chart;
        this.children = children;
        this.children.add(subtitleLabel);
        subtitleLabel.getStyleClass().add("chart-subtitle");
        subtitleLabel.setAlignment(Pos.CENTER);

        // TODO: could possibly discover or validate title and content based on styles...
        // Observe this excerpt from the Chart constructor:
        //        titleLabel.getStyleClass().add("chart-title");
        //        chartContent.getStyleClass().add("chart-content");
        this.titleLabel = (Label) children.get(0);
        this.chartContent = (Pane) children.get(1);
        this.legend = legend;
    }

    public List<String> getSubtitles() {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(this.subtitles.keySet());
        return list;
    }

    /**
     * Adds a subtitle to a chart. Subtitles are displayed in the order they are
     * inserted.
     *
     * @param subtitle Subtitle text.
     */
    public void addSubtitle(String subtitle) {
        if (Objects.isNull(subtitle) || subtitle.isEmpty()) {
            throw new IllegalArgumentException("subtitle text cannot be null or empty.");
        }
        Label label = new Label(subtitle);
        label.getStyleClass().add("chart-subtitle");
        label.setAlignment(Pos.CENTER);
        this.subtitles.put(subtitle, label);
        this.children.add(label);
        this.chart.requestLayout();
    }

    /**
     * Removes all the subtitles from the chart.
     */
    public void clearSubtitles() {
        this.subtitles.values().stream().forEach((label) -> {
            this.children.remove(label);
        });
        this.subtitles.clear();
    }

    /**
     * Lays out the subtitles.
     */
    public void layoutSubtitles() {

        if (subtitles.isEmpty()) {
            return;
        }
        //subtitleLabel.setVisible(true);

        // Get the current layout coordinates
        double top = titleLabel.getLayoutY() + titleLabel.getHeight();
        double left = chart.snappedLeftInset();
        double bottom = chart.snappedBottomInset();
        double right = chart.snappedRightInset();
        final double width = chart.getWidth();
        final double height = chart.getHeight();

        Collection<Label> labels = subtitles.values();

        switch (chart.getTitleSide()) {
            case TOP: {
                double subtitlesHeight = 0;
                for (Label label : labels) {
                    final double labelHeight = (label.prefHeight(width - left - right));
                    final double labelTop = top + subtitlesHeight;
                    label.resizeRelocate(left, labelTop, width - left - right, labelHeight);
                    subtitlesHeight += labelHeight;
                }
                // Resize the chart content to accomdate the subtitles
                top += subtitlesHeight;
                chartContent.resizeRelocate(
                        chartContent.getLayoutX(),
                        chartContent.getLayoutY() + subtitlesHeight,
                        chartContent.getWidth(),
                        chartContent.getHeight() - subtitlesHeight);
                break;
            }
            case BOTTOM: {
                throw new UnsupportedOperationException("BOTTOM side Subtitle not implemented yet.");
//                final double subtitleHeight = (subtitleLabel.prefHeight(width - left - right));
//                subtitleLabel.resizeRelocate(left, height - bottom - subtitleHeight, width - left - right, subtitleHeight);
//                bottom += subtitleHeight;
//                top += subtitleHeight;
//                break;
            }
            case LEFT: {
                throw new UnsupportedOperationException("LEFT side Subtitle not implemented yet.");
//                final double titleWidth = (subtitleLabel.prefWidth(height - top - bottom));
//                subtitleLabel.resizeRelocate(left, top, titleWidth, height - top - bottom);
//                left += titleWidth;
//                break;
            }
            case RIGHT: {
                throw new UnsupportedOperationException("RIGHT side Subtitle not implemented yet.");
//                final double titleWidth = (subtitleLabel.prefWidth(height - top - bottom));
//                subtitleLabel.resizeRelocate(width - right - titleWidth, top, titleWidth, height - top - bottom);
//                right += titleWidth;
//                break;
            }
            default:
                break;
        }

    }

}
