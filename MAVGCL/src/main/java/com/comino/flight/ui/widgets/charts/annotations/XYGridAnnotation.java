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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.flight.model.map.MAVGCLMap;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.emxsys.chart.extension.XYAnnotation;

import bubo.maps.d3.grid.CellProbability_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point3D_I32;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;

public class XYGridAnnotation  implements XYAnnotation {


	private  Pane   	      pane 		= null;
//	private  Pane             indicator = null;
	private  MAVGCLMap          map       = null;
	private Map<Long,Pane>    blocks    = null;
	private DataModel         model     = null;
	private boolean           enabled   = false;
    private Point3D_I32       mapp      = new Point3D_I32();
    private Point3D_F64       mapo      = new Point3D_F64();


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
		this.map   = MAVGCLMap.getInstance();
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(pane.isDisabled() || !pane.isVisible())
			return;

		pane.getChildren().retainAll(blocks.values());

		if(model == null || model.grid==null || !enabled)
			return;
		
		if(map.size()==0) {
			blocks.clear();
			return;
		}
		
		Set<Long> set = map.getLevelSet(blocks.isEmpty());
		if(set.isEmpty())
			return;
		
		blocks.keySet().retainAll(set);	
		
		set.forEach((i) -> {
			
			Pane p = null;
			if(!blocks.containsKey(i))
				p = addBlockPane(i);
			else
				p = blocks.get(i);
			
			map.getInfo().decodeMapPoint(i, mapp);
			map.getInfo().mapToGlobal(mapp, mapo);
			
			p.setLayoutX(xAxis.getDisplayPosition(mapo.y));
			p.setLayoutY(yAxis.getDisplayPosition(mapo.x+map.getInfo().getCellSize()));
			p.setPrefWidth(xAxis.getDisplayPosition(map.getInfo().getCellSize())-xAxis.getDisplayPosition(0));
			p.setPrefHeight(yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(map.getInfo().getCellSize()));
		
			
		});
	}

	public  void invalidate(boolean enable) {
		if(map!=null) {
		    blocks.keySet().retainAll(map.getLevelSet(true));	
		}
		enabled = enable;
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
