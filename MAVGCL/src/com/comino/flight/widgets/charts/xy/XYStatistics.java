package com.comino.flight.widgets.charts.xy;

import java.util.List;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.KeyFigureMetaData;

public class XYStatistics {

	public float center_x;
	public float center_y;

	public float stddev_x;
	public float stddev_y;

	public void getStatistics(int x0, int x1, List<AnalysisDataModel> list, KeyFigureMetaData fx, KeyFigureMetaData fy) {
		float vx=0; float vy=0; int i=0;
		float maxx = -Float.MAX_VALUE; float maxy = -Float.MAX_VALUE;
		float minx = -Float.MAX_VALUE; float miny = -Float.MAX_VALUE;

		if(list.size() < 10 || fx.hash==0 || fy.hash==0)
			return;

		for(i = x0; i< x1 && i < list.size();i++) {
	        vx += list.get(i).getValue(fx);
	        vy += list.get(i).getValue(fy);
		}
		center_x = vx / (i - x0);
		center_y = vy / (i - x0);

		vx = 0; vy = 0;
		for(i = x0; i< x1 && i < list.size();i++) {
	        vx += (list.get(i).getValue(fx) - center_x) * (list.get(i).getValue(fx) - center_x);
	        vy += (list.get(i).getValue(fy) - center_y) * (list.get(i).getValue(fy) - center_y);
		}

		stddev_x =(float)Math.sqrt( vx / (i - x0));
		stddev_y =(float)Math.sqrt( vy / (i - x0));
	}

	public void clear() {
		center_x = 0;
		center_y = 0;
		stddev_x = 0;
		stddev_y = 0;
	}

}
