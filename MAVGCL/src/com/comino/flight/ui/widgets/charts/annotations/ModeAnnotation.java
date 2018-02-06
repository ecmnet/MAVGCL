/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.ui.widgets.charts.annotations;

import java.util.HashMap;
import java.util.Map;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ModeAnnotation implements XYAnnotation {

	private Pane         		node         = null;
	private Map<Integer,Paint>	colors       = null;

	private double lowBound;
	private double highBound;

	private Area last = null;

	public ModeAnnotation() {
		this.node   = new Pane();
		this.colors = new HashMap<Integer,Paint>();
		colors.put(0, Color.TRANSPARENT);
	}


	@Override
	public Node getNode() {
		return node;
	}

	public void setModes(String... color) {
		colors.clear();
		for(int i=0;i<color.length;i++)
			colors.put(i+1, Color.web(color[i], 0.04f));
	}

	public void clear() {
			node.getChildren().clear();
	}

	public void setVisible(boolean visible) {
		this.node.setVisible(visible);
	}

	public void setBounds(double lowBound, double highBound) {
		this.lowBound  = lowBound;
		this.highBound = highBound;
	}

	public void addAreaData(double time, int mode) {


		if(!this.node.isVisible())
			return;

		if(node.getChildren().isEmpty()) {
			last = new Area(mode, time,time, colors.get(mode));
			node.getChildren().add(last);
			return;
		}

		if(time >= last.to) {
			if(last.isMode(mode)) {
				last.to = time;
				return;
			}
			last = new Area(mode, time,time, colors.get(mode));
			node.getChildren().add(last);
		}
	}


	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(!this.node.isVisible())
			return;

		node.getChildren().forEach((n) -> {
			((Area)n).layout(xAxis, yAxis);
		});
	}

	private class Area extends Rectangle implements Comparable<Area> {


		private double from;
		private double to;
		private int    mode;

		public Area(int mode, double from, double to, Paint fillPaint) {
			super();
			this.from = from;
			this.to   = to;
			this.mode = mode;
			this.setStrokeWidth(0);
			this.setStroke(fillPaint);
			this.setFill(fillPaint);
			this.setY(0);
			this.setHeight(1000);
		}

		public void layout(ValueAxis<Double> xAxis, ValueAxis<Double> yAxis) {

			if(to < lowBound || from > highBound)
				return;

			double	x = xAxis.getDisplayPosition(from);
			double	w = xAxis.getDisplayPosition(to) - x;
			this.setX(x);
			this.setWidth(w);
		}

		public boolean isMode(int mode) {
			return this.mode == mode;
		}

		@Override
		public int compareTo(Area a) {
			if (from < a.from)
				return -1;
			if (from > a.from)
				return 1;
			return 0;
		}
	}

}

