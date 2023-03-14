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
import java.util.Set;

import com.comino.flight.model.map.MAVGCLMap;
import com.comino.flight.model.map.MAVGCLOctoMap;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.impl.octomap.boundingbox.MAVBoundingBox;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point3D_I32;
import georegression.struct.point.Point4D_F32;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import us.ihmc.jOctoMap.iterators.OcTreeIteratorFactory;

public class XYGridAnnotation  implements XYAnnotation {


	private  Pane   	       pane 		= null;
//	private  Pane             indicator = null;
	private  MAVGCLOctoMap     map       = null;
	private  Map<Long,Pane>    blocks    = null;
	private  DataModel         model     = null;
	private  boolean           enabled   = false;
    private  Point4D_F32       mapo      = new Point4D_F32();
    
	public XYGridAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		this.blocks = new HashMap<Long,Pane>();

		//		indicator = new Pane();
		//		indicator.setStyle("-fx-background-color: rgba(180.0, 60.0, 100.0, 0.7);; -fx-padding:-1px; -fx-border-color: #606030;");
		//		indicator.setVisible(false);
		//		indicator.setCache(true);
		//		indicator.setCacheHint(CacheHint.SPEED);
		//		pane.getChildren().add(indicator);
	}

	public void setController(IMAVController control) {
		this.model = control.getCurrentModel();
		this.map   = MAVGCLOctoMap.getInstance(control);
	}


	@Override
	public Node getNode() {
		return pane;
	}
	
	public void setScale(float scale) {
		
	}
	
	private long tms;

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(pane.isDisabled() || !pane.isVisible())
			return;
		
		tms = System.currentTimeMillis();

		pane.getChildren().retainAll(blocks.values());

		if(model == null || model.grid==null || !enabled)
			return;
		
		if(map.getNumberOfNodes() == 0) {
			blocks.clear();
			return;
		}
		
		blocks.forEach((i,p) -> {
			map.decode(i, mapo);
			p.setLayoutX(xAxis.getDisplayPosition(mapo.y-map.getResolution()/2)+1.0f);
			p.setLayoutY(yAxis.getDisplayPosition(mapo.x+map.getResolution()/2));
			p.setPrefWidth(xAxis.getDisplayPosition(map.getResolution())-xAxis.getDisplayPosition(0));
			p.setPrefHeight(yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(map.getResolution()));
		});
		
		
		
		float current_altitude = (float)AnalysisModelService.getInstance().getCurrent().getValue("ALTRE");
		
		List<Long> set = map.getAtAltitudeEncoded(current_altitude, 0.6f);
		
		if(set.isEmpty())
			return;
		
		blocks.keySet().retainAll(set);
		
		set.forEach((i) -> {	
			Pane p = null;
			if(!blocks.containsKey(i) && blocks.size()<200)
				p = addBlockPane(i & 0x0FFFFFFFFFF00000L);
		});
		
	}

	public  void invalidate(boolean enable) {
        blocks.clear();
		enabled = enable;
	}
	
	public void clear() {
		pane.getChildren().clear();
		blocks.clear();
	}


	private Pane addBlockPane(long block) {
		Pane p = new Pane();
		p.setStyle("-fx-background-color: rgba(38, 136, 163, 0.5); -fx-padding:-1px; -fx-border-color: #20738a;");
	    pane.getChildren().add(p);
		blocks.put(block, p);
		p.setVisible( true);
		return p;
	}
	


}
