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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.comino.flight.model.map.MAVGCLMap;
import com.comino.mavcom.control.IMAVController;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point4D_F32;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.ValueAxis;
import javafx.scene.paint.Color;

public class XYGridAnnotation2  implements XYAnnotation {


	private final Canvas                 canvas;
	private final Map<Long,Point4D_F32>  blocks;
	private final GraphicsContext        gc;

	private MAVGCLMap                     map;
	private float scale = 5.0f;



	public XYGridAnnotation2() {
		super();
		this.canvas = new Canvas(5000,5000);
		this.blocks = new ConcurrentHashMap<Long,Point4D_F32>();
		this.gc     = canvas.getGraphicsContext2D();

		gc.setFill(Color.web("#2688A3",0.3f));
		gc.setStroke(Color.web("#2688A3",1f).darker());
		gc.setLineWidth(2);
	
	
	}


	public void setController(IMAVController control) {
		this.map   = MAVGCLMap.getInstance();
		
	}

	public  void invalidate(boolean enable) {
		blocks.clear();
	}

	public void clear() {
		blocks.clear();
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	@Override
	public Node getNode() {
		return canvas;
	}
	
	

	@SuppressWarnings("rawtypes")
	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(canvas.isDisabled() || !canvas.isVisible())
			return;
		
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
try {
		map.getMap().values().forEach((r) -> {
			
			@SuppressWarnings("unchecked")
			final double width  = xAxis.getDisplayPosition(r.width)-xAxis.getDisplayPosition(0);
			@SuppressWarnings("unchecked")
			final double height = yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(r.width);
			
			@SuppressWarnings("unchecked")
			final double x0 = xAxis.getDisplayPosition((int)(r.p.y/r.width)*r.width-r.width/2);
			@SuppressWarnings("unchecked")
			final double y0 = yAxis.getDisplayPosition((int)(r.p.x/r.width)*r.width+r.width/2);
			
			gc.fillRect(x0,y0,width, height);
			
			gc.strokeLine(x0, y0,x0+width,y0);
			gc.strokeLine(x0+width,y0,x0+width,y0+height);
			gc.strokeLine(x0+width,y0+height,x0,y0+height);
			gc.strokeLine(x0,y0+height,x0,y0);

		});
}
catch( java.util.ConcurrentModificationException z) { };

	}


	public void setScale(float scale) {
		this.scale = scale;
		
	}
}
