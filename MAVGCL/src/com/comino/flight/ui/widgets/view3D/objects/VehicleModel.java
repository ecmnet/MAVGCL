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
import com.comino.msp.utils.MSPMathUtils;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

import javafx.scene.shape.MeshView;

public class VehicleModel extends Xform {

	private ObjModelImporter obj = null;
	private MeshView[]      mesh = null;

	public VehicleModel(float scale) {
		super();
		obj = new ObjModelImporter();
		obj.read(this.getClass().getResource("resources/quad_x.obj"));
		mesh = obj.getImport();
		this.setScale(scale);
		this.setRotateX(-90);

	}

	public void updateState(AnalysisDataModel model) {
		this.setTranslate(-model.getValue("LPOSY")*100, model.getValue("LPOSZ") > 0 ? 0 : -model.getValue("LPOSZ") *100, model.getValue("LPOSX")*100);
	//	this.setRotate(-90+MSPMathUtils.fromRad(model.attitude.r), -90+MSPMathUtils.fromRad(model.attitude.y), MSPMathUtils.fromRad(model.attitude.p));
		this.ry.setAngle(180-MSPMathUtils.fromRad(model.getValue("YAW"))+90);
	}

	public void show(boolean show) {
		if(show)
			this.getChildren().addAll(mesh);
		else
			this.getChildren().clear();
	}

}
