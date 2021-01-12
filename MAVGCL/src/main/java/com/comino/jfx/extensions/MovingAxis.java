/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.jfx.extensions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.ValueAxis;

public class MovingAxis extends ValueAxis<Number> {

	private DecimalFormat fo = new DecimalFormat("#0");
	private List<Number> mticks = new ArrayList<Number>();
	private List<Number> vticks = new ArrayList<Number>();

	private double mstart = -1;
	private double vstart = -1;

	private double munit = -1;
	private double vunit = -1;

	private SimpleObjectProperty<Double> tickUnitProperty = new SimpleObjectProperty<Double>(
			5d);

	@Override
	protected List<Number> calculateMinorTickMarks() {
		double tickUnit = tickUnitProperty.get() / getMinorTickCount();
		double start = Math.floor(getLowerBound() / tickUnit) * tickUnit;
		if(start != mstart || munit != tickUnit ) {
			mticks.clear();
			mstart = start; munit = tickUnit;
			for (double value = start; value <= getUpperBound(); value += tickUnit) {
				mticks.add(value);
			}}
		return mticks;
	}

	@Override
	protected List<Number> calculateTickValues(double arg0, Object arg1) {
		double tickUnit = tickUnitProperty.get();
		double start = Math.floor(getLowerBound() / tickUnit) * tickUnit;
		if(start != vstart || vunit != tickUnit  ) {
			vticks.clear();
			vstart = start; vunit = tickUnit;
			for (double value = start; value <= getUpperBound(); value += tickUnit) {
				vticks.add(value);
			}
		}
		return vticks;
	}

	@Override
	protected Object getRange() {
		return null;
	}

	@Override
	protected String getTickMarkLabel(Number label) {
		return String.valueOf((int)label.floatValue());
	//	return fo.format(label.doubleValue());
	}

	@Override
	protected void setRange(Object range, boolean arg1) {
		// not sure how this is used??
	}

	public SimpleObjectProperty<Double> getTickUnitProperty() {
		return tickUnitProperty;
	}

	public void setTickUnit(double tickUnit) {
		tickUnitProperty.set(tickUnit);
	}



}