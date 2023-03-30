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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.model.map.MAVGCLOctoMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOccupancyOcTreeNode;
import com.comino.mavmap.map.map3D.impl.octomap.boundingbox.MAVSimpleBoundingBox;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point4D_F32;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.ValueAxis;
import javafx.scene.paint.Color;
import us.ihmc.jOctoMap.iterators.OcTreeIterable;
import us.ihmc.jOctoMap.iterators.OcTreeIteratorFactory;

public class XYGridAnnotation  implements XYAnnotation {


	private final Canvas                 canvas;
	private final Map<Long,Point4D_F32>  blocks;
	private final GraphicsContext        gc;
	private final Point4D_F32            mapo;

	private MAVGCLOctoMap                map;
	private MAVSimpleBoundingBox         boundingBox;
	private AnalysisDataModel           model;

	private float                        scale = 10.0f;

	public XYGridAnnotation() {
		super();
		this.canvas = new Canvas(2000,2000);
		this.blocks = new HashMap<Long,Point4D_F32>();
		this.gc     = canvas.getGraphicsContext2D();
		this.mapo   = new Point4D_F32();

		gc.setFill(Color.web("#2688A3",0.3f));
		gc.setStroke(Color.web("#2688A3",1f).darker());
		gc.setLineWidth(2);
		
		
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setController(IMAVController control) {
		this.map   = MAVGCLOctoMap.getInstance(control);
		this.boundingBox = new MAVSimpleBoundingBox(map.getResolution(),16);
		this.model = AnalysisModelService.getInstance().getCurrent();
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

		@SuppressWarnings("unchecked")
		final double width  = xAxis.getDisplayPosition(map.getResolution())-xAxis.getDisplayPosition(0);
		@SuppressWarnings("unchecked")
		final double height = yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(map.getResolution());


		mapo.setTo((float)model.getValue("LPOSX"),(float)model.getValue("LPOSY"),(float)model.getValue("LPOSZ"),0);
		boundingBox.set(mapo,scale,0.25f);
		List<Long> set = map.getLeafsInBoundingBoxEncoded(boundingBox);
		
		blocks.keySet().retainAll(set);

		set.forEach((i) -> {
			if(blocks.containsKey(i & 0x0FFFFFFFFFF00000L))
				return;
			final Point4D_F32 p = new Point4D_F32();
			map.decode(i, p);
			blocks.put(i & 0x0FFFFFFFFFF00000L, p);	
		});
	

		blocks.values().forEach((r) -> {
			
			@SuppressWarnings("unchecked")
			final double x0 = xAxis.getDisplayPosition(r.y-map.getResolution()/2);
			@SuppressWarnings("unchecked")
			final double y0 = yAxis.getDisplayPosition(r.x+map.getResolution()/2);
			
			gc.fillRect(x0,y0,width, height);
			
			gc.strokeLine(x0, y0,x0+width,y0);
			gc.strokeLine(x0+width,y0,x0+width,y0+height);
			gc.strokeLine(x0+width,y0+height,x0,y0+height);
			gc.strokeLine(x0,y0+height,x0,y0);


		});

	}
}
