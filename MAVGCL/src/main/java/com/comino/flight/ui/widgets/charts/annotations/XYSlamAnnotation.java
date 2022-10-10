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
import com.comino.flight.prefs.MAVPreferences;
import com.comino.mavcom.model.segment.Vision;
import com.comino.mavutils.MSPMathUtils;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class XYSlamAnnotation  implements XYAnnotation {

	private static final int SIZE 		= 4;
	private static final int SIZE_OBS 	= 3;


	private  Pane   	  			pane 	    = null;
	private  Polygon        		act_dir     = null;
	private  Polygon        		plan_dir    = null;
	private  Polyline        		lock        = null;
	private  Rotate       		    act_rotate  = null;
	private  Rotate       		    plan_rotate = null;
	private  Rotate       		    lock_rotate = null;
	private  Polygon  				vhc         = null;
	private  Rotate  				vhc_rotate  = null;
	private  Circle                 obstacle    = null;

	private  AnalysisDataModel      model        = null;
	private boolean enable_slam = true;

	public XYSlamAnnotation(Color color) {

		this.pane = new Pane();
		this.pane.setMaxWidth(999); this.pane.setMaxHeight(999);
		this.pane.setLayoutX(0); this.pane.setLayoutY(0);

		plan_rotate = Rotate.rotate(0, 0, 0);
		plan_dir = new Polygon( -4,30, -1,30, -1,0, 1,0, 1,30, 4,30, 0,35);
		plan_dir.setFill(Color.rgb(230, 230, 20, 0.6));
		if(MAVPreferences.isLightTheme())
			plan_dir.setFill(Color.valueOf(plan_dir.getFill().toString()).darker());
		plan_dir.getTransforms().add(plan_rotate);
		plan_dir.setStrokeType(StrokeType.INSIDE);
		plan_dir.setVisible(false);

		act_rotate = Rotate.rotate(0, 0, 0);
		act_dir = new Polygon( -4,30, -1,30, -1,0, 1,0, 1,30, 4,30, 0,35);
		act_dir.setFill(Color.DARKRED);
		act_dir.getTransforms().add(act_rotate);
		act_dir.setStrokeType(StrokeType.INSIDE);
		act_dir.setVisible(false);
		
		lock_rotate = Rotate.rotate(0, 0, 0);
//		lock = new Polyline(-6,-3,6,-3,0,12,-6,-3);
		lock = new Polyline(-6,0,6,0,0,0,0,6,0,-6);
		lock.getTransforms().add(lock_rotate);
		lock = new Polyline(-6,0,6,0,0,0,0,6,0,-6);
		lock.setStroke(Color.DARKBLUE);
		lock.setVisible(false);

		vhc_rotate = Rotate.rotate(0, 0, 0);
		vhc = new Polygon( -7,-2, -1,1, 1,1, 7,-2, 0,12);
		vhc.getTransforms().add(vhc_rotate);
		vhc.setFill(color.brighter().brighter().brighter());
		vhc.setStrokeType(StrokeType.INSIDE);

		this.obstacle = new Circle();
		this.obstacle.setCenterX(SIZE_OBS/2);
		this.obstacle.setCenterY(SIZE_OBS/2);
		this.obstacle.setRadius(SIZE_OBS);
		this.obstacle.setFill(Color.TRANSPARENT);
		this.obstacle.setStroke(Color.RED);
		this.obstacle.setVisible(enable_slam);

		pane.getChildren().addAll(act_dir,plan_dir, vhc, obstacle, lock);
	}

	public void setModel(AnalysisDataModel model) {
		this.model = model;
	}


	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {

		if(model==null)
			return;

		vhc.setLayoutX(xAxis.getDisplayPosition(model.getValue("LPOSY")));
		vhc.setLayoutY(yAxis.getDisplayPosition(model.getValue("LPOSX")));
		vhc_rotate.angleProperty().set(180+MSPMathUtils.fromRad(model.getValue("YAW")));


		if(model.getValue("SLAMSPD") != 0 && !Double.isNaN(model.getValue("SLAMSPD"))) {
			setArrowLength(plan_dir,(float)model.getValue("SLAMSPD")*50);
			plan_dir.setLayoutX(xAxis.getDisplayPosition(model.getValue("LPOSY")));
			plan_dir.setLayoutY(yAxis.getDisplayPosition(model.getValue("LPOSX")));
			plan_rotate.angleProperty().set(180+MSPMathUtils.fromRad(model.getValue("SLAMDIR")));
			plan_dir.setVisible(true);


		} else {
			plan_dir.setVisible(false);

		}
		
		if((((int)model.getValue("VISIONFLAGS")) & 1 << Vision.FIDUCIAL_LOCKED ) == 1 << Vision.FIDUCIAL_LOCKED) {
			
			lock_rotate.angleProperty().set(180+model.getValue("PRECLOCKW"));
			lock.setLayoutX(xAxis.getDisplayPosition(model.getValue("PRECLOCKY")));
			lock.setLayoutY(yAxis.getDisplayPosition(model.getValue("PRECLOCKX")));
			lock.setVisible(true);
		} else
			lock.setVisible(false);
			

		if(model.getValue("SLAMOBX") != 0 && model.getValue("SLAMOBY") != 0 ) {

			obstacle.setLayoutX(xAxis.getDisplayPosition(model.getValue("SLAMOBY"))-SIZE_OBS/2f);
			obstacle.setLayoutY(yAxis.getDisplayPosition(model.getValue("SLAMOBX"))-SIZE_OBS/2f);
			obstacle.setVisible(enable_slam);
		} else
			obstacle.setVisible(false);

		//		setArrowLength(act_dir,model.getValue("GNDV"));
		//		act_dir.setLayoutX(xAxis.getDisplayPosition(model.getValue("LPOSY")));
		//		act_dir.setLayoutY(yAxis.getDisplayPosition(model.getValue("LPOSX")));
		//		act_rotate.angleProperty().set(180+MSPMathUtils.fromRad(model.getValue("YAW")));
//		act_dir.setVisible(true);


	}
	
	public void enableSLAM(boolean enable) {
		this.enable_slam  = enable;
		
	}

	public void clear() {
		Platform.runLater(() -> {
			act_dir.setVisible(false);
			plan_dir.setVisible(false);
		});
	}

	private void setArrowLength(Polygon p, double k) {
		p.getPoints().set(1,k);
		p.getPoints().set(3,k);
		p.getPoints().set(9,k);
		p.getPoints().set(11,k);
		p.getPoints().set(13,k+10);
	}
}
