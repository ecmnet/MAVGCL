package com.comino.flight.widgets.charts.line;

import java.util.Enumeration;
import java.util.Hashtable;

import javafx.scene.chart.XYChart;

public class XYValueItemPool {

	private Hashtable<XYChart.Data<Number,Number>,Boolean> locked, unlocked;

	public XYValueItemPool() {
		locked = new Hashtable<XYChart.Data<Number,Number>,Boolean>();
		unlocked = new Hashtable<XYChart.Data<Number,Number>,Boolean>();
	}

	public synchronized XYChart.Data<Number,Number> checkOut(float x, float y)
	{
		XYChart.Data<Number,Number> o;
		if( unlocked.size() > 0 )
		{
			Enumeration<XYChart.Data<Number,Number>> e = unlocked.keys();
			o = e.nextElement();
			o.setXValue(x);
			o.setYValue(y);
			unlocked.remove(o);
			locked.put(o, true);
			return(o);
		}
		// no objects available, create a new one
		o = new XYChart.Data<Number,Number>(x,y);
		locked.put( o, true );
		return( o );
	}

	public synchronized void invalidate(XYChart.Data<Number,Number> o) {
		locked.remove(o);
		unlocked.put(o, true);
	}

	public synchronized void invalidateAll() {
		unlocked.putAll(locked);
		locked.clear();
	}

	public int getLockedSize() {
		return locked.size();
	}

}
