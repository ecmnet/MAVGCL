package com.comino.flight.widgets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.ValueAxis;

public class MovingAxis extends ValueAxis<Number> {

	private DecimalFormat fo = new DecimalFormat("#0");

    private SimpleObjectProperty<Double> tickUnitProperty = new SimpleObjectProperty<Double>(
            5d);

    @Override
    protected List<Number> calculateMinorTickMarks() {
        List<Number> ticks = new ArrayList<Number>();
        double tickUnit = tickUnitProperty.get() / getMinorTickCount();
        double start = Math.floor(getLowerBound() / tickUnit) * tickUnit;
        for (double value = start; value <= getUpperBound(); value += tickUnit) {
            ticks.add(value);
        }
        return ticks;
    }

    @Override
    protected List<Number> calculateTickValues(double arg0, Object arg1) {
        List<Number> ticks = new ArrayList<Number>();
        double tickUnit = tickUnitProperty.get();
        double start = Math.floor(getLowerBound() / tickUnit) * tickUnit;
        for (double value = start; value <= getUpperBound(); value += tickUnit) {
            ticks.add(value);
        }
        return ticks;
    }

    @Override
    protected Object getRange() {
       return null;
    }

    @Override
    protected String getTickMarkLabel(Number label) {
        return fo.format(label.doubleValue());
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