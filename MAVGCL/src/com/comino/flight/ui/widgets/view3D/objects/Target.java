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

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class Target extends Xform {

	private TriangleMesh pyramidMesh = new TriangleMesh();
	private MeshView pyramid = null;

	public Target() {
		super();

		final float h = 6;                      // Height
		final float s = 10;                      // Side

		pyramidMesh.getTexCoords().addAll(0,0);
		pyramidMesh.getPoints().addAll(
				0,    h/2,     0,            // Point 0 - Top
				0,    -h/2,    -s/2,         // Point 1 - Front
				-s/2, -h/2,    0,            // Point 2 - Left
				s/2,  -h/2,    0,            // Point 3 - Back
				0,    -h/2,    s/2           // Point 4 - Right
				);

		pyramidMesh.getFaces().addAll(
				0,0,  2,0,  1,0,          // Front left face
				0,0,  1,0,  3,0,          // Front right face
				0,0,  3,0,  4,0,          // Back right face
				0,0,  4,0,  2,0,          // Back left face
				4,0,  1,0,  2,0,          // Bottom rear face
				4,0,  3,0,  1,0           // Bottom front face
				);

		pyramid = new MeshView(pyramidMesh);

		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.DARKORCHID);
		pyramid.setMaterial(material);
		pyramid.setDrawMode(DrawMode.FILL);
		this.getChildren().addAll(pyramid);
	}

	public void updateState(AnalysisDataModel model) {
		if(model.getValue("SLAMDIS") != 0 && !Double.isNaN(model.getValue("SLAMDIS"))) {
			pyramid.setVisible(true);
			this.setTranslate(-model.getValue("SLAMPY")*100f, model.getValue("SLAMPZ") > 0 ? 0 : -model.getValue("SLAMPZ")*100f, model.getValue("SLAMPX")*100f);
		} else
			pyramid.setVisible(false);
	}

}
