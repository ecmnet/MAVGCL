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

import com.comino.flight.model.AnalysisDataModel;
import com.comino.msp.utils.MSPMathUtils;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class XYSlamAnnotation  implements XYAnnotation {


	private  Pane   	  			  pane 		= null;
	private  Polygon        		direction   = null;
	private  Rotate       		    rotate      = null;
	private  AnalysisDataModel      model       = null;

	public XYSlamAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		rotate = Rotate.rotate(0, 0, 0);
		direction = new Polygon( -7,30, -1,30, -1,0, 1,0, 1,30, 7,30, 0,40);
		direction.setFill(Color.YELLOW);
		direction.getTransforms().add(rotate);
		direction.setStrokeType(StrokeType.INSIDE);
		direction.setVisible(false);

		pane.getChildren().add(direction);
	}

	public void setModel(AnalysisDataModel model) {
		this.model = model;
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(model==null)
			return;

		if(model.getValue("SLAMSPD") != 0) {
			setArrowLength(model.getValue("SLAMSPD"));
			direction.setLayoutX(xAxis.getDisplayPosition(model.getValue("LPOSY")));
			direction.setLayoutY(yAxis.getDisplayPosition(model.getValue("LPOSX")));
			rotate.angleProperty().set(180+MSPMathUtils.fromRad(model.getValue("SLAMDIR")));
			direction.setVisible(true);
		} else
			direction.setVisible(false);

	}

	private void setArrowLength(float length) {
		Double k = (double)(length * 30);
		direction.getPoints().set(1,k);
		direction.getPoints().set(3,k);
		direction.getPoints().set(9,k);
		direction.getPoints().set(11,k);
		direction.getPoints().set(13,k+10);
	}
}
