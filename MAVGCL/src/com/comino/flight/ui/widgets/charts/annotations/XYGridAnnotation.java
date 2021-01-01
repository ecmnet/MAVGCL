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

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.comino.flight.model.map.IMAVMap;
import com.comino.flight.model.map.MAV2DMap;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.struct.MapPoint3D_F32;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_I32;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;

public class XYGridAnnotation  implements XYAnnotation {


	private  Pane   	    pane 		= null;
	private  Pane           indicator   = null;

	private  IMAVMap          map       = null;

	private Map<Integer,Pane> blocks    = null;
	private Set<Integer>      tmp      = null;
	private DataModel         model     = null;

	private boolean        enabled = false;


	public XYGridAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		this.blocks = new HashMap<Integer,Pane>();
		this.tmp    = new HashSet<Integer>();

		//		indicator = new Pane();
		//		indicator.setStyle("-fx-background-color: rgba(180.0, 60.0, 100.0, 0.7);; -fx-padding:-1px; -fx-border-color: #606030;");
		//		indicator.setVisible(false);
		//		indicator.setCache(true);
		//		indicator.setCacheHint(CacheHint.SPEED);
		//		pane.getChildren().add(indicator);
	}

	public void setController(IMAVController control) {
		this.model = control.getCurrentModel();
		this.map   = new MAV2DMap(control);	
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(pane.isDisabled() || !pane.isVisible())
			return;

		pane.getChildren().retainAll(blocks.values());

		if(model == null || model.grid==null || !enabled)
			return;

		blocks.keySet().retainAll(map.getMap().keySet());	

		map.getMap().forEach((i,b) -> {
			Pane p = null;
			if(!blocks.containsKey(i))
				p = addBlockPane(i);
			else
				p = blocks.get(i);

			p.setLayoutX(xAxis.getDisplayPosition(b.y/100f));
			p.setLayoutY(yAxis.getDisplayPosition(b.x/100f+model.grid.getResolution()));
			p.setPrefWidth(xAxis.getDisplayPosition(model.grid.getResolution())-xAxis.getDisplayPosition(0));
			p.setPrefHeight(yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(model.grid.getResolution()));
		});
	}

	public  void invalidate(boolean enable) {
		if(map!=null)
			blocks.keySet().retainAll(map.getMap().keySet());	
		enabled = enable;
	}

	public void clear() {
		blocks.clear();
	}


	private Pane addBlockPane(int block) {
		Pane p = new Pane();
		p.setStyle("-fx-background-color: rgba(38, 136, 163, 0.5); -fx-padding:-1px; -fx-border-color: #20738a;");
		pane.getChildren().add(p);
		blocks.put(block, p);
		p.setVisible( true);
		return p;
	}


}
