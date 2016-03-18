package com.comino.flight.widgets.charts.line;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.geometry.Orientation;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class ModeAnnotation implements XYAnnotation {

	private final Rectangle rectangle = new Rectangle();

	private double min, max;
	private final Orientation orientation;

	public ModeAnnotation(double min, double max, Orientation orientation, Paint fillPaint) {
		this(min, max, orientation, 0, null, fillPaint);
	}

	public ModeAnnotation(double min, double max, Orientation orientation, double strokeWidth, Paint outlinePaint, Paint fillPaint) {

		this.min = min;
		this.max = max;
		this.orientation = orientation;

		rectangle.getStyleClass().add("chart-annotation-field");

		rectangle.setStrokeWidth(strokeWidth);
		rectangle.setStroke(outlinePaint);
		rectangle.setFill(fillPaint);
	}


	@Override
	public Rectangle getNode() {
		return rectangle;
	}


	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		double strokeW = rectangle.getStrokeWidth();

		double x, y, w, h;
		if (orientation == Orientation.HORIZONTAL) {
			x = xAxis.getDisplayPosition(xAxis.getLowerBound()) - strokeW;
			y = yAxis.getDisplayPosition(max);
			w = xAxis.getDisplayPosition(xAxis.getUpperBound()) + strokeW - x;
			h = yAxis.getDisplayPosition(min) - y;
		} else {
			x = xAxis.getDisplayPosition(min);
			y = yAxis.getDisplayPosition(yAxis.getUpperBound()) - strokeW;
			w = xAxis.getDisplayPosition(max) - x;
			h = yAxis.getDisplayPosition(yAxis.getLowerBound()) + strokeW - y;
		}

		rectangle.setX(x);
		rectangle.setY(y);
		rectangle.setWidth(w);
		rectangle.setHeight(h);
	}

	public void setMax(double max) {
		this.max = max;
	}

}

