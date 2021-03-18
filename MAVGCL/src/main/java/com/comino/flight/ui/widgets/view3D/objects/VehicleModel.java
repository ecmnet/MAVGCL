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
import com.comino.mavutils.MSPMathUtils;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

public class VehicleModel extends Group {

	private ObjModelImporter obj = null;
	private MeshView[]      mesh = null;

	public Rotate rx = new Rotate(0, Rotate.X_AXIS);
	public Rotate ry = new Rotate(0, Rotate.Y_AXIS);
	public Rotate rz = new Rotate(0, Rotate.Z_AXIS);

	private double z_pos        = 0;


	public VehicleModel(float scale) {
		super();
		obj = new ObjModelImporter();
		obj.read(this.getClass().getResource("resources/quad_x.obj"));
		mesh = obj.getImport();
		this.setScaleX(scale);
		this.setScaleY(scale);
		this.setScaleZ(scale);
	}

	public void updateState(AnalysisDataModel model, double z_offset) {
		


		//		model.setValue("LPOSZ", -2.0);
		//		model.setValue("LPOSX", 1.0);
		//		model.setValue("LPOSY", 1.0);
		//		model.setValue("YAW", MSPMathUtils.toRad(45));
		//		model.setValue("ROLL", MSPMathUtils.toRad(10));
		//		model.setValue("PITCH", MSPMathUtils.toRad(0));


		this.getTransforms().clear();

		this.addRotate(this, this.ry, 180-MSPMathUtils.fromRad(model.getValue("YAW"))-90);
		this.addRotate(this, this.rz, 180-MSPMathUtils.fromRad(model.getValue("PITCH")));
		this.addRotate(this, this.rx, MSPMathUtils.fromRad(model.getValue("ROLL"))+90);

		this.setTranslateX(-model.getValue("LPOSY")*100);
//		
//		if(Double.isFinite(z_offset))
		     z_pos =    ( - model.getValue("LPOSZ") - z_offset ) * 100 - 12 ;
//		else
//			 z_pos =  - model.getValue("LPOSZ") * 100  ;
		
//		 z_pos =   model.getValue("ALTRE") * 100  ;


		this.setTranslateY(z_pos < 0 ? 0 : z_pos);
		this.setTranslateZ(model.getValue("LPOSX")*100);


		//	this.setTranslate(-model.getValue("LPOSY")*100, model.getValue("LPOSZ") > 0 ? 0 : -model.getValue("LPOSZ") *100, model.getValue("LPOSX")*100);
		//	this.ry.setAngle(180-MSPMathUtils.fromRad(model.getValue("YAW"))+90);


	}

	public void show(boolean show) {
		if(show)
			this.getChildren().addAll(mesh);
		else
			this.getChildren().clear();
	}

	private void addRotate(Group node, Rotate rotate, double angle) {

		Affine affine = node.getTransforms().isEmpty() ? new Affine() : (Affine)(node.getTransforms().get(0));
		
		double A11 = affine.getMxx(), A12 = affine.getMxy(), A13 = affine.getMxz();
		double A21 = affine.getMyx(), A22 = affine.getMyy(), A23 = affine.getMyz();
		double A31 = affine.getMzx(), A32 = affine.getMzy(), A33 = affine.getMzz();
		

		// rotations over local axis
		Rotate newRotateX = new Rotate(angle, new Point3D(A11, A21, A31));
		Rotate newRotateY = new Rotate(angle, new Point3D(A12, A22, A32));
		Rotate newRotateZ = new Rotate(angle, new Point3D(A13, A23, A33));
		

		// apply rotation
		affine.prepend(rotate.getAxis() == Rotate.X_AXIS ? newRotateX :
			rotate.getAxis() == Rotate.Y_AXIS ? newRotateY : newRotateZ);
		node.getTransforms().setAll(affine);
	}

}
