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

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.map.MAVGCLOctoMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavutils.workqueue.WorkQueue;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point4D_F32;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.ValueAxis;
import javafx.scene.paint.Color;

public class XYEDFAnnotation  implements XYAnnotation {


	private final Canvas                 canvas;
	private final GraphicsContext        gc;
	private final Point4D_F32            mapo;

	private MAVGCLOctoMap                map;
	private AnalysisDataModel           model;

	private float                        scale = 10.0f;
	private float                        resolution;
	private int[][]                      edf_map;

	public XYEDFAnnotation() {
		super();
		this.canvas = new Canvas(2000,2000);
		this.gc     = canvas.getGraphicsContext2D();
		this.mapo   = new Point4D_F32();
		
		gc.setFill(Color.web("#4628A3",0.6f));
		gc.setStroke(Color.web("#4688A3",1f).darker());
		gc.setLineWidth(2);	
		
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setController(IMAVController control) {
		this.map      = MAVGCLOctoMap.getInstance(control);
		this.edf_map  = map.getLocalEDF2D().getESDF2DMap();
		this.model    = AnalysisModelService.getInstance().getCurrent();
		
	}

	public  void invalidate(boolean enable) {
		//update();
	}

	public void clear() {
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	@Override
	public Node getNode() {
		return canvas;
	}
	
	public void update() {
		
		if(canvas.isDisabled() || !canvas.isVisible() || model == null)
			return;
		
		float xp = (float)model.getValue("LPOSX");  xp = ((int)(xp / resolution)) * resolution;
		float yp = (float)model.getValue("LPOSY");  yp = ((int)(yp / resolution)) * resolution;
		float zp = (float)model.getValue("LPOSZ");  zp = ((int)(zp / resolution)) * resolution;
	
		mapo.setTo(xp,yp,zp,0);
		map.updateESDF(mapo);
		edf_map  = map.getLocalEDF2D().getESDF2DMap();


	}


	@SuppressWarnings("rawtypes")
	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(canvas.isDisabled() || !canvas.isVisible() || model == null)
			return;
		
		this.resolution = map.getResolution();
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

		@SuppressWarnings("unchecked")
		final double width  = xAxis.getDisplayPosition(map.getResolution())-xAxis.getDisplayPosition(0);
		@SuppressWarnings("unchecked")
		final double height = yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(map.getResolution());
		
		
		for(int x =0; x < map.getLocalEDF2D().gezSizeX();x++) {
			for(int y =0; y < map.getLocalEDF2D().gezSizeY();y++) {
				
				float xf = ( x - map.getLocalEDF2D().gezSizeX()/2 ) * resolution + mapo.x; //((int)(mapo.x / resolution))*resolution;
				float yf = ( y - map.getLocalEDF2D().gezSizeY()/2 ) * resolution + mapo.y; //((int)(mapo.y / resolution))*resolution;
				
				@SuppressWarnings("unchecked")
				final double x0 = xAxis.getDisplayPosition(yf-map.getResolution()/2);
				@SuppressWarnings("unchecked")
				final double y0 = yAxis.getDisplayPosition(xf+map.getResolution()/2);
				
				int val = edf_map[x][y];
				if(val == -1) {	
					gc.setFill(Color.web("#4688A3",0.8));
				} else {
					float t = 1.0f-(float)Math.sqrt(val)/10f; if(t<0) t = 0; if(t>0.7f) t= 0.7f;
					gc.setFill(Color.web("#4688A3",t).darker());
				}
				
				gc.fillRect(x0,y0,width, height);	
			}
		}
	
	}
	

}
