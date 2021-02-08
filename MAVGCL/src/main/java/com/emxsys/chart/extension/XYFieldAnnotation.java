package com.emxsys.chart.extension;

import javafx.geometry.Orientation;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Similar to {@link XYPolygonAnnotation} this annotation draws a polygon. The difference is that it spans the
 * chart in one dimension and is square, so that only a min/max needs to be specified.
 * @author Mikael Grev, Avioniq AB
 *         Date: 16-02-09, Time: 23:09
 */

public class XYFieldAnnotation implements XYAnnotation {

	private final Rectangle rectangle = new Rectangle();

	private final double min, max;
	private final Orientation orientation;


	/**
	 * Constructs a polygon annotation with specific fill only.
	 *
	 * @param min The start/min value in either vertical of horizontal dimension
	 * @param max The end/max value in either vertical of horizontal dimension
	 * @param orientation E.g. HORIZONTAL for a horizontal band/field where the coordinates above are y-values.
	 * @param fillPaint See {@link Polygon#setFill(Paint)}
	 */
	public XYFieldAnnotation(double min, double max, Orientation orientation, Paint fillPaint) {
		this(min, max, orientation, 0, null, fillPaint);
	}

	/**
	 * Constructs a polygon annotation with specific stroke and colors that override CSS styles.
	 *
	 * @param min The start/min value in either vertical of horizontal dimension
	 * @param max The end/max value in either vertical of horizontal dimension
	 * @param orientation E.g. HORIZONTAL for a horizontal band/field where the coordinates above are y-values.
	 * @param strokeWidth See {@link Polygon#setStrokeWidth(double)}
	 * @param outlinePaint See {@link Polygon#setStroke(Paint)}
	 * @param fillPaint See {@link Polygon#setFill(Paint)}
	 */
	public XYFieldAnnotation(double min, double max, Orientation orientation, double strokeWidth, Paint outlinePaint, Paint fillPaint) {

		this.min = min;
		this.max = max;
		this.orientation = orientation;

		rectangle.getStyleClass().add("chart-annotation-field");

		rectangle.setStrokeWidth(strokeWidth);
		rectangle.setStroke(outlinePaint);
		rectangle.setFill(fillPaint);
	}


	/**
	 * Gets the node representation. Can be used for setting the stroke more precisely, for instance.
	 * @return A Polygon object.
	 */
	@Override
	public Rectangle getNode() {
		return rectangle;
	}


	/**
	 * Performs the layout for the polygon.
	 *
	 * @param xAxis
	 * @param yAxis
	 */
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

	/**
	 * Assigns a Tooltip to the polygon.
	 *
	 * @param text The tooltip text to be displayed.
	 */
	public void setTooltipText(String text) {
		Tooltip.install(rectangle, new Tooltip(text));
	}
}

