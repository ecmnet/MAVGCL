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

package com.comino.mav3d;

import com.comino.msp.model.DataModel;
import com.comino.msp.utils.MSPMathUtils;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

public class VehicleModel extends Xform{

	private ObjModelImporter obj = null;
	private float           scale = 0;

	public VehicleModel(float scale) {
		super();
		this.scale = scale;
		obj = new ObjModelImporter();
		obj.read(this.getClass().getResource("res/quad_x.obj"));
		this.getChildren().addAll(obj.getImport());
		this.setScale(scale);
		this.setRotateX(-90);
	}

	public void updateState(DataModel model) {
		this.setTranslate(model.state.l_y*scale, -model.state.l_z*scale, model.state.l_x*scale);
		this.setRotate(-90+MSPMathUtils.fromRad(model.attitude.r), -90+MSPMathUtils.fromRad(model.attitude.y), MSPMathUtils.fromRad(model.attitude.p));
	}
}
