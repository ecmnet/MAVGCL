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

import java.util.HashMap;
import java.util.Map;

import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Slam;
import com.comino.msp.model.segment.State;
import com.comino.msp.slam.BlockPoint2D;
import com.comino.msp.utils.MSPMathUtils;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

public class XYSLAMBlockAnnotation  implements XYAnnotation {


	private  Pane   	    pane 		= null;
	private  Pane           indicator   = null;
	private  Polygon        direction   = null;
	private  Rotate         rotate      = null;

	private  Slam		  	slam 		= null;
	private  State          state       = null;

	private Map<Integer,Pane> blocks    = null;


	public XYSLAMBlockAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		this.blocks = new HashMap<Integer,Pane>();

		rotate = Rotate.rotate(0, 0, 0);
		direction = new Polygon( -7,30, -1,30, -1,0, 1,0, 1,30, 7,30, 0,40);
		direction.setFill(Color.YELLOW);
		direction.getTransforms().add(rotate);
		direction.setStrokeType(StrokeType.INSIDE);
		direction.setVisible(true);

		pane.getChildren().add(direction);

		indicator = new Pane();
		indicator.setStyle("-fx-background-color: rgba(180.0, 60.0, 100.0, 0.7);; -fx-padding:-1px; -fx-border-color: #606030;");
		indicator.setVisible(false);
		pane.getChildren().add(indicator);
	}

	public void setModel(DataModel model) {
		this.slam  = model.slam;
		this.state = model.state;
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

		if(slam==null || !slam.hasBlocked())
			return;

		slam.getData().forEach((i,b) -> {
			Pane bp = getBlockPane(i,b);
			bp.setLayoutX(xAxis.getDisplayPosition(b.y));
			bp.setLayoutY(yAxis.getDisplayPosition(b.x+slam.getResolution()));
			bp.setPrefSize(xAxis.getDisplayPosition(slam.getResolution())-xAxis.getDisplayPosition(0),
					yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(slam.getResolution()));
			bp.setVisible( true);
		});


		indicator.setPrefSize(xAxis.getDisplayPosition(slam.getResolution())-xAxis.getDisplayPosition(0),
				yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(slam.getResolution()));
		indicator.setLayoutX(xAxis.getDisplayPosition(slam.getIndicatorY()));
		indicator.setLayoutY(yAxis.getDisplayPosition(slam.getIndicatorX()));
		indicator.setVisible(true);

		if(slam.pv != 0) {
			setArrowLength(slam.pv);
			direction.setLayoutX(xAxis.getDisplayPosition(state.l_y));
			direction.setLayoutY(yAxis.getDisplayPosition(state.l_x));
			rotate.angleProperty().set(180+MSPMathUtils.fromRad(slam.pd));
			direction.setVisible(true);
		} else
			direction.setVisible(false);

	}

	public void invalidate() {
		blocks.forEach((i,p) -> {
			p.setVisible(false);
		});
		indicator.setVisible(false);
		direction.setVisible(false);
	}

	public void clear() {
		Platform.runLater(() -> {
			blocks.forEach((i,p) -> {
				pane.getChildren().remove(p);
			});
			indicator.setVisible(false);
			direction.setVisible(false);
		});
		blocks.clear();
	}

	private void setArrowLength(float length) {
		Double k = (double)(length * 30);
		direction.getPoints().set(1,k);
		direction.getPoints().set(3,k);
		direction.getPoints().set(9,k);
		direction.getPoints().set(11,k);
		direction.getPoints().set(13,k+10);
	}


	private Pane getBlockPane(int block, BlockPoint2D b) {

		if(blocks.containsKey(block))
			return blocks.get(block);

		Pane p = new Pane();
		p.setStyle("-fx-background-color: rgba(160.0, 60.0, 100.0, 0.5); -fx-padding:-1px; -fx-border-color: #603030;");
		p.setVisible(false);
		pane.getChildren().add(p);
		blocks.put(block, p);
		return p;
	}


}
