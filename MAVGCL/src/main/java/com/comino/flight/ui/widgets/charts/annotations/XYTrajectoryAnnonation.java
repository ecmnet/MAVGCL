/****************************************************************************
 *
 *   Copyright (c) 2021 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

import com.comino.flight.model.AnalysisDataModel;
import com.comino.mavcom.model.DataModel;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class XYTrajectoryAnnonation  implements XYAnnotation {

	private static final int   SIZE 		= 6;
	private static final float STEP 		= 0.2f;


	private Pane   	  			pane 	    = null;
	private AnalysisDataModel   model       = null;
	private Color               color       = null;
	private Circle              start       = null;
	private Circle              projected   = null;

	private float  p0x = 0;
	private float  v0x = 0;
	private float  a0x = 0;

	private float  p0y = 0;
	private float  v0y = 0;
	private float  a0y = 0;

	private float x= 0; 
	private float y= 0;

	private float x0= 0; 
	private float y0= 0;
	
	private boolean refresh = false;

	public XYTrajectoryAnnonation(final Color color) {

		this.color = color;
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);
		
		this.start = new Circle();
		this.start.setCenterX(SIZE/2);
		this.start.setCenterY(SIZE/2);
		this.start.setRadius(SIZE/2);
		this.start.setFill(color);
		this.start.setVisible(false);

		this.projected = new Circle();
		this.projected.setCenterX(SIZE/2);
		this.projected.setCenterY(SIZE/2);
		this.projected.setRadius(SIZE/2);
		this.projected.setFill(color);
		this.projected.setVisible(false);


	}

	public void setModel(AnalysisDataModel model) {
		this.model = model;
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override

	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(model==null)
			return;


		double current = model.getValue("TRAJCURRENT");
		double length  = model.getValue("TRAJLEN");
		

		if(!Double.isNaN(current) && !Double.isNaN(length) && current >= 0 && length >0) {
refresh = true; 
			if(current < STEP || refresh) {
				
				pane.getChildren().clear();
				pane.getChildren().addAll(start,projected);

				p0x = (float)model.getValue("TRAJSTARTX");
				p0y = (float)model.getValue("TRAJSTARTY");
				v0x = (float)model.getValue("TRAJSTARTVX");
				v0y = (float)model.getValue("TRAJSTARTVY");
				a0x = (float)model.getValue("TRAJSTARTAX");
				a0y = (float)model.getValue("TRAJSTARTAY");
				
				x = getPosition((float)length, p0x, v0x, a0x,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
				y = getPosition((float)length, p0y, v0y, a0y,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));

				projected.setLayoutX(xAxis.getDisplayPosition(y)-SIZE/2);
				projected.setLayoutY(yAxis.getDisplayPosition(x)-SIZE/2);
				projected.setVisible(true);
				

				start.setLayoutX(xAxis.getDisplayPosition(p0y)-SIZE/2);
				start.setLayoutY(yAxis.getDisplayPosition(p0x)-SIZE/2);
				start.setVisible(true);

				x0 = getPosition(0, p0x, v0x, 0,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
				y0 = getPosition(0, p0y, v0y, 0,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));

				for(double t = current; t < length; t = t + STEP ) {
					x = getPosition((float)t, p0x, v0x, a0x,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
					y = getPosition((float)t, p0y, v0y, a0y,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));
					Line l = new Line(xAxis.getDisplayPosition(y0),yAxis.getDisplayPosition(x0), xAxis.getDisplayPosition(y), yAxis.getDisplayPosition(x));
					l.setStroke(color);
					pane.getChildren().add(l);
					x0 = x;
					y0 = y;	

				}
				
				
				x = getPosition((float)length, p0x, v0x, a0x,(float)model.getValue("TRAJALPHAX"),(float)model.getValue("TRAJBETAX"),(float)model.getValue("TRAJGAMMAX"));
				y = getPosition((float)length, p0y, v0y, a0y,(float)model.getValue("TRAJALPHAY"),(float)model.getValue("TRAJBETAY"),(float)model.getValue("TRAJGAMMAY"));
				Line l = new Line(xAxis.getDisplayPosition(y0),yAxis.getDisplayPosition(x0), xAxis.getDisplayPosition(y), yAxis.getDisplayPosition(x));
				l.setStroke(color);
				pane.getChildren().add(l);
				
				refresh = false;
				
			}

		} else {
			clear();
		}

	}

	public void clear() {
		pane.getChildren().clear();
		refresh = true;
	}
	
	public void refresh() {
		refresh = true;
	}



	private float getPosition(float t, float p0, float v0, float a0, float a, float b, float g) { 
		return p0 + v0*t + (1.0f/2.0f)*a0*t*t + (1.0f/6.0f)*g*t*t*t + (1.0f/24.0f)*b*t*t*t*t + (1.0f/120.0f)*a*t*t*t*t*t; 
	}



}
