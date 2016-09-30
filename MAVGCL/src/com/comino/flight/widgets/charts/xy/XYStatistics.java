package com.comino.flight.widgets.charts.xy;

import java.util.List;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.KeyFigureMetaData;

public class XYStatistics {

	public float center_x;
	public float center_y;

	public float stddev_x;
	public float stddev_y;

	public float stddev_xy;
	public float radius;

	public float distance;

	private KeyFigureMetaData fy;
	private KeyFigureMetaData fx;


	public void setKeyFigures(KeyFigureMetaData fx, KeyFigureMetaData fy) {
		this.fx = fx; this.fy=fy;
	}

	public void getStatistics(int x0, int x1, List<AnalysisDataModel> list) {
		float vx=0; float vy=0; int i=0; float radius=0;

		x1 =  list.size() < x1 ? list.size()-1 : x1-1;

		if(list.size() < 10 || fx.hash==0 || fy.hash==0)
			return;

		for(i = x0; i< x1;i++) {
	        vx += list.get(i).getValue(fx);
	        vy += list.get(i).getValue(fy);
		}
		center_x = vx / (i - x0);
		center_y = vy / (i - x0);

		vx = 0; vy = 0;
		for(i = x0; i< x1 ;i++) {
	        vx += (list.get(i).getValue(fx) - center_x) * (list.get(i).getValue(fx) - center_x);
	        vy += (list.get(i).getValue(fy) - center_y) * (list.get(i).getValue(fy) - center_y);
	        if(Math.abs(list.get(i).getValue(fx)-center_x) > radius)
	        	radius = Math.abs(list.get(i).getValue(fx)-center_x);
	        if(Math.abs(list.get(i).getValue(fy)-center_y) > radius)
	        	radius = Math.abs(list.get(i).getValue(fy)-center_y);

		}

        this.radius = radius;
		stddev_x =(float)Math.sqrt( vx / (i - x0));
		stddev_y =(float)Math.sqrt( vy / (i - x0));

		distance =  (float)Math.sqrt(
				(list.get(0).getValue(fx) - list.get(x1).getValue(fx)) *
				(list.get(0).getValue(fx) - list.get(x1).getValue(fx)) +
				(list.get(0).getValue(fy) - list.get(x1).getValue(fy)) *
				(list.get(0).getValue(fy) - list.get(x1).getValue(fy)));

		stddev_xy = (float)Math.sqrt(stddev_x*stddev_x+stddev_y*stddev_y);
	}

	public String getHeader() {
		if(fx!=null && fy!=null)
		   return fx.desc1+" / "+fy.desc1+" ["+fx.uom+"]";
		return null;
	}

	public void clear() {
		center_x = 0;
		center_y = 0;
		stddev_x = 0;
		stddev_y = 0;
	}

}
