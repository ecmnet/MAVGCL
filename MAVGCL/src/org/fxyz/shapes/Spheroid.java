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
package org.fxyz.shapes;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import org.fxyz.shapes.containers.ShapeContainer;
import org.fxyz.shapes.primitives.SpheroidMesh;

/**
 *
 * @author jdub1581
 */
public class Spheroid extends ShapeContainer<SpheroidMesh> {
    private SpheroidMesh mesh;
    public Spheroid() {
        super(new SpheroidMesh());
        mesh = getShape();        
    }

    public Spheroid(double radius) {
        this();
        mesh.setMinorRadius( radius);
        mesh.setMajorRadius( radius);
    }

    public Spheroid(double minorRadius, double majorRadius) {
        this();
        mesh.setMinorRadius(minorRadius);
        mesh.setMajorRadius(majorRadius);
    }
    
    public Spheroid(int divisions, double minorRadius, double majorRadius) {
        this();
        mesh.setDivisions(divisions);
        mesh.setMinorRadius(minorRadius);
        mesh.setMajorRadius(majorRadius);
    }
    
    public Spheroid(Color c) {
        this();
        this.setDiffuseColor(c);
    }

    public Spheroid(double radius, Color c) {
        this(radius);
        this.setDiffuseColor(c);
    }

    public Spheroid(double minorRadius, double majorRadius, Color c) {
        this(minorRadius, majorRadius);
        this.setDiffuseColor(c);
    }
    
    public Spheroid(int divisions, double minorRadius, double majorRadius, Color c) {
        this(divisions, minorRadius, majorRadius);
        this.setDiffuseColor(c);
    }

    public final void setMajorRadius(double value) {
        mesh.setMajorRadius(value);
    }

    public final void setMinorRadius(double value) {
        mesh.setMinorRadius(value);
    }

    public final void setDivisions(int value) {
        mesh.setDivisions(value);
    }

    public final void setDrawMode(DrawMode value) {
        mesh.setDrawMode(value);
    }

    public final void setCullFace(CullFace value) {
        mesh.setCullFace(value);
    }

    public boolean isSphere() {
        return mesh.isSphere();
    }

    public boolean isOblateSpheroid() {
        return mesh.isOblateSpheroid();
    }

    public boolean isProlateSpheroid() {
        return mesh.isProlateSpheroid();
    }

    public final double getMajorRadius() {
        return mesh.getMajorRadius();
    }

    public DoubleProperty majorRadiusProperty() {
        return mesh.majorRadiusProperty();
    }

    public final double getMinorRadius() {
        return mesh.getMinorRadius();
    }

    public DoubleProperty minorRadiusProperty() {
        return mesh.minorRadiusProperty();
    }

    public final int getDivisions() {
        return mesh.getDivisions();
    }

    public IntegerProperty divisionsProperty() {
        return mesh.divisionsProperty();
    }
    
    
    
}
