/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de.
 *   All rights reserved.
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

package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.model.AnalysisDataModel;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mavutils.MSPMathUtils;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;

public class Camera extends Xform {

	public static final int	OBSERVER_PERSPECTIVE 	= 0;
	public static final int	VEHICLE_PERSPECTIVE 		= 1;
	public static final int	BIRDS_PERSPECTIVE 		= 2;

	private static final double CAMERA_INITIAL_DISTANCE 	= -1000;
	private static final double CAMERA_INITIAL_HEIGHT    	=  -250;
	private static final double CAMERA_INITIAL_X_ANGLE  	=  5.0;
	private static final double CAMERA_INITIAL_Y_ANGLE  	=  0.0;
	private static final double CAMERA_INITIAL_FOV_OBS  	=  35.0;
	private static final double CAMERA_INITIAL_FOV_VCL  	=  60.0;

	private static final double CAMERA_NEAR_CLIP 		= 	0.1;
	private static final double CAMERA_FAR_CLIP 			= 100000.0;

	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Xform cameraXform2 = new Xform();
	private final Xform cameraXform3 = new Xform();

	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 2.0;

	private double mousePosX;
	private double mousePosY;
	private double mouseOldX;
	private double mouseOldY;
	private double mouseDeltaX;
	private double mouseDeltaY;

	private double vv_angle = 0;

	private int perspective;

	public Camera(final SubScene scene) {

		super();

		this.getChildren().add(cameraXform2);
		cameraXform2.getChildren().add(cameraXform3);
		cameraXform3.getChildren().add(camera);

		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);

		scene.setCamera(camera);

		camera.setVisible(true);

		registerHandlers(scene);

	}

	public void setPerspective(int perspective,AnalysisDataModel model) {
		this.perspective = perspective;
		this.vv_angle = 0;
		switch(perspective) {
		case OBSERVER_PERSPECTIVE:
			camera.setTranslateX(0); camera.setTranslateY(CAMERA_INITIAL_HEIGHT);
			camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
			this.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
			this.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
			this.setRotateZ(180.0);
			camera.setFieldOfView(CAMERA_INITIAL_FOV_OBS);
			break;
		case VEHICLE_PERSPECTIVE:
			camera.setTranslateX(0); camera.setTranslateY(0); camera.setTranslateZ(0);
			camera.setFieldOfView(CAMERA_INITIAL_FOV_VCL);
			break;
		}
		updateState(model);
	}

	public void updateState(AnalysisDataModel model) {
		this.setTranslate(-model.getValue("LPOSY")*100, model.getValue("LPOSZ") > -0.05 ? 5 : -model.getValue("LPOSZ") *100, model.getValue("LPOSX")*100);
		this.ry.setAngle(MSPMathUtils.fromRad(model.getValue("YAW")));

		if(perspective==VEHICLE_PERSPECTIVE) {
			this.rz.setAngle(-MSPMathUtils.fromRad(model.getValue("ROLL"))+180);
			this.rx.setAngle(MSPMathUtils.fromRad(model.getValue("PITCH"))+vv_angle);
		}
	}

	public void setFieldOfView(double fov) {
		camera.setFieldOfView(100-fov/2+10);
	}

	private void registerHandlers(final Node node) {

		node.setOnScroll(event -> {
			if(perspective!=VEHICLE_PERSPECTIVE) {
				camera.setTranslateX(camera.getTranslateX() - event.getDeltaX()*MOUSE_SPEED);
				camera.setTranslateY(camera.getTranslateY() - event.getDeltaY()*MOUSE_SPEED);
			}
		});

		node.setOnMousePressed((me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});

		node.setOnMouseDragged((me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = -(mousePosX - mouseOldX);
			mouseDeltaY = -(mousePosY - mouseOldY);

			double modifier = 1.0;

			if (me.isControlDown()) {
				modifier = CONTROL_MULTIPLIER;
			}
			if (me.isShiftDown()) {
				modifier = SHIFT_MULTIPLIER;
			}

			if (me.isPrimaryButtonDown()) {
				switch(perspective) {
				case OBSERVER_PERSPECTIVE:
					this.ry.setAngle(this.ry.getAngle() - mouseDeltaX*MOUSE_SPEED*modifier*ROTATION_SPEED);  //
					this.rx.setAngle(this.rx.getAngle() + mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED);  // -
					break;
				case VEHICLE_PERSPECTIVE:
					vv_angle +=  mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED;

				}
			}
		});

		node.setOnMouseClicked((me) -> {
			if(me.getClickCount()==2) {
				vv_angle = 0;
				switch(perspective) {
				case OBSERVER_PERSPECTIVE:
					camera.setTranslateX(0); camera.setTranslateY(0);
					camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
					this.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
					this.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
					this.setRotateZ(180.0);
					camera.setFieldOfView(CAMERA_INITIAL_FOV_OBS);
					break;
				case VEHICLE_PERSPECTIVE:
					camera.setTranslateX(0); camera.setTranslateY(0);
					camera.setTranslateZ(0);
					this.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
					camera.setFieldOfView(CAMERA_INITIAL_FOV_VCL);
					break;
				}

			}
		});
	}

}
