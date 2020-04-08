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

import java.util.HashMap;
import java.util.Map;

import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.struct.MapPoint3D_F32;

import georegression.struct.point.Point3D_F32;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.util.Duration;

public class MapGroup extends Xform {

	private final Map<Integer,Box> blocks   	= new HashMap<Integer,Box>();

	private final PhongMaterial mapMaterial	= new PhongMaterial();

	private Timeline 		maptimer 	= null;
	private boolean			mode2D		= false;
	private DataModel		model	    = null;

	public MapGroup(DataModel model) {
		this.model = model;
		mapMaterial.setDiffuseColor(Color.web("#2892b0"));

		maptimer = new Timeline(new KeyFrame(Duration.millis(250), ae -> {
			for(int k=0;k<this.getChildren().size();k++)
				this.getChildren().get(k).setVisible(false);
			model.grid.getData().forEach((i,b) -> {
				getBlockBox(i,b).setVisible(true);;
			});
		} ) );
		maptimer.setCycleCount(Timeline.INDEFINITE);

		this.disabledProperty().addListener((l,o,n) -> {
			if(!n.booleanValue()) {
				maptimer.play();
			} else {
				maptimer.stop();
			}
		});

	}

	public void clear() {
		blocks.forEach((i,p) -> {
			Platform.runLater(() -> {
				this.getChildren().remove(p);
			});
		});
		blocks.clear();
	}

	public void setMode2D(boolean mode2d) {
		this.mode2D = mode2d;
		clear();
	}

	private Box getBlockBox(int block, MapPoint3D_F32 b) {

		if(blocks.containsKey(block))
			return blocks.get(block);

		final Box box = new Box(5, 5, 5);

		box.setTranslateX(-b.y*100);
		if(mode2D)
			box.setTranslateY(-model.state.l_z*100+box.getHeight()/2);
		else
			box.setTranslateY(-b.z*100+box.getHeight()/2);
		box.setTranslateZ(b.x*100);
		box.setMaterial(mapMaterial);

		this.getChildren().addAll(box);
		blocks.put(block,box);

		return box;
	}
}
