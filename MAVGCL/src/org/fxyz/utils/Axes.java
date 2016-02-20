/*
 * Copyright (C) 2013-2015 F(X)yz,
 * Sean Phillips, Jason Pollastrini and Jose Pereda
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fxyz.utils;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 *
 * @author jpereda
 */
public class Axes extends Group {

    Cylinder axisX = new Cylinder(3, 60);
    Cylinder axisY = new Cylinder(3, 60);
    Cylinder axisZ = new Cylinder(3, 60);

    public Axes() {
        this(1);
    }

    public Axes(double scale) {
        axisX.getTransforms().addAll(new Rotate(90, Rotate.Z_AXIS), new Translate(0, 30, 0));
        axisX.setMaterial(new PhongMaterial(Color.RED));
        axisY.getTransforms().add(new Translate(0, 30, 0));
        axisY.setMaterial(new PhongMaterial(Color.GREEN));
        axisZ.setMaterial(new PhongMaterial(Color.BLUE));
        axisZ.getTransforms().addAll(new Rotate(90, Rotate.X_AXIS), new Translate(0, 30, 0));
        getChildren().addAll(axisX, axisY, axisZ);
        getTransforms().add(new Scale(scale, scale, scale));
    }
    public void setHeight(double equalHeights) {
        double oldHeight = axisX.getHeight();
        axisX.setHeight(equalHeights);
        axisX.getTransforms().add(new Translate(0, (equalHeights/2.0)-(oldHeight/2.0), 0));
        axisY.setHeight(equalHeights);
        axisY.getTransforms().add(new Translate(0, (equalHeights/2.0)-(oldHeight/2.0), 0));
        axisZ.setHeight(equalHeights);
        axisZ.getTransforms().add(new Translate(0,(equalHeights/2.0)-(oldHeight/2.0), 0));
    }
    public void setRadius(double equalRadius) {
        axisX.setRadius(equalRadius);
        axisY.setRadius(equalRadius);
        axisZ.setRadius(equalRadius);

    }
}