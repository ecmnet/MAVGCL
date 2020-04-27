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
import java.util.Map;

import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.struct.MapPoint3D_F32;
import com.emxsys.chart.extension.XYAnnotation;

import georegression.struct.point.Point3D_F32;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;

public class XYGridAnnotation  implements XYAnnotation {


	private  Pane   	    pane 		= null;
	private  Pane           indicator   = null;

	private Map<Integer,Pane> blocks    = null;
	private DataModel         model     = null;

	private boolean        enabled = false;


	public XYGridAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		this.blocks = new HashMap<Integer,Pane>();

//		indicator = new Pane();
//		indicator.setStyle("-fx-background-color: rgba(180.0, 60.0, 100.0, 0.7);; -fx-padding:-1px; -fx-border-color: #606030;");
//		indicator.setVisible(false);
//		indicator.setCache(true);
//		indicator.setCacheHint(CacheHint.SPEED);
//		pane.getChildren().add(indicator);
	}

	public void setModel(DataModel model) {
		this.model = model;
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		for(int i=0;i<pane.getChildren().size();i++)
			pane.getChildren().get(i).setVisible(false);

		if(model == null || model.grid==null || !model.grid.hasBlocked() || !enabled)
			return;

		model.grid.getData().forEach((i,b) -> {

			if( (b.x==0 && b.y==0))
				return;

			// TODO: Z Plane filter (selectable)

			Pane bp = getBlockPane(i,b);
			bp.setLayoutX(xAxis.getDisplayPosition(b.y));
			bp.setLayoutY(yAxis.getDisplayPosition(b.x+model.grid.getResolution()));
			bp.setPrefSize(xAxis.getDisplayPosition(model.grid.getResolution())-xAxis.getDisplayPosition(0),
					yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(model.grid.getResolution()));
			bp.setVisible( true);
		});
	}

	public void invalidate(boolean enable) {
		blocks.forEach((i,p) -> {
			p.setVisible(false);
		});
		enabled = enable;
	}

	public void clear() {
		blocks.forEach((i,p) -> {
			Platform.runLater(() -> {
				pane.getChildren().remove(p);
			});
		});
		blocks.clear();
	}


	private Pane getBlockPane(int block, MapPoint3D_F32 b) {

		if(blocks.containsKey(block))
			return blocks.get(block);

		Pane p = new Pane();
		p.setStyle("-fx-background-color: rgba(38, 136, 163, 0.5); -fx-padding:-1px; -fx-border-color: #20738a;");
		p.setVisible(false);
		pane.getChildren().add(p);
		blocks.put(block, p);
		return p;
	}


}
