/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.widgets.charts.xy;

import java.util.ArrayList;
import java.util.List;

import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Slam;
import com.comino.msp.utils.BlockPoint3D;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class XYSLAMBlockAnnotation  implements XYAnnotation {

	private static final int MAXPANES = 50;

	private  Pane   	    pane 		= null;
	private  Slam		  	slam 		= null;


	public XYSLAMBlockAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);
		for(int i=0;i<MAXPANES;i++) {
			Pane p = new Pane();
			p.setStyle("-fx-background-color: rgba(60.0, 60.0, 100.0, 0.5); -fx-padding:-1px; -fx-border-color: #603030;");
			p.setVisible(false);
			pane.getChildren().add(i,p);
		}
	}

	public void set(Slam slam) {
		this.slam = slam;
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		BlockPoint3D p; double ext;

		for(int i=0;i<MAXPANES;i++)
			pane.getChildren().get(i).setVisible(false);

		if(slam==null)
			return;

		List<BlockPoint3D> blocks = slam.getBlocks();
		if(blocks!=null) {
			for(int i=0; i<blocks.size() && i < MAXPANES;i++) {
				p = blocks.get(i);
				ext = xAxis.getDisplayPosition(p.res)-xAxis.getDisplayPosition(0);
				pane.getChildren().get(i).setLayoutX(xAxis.getDisplayPosition(p.y-p.res/2f));
				pane.getChildren().get(i).setLayoutY(yAxis.getDisplayPosition(p.x+p.res/2f));
				((Pane)pane.getChildren().get(i)).setPrefSize(xAxis.getDisplayPosition(p.res)-xAxis.getDisplayPosition(0),
						yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(p.res));;
				pane.getChildren().get(i).setVisible(true);
			}
		}
	}

	public void invalidate() {
		slam=null;
		for(int i=0;i<MAXPANES;i++)
			pane.getChildren().get(i).setVisible(false);
	}


}
