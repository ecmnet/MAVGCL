package com.comino.flight.widgets;

import com.emxsys.chart.extension.XYAnnotations;

import javafx.beans.NamedArg;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;

public class SectionLineChart<X,Y> extends LineChart<X, Y> {

	private XYAnnotations annotations;

	public SectionLineChart(@NamedArg("xAxis")Axis<X> xAxis, @NamedArg("yAxis")Axis<Y> yAxis) {
		super(xAxis, yAxis);
		annotations = new XYAnnotations(this, getChartChildren());
	}

	@Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        if(annotations!=null)
          this.annotations.layoutAnnotations();
    }

	public XYAnnotations getAnnotations() {
        return this.annotations;
    }

}
