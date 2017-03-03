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

import org.mavlink.messages.MSP_CMD;

import com.comino.flight.observables.StateProperties;
import com.comino.msp.model.segment.Slam;
import com.comino.msp.utils.BlockPoint2D;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;

public class XYSLAMBlockAnnotation  implements XYAnnotation {


	private  Pane   	    pane 		= null;
	private  Pane           vehicle     = null;
	private  Slam		  	slam 		= null;

	private Map<Integer,Pane> blocks    = null;

	private float           scale       = 0;


	public XYSLAMBlockAnnotation() {
		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		this.blocks = new HashMap<Integer,Pane>();

		vehicle = new Pane();
		vehicle.setStyle("-fx-background-color: rgba(60.0, 160.0, 100.0, 0.5);; -fx-padding:-1px; -fx-border-color: #606030;");
		vehicle.setVisible(false);
		pane.getChildren().add(vehicle);

		// Workaraound: Refresh annotations if re-connected
		StateProperties.getInstance().getConnectedProperty().addListener((o,ov,nv) -> {
			if(nv.booleanValue()) {
				Platform.runLater(() -> {
					clear();
				});
			}
		});
	}

	public void set(Slam slam, float scale) {
		this.slam  = slam;
		this.scale = scale;
		slam.getData().forEach((i,b) -> {
			getBlockPane(i,b);
		});
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

		if(slam.count==0) {
			clear();
		}


		slam.getData().forEach((i,b) -> {
			Pane bp = getBlockPane(i,b);
			if(b.x > -scale && b.x < scale && b.y > -scale && b.y < scale) {
				bp.setLayoutX(xAxis.getDisplayPosition(b.y));
				bp.setLayoutY(yAxis.getDisplayPosition(b.x+slam.getResolution()));
				bp.setPrefSize(xAxis.getDisplayPosition(slam.getResolution())-xAxis.getDisplayPosition(0),
						yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(slam.getResolution()));
				bp.setVisible( true);
			}

		});


		vehicle.setPrefSize(xAxis.getDisplayPosition(slam.getResolution())-xAxis.getDisplayPosition(0),
				yAxis.getDisplayPosition(0)-yAxis.getDisplayPosition(slam.getResolution()));;
				vehicle.setLayoutX(xAxis.getDisplayPosition(slam.getVehicleY()));
				vehicle.setLayoutY(yAxis.getDisplayPosition(slam.getVehicleX()));
				vehicle.setVisible(true);
	}

	public void invalidate() {
		blocks.forEach((i,p) -> {
			p.setVisible(false);
		});
		vehicle.setVisible(false);
	}

	public void clear() {
		blocks.forEach((i,p) -> {
			pane.getChildren().remove(p);
		});
		blocks.clear();
		vehicle.setVisible(false);
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
