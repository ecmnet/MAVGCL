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
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

import org.fxyz.shapes.containers.ShapeContainer;
import org.fxyz.shapes.primitives.TrapezoidMesh;

/**
 *
 * @author Moussaab AMRINE <dy_amrine@esi.dz>
 * @author  Yehya BELHAMRA <dy_belhamra@esi.dz>
 */

public class Trapezoid extends ShapeContainer<TrapezoidMesh> {
	
	private TrapezoidMesh mesh;
	
	public Trapezoid() {
        super(new TrapezoidMesh());
        this.mesh = getShape();
    }
	
	public Trapezoid(double smallSize , double bigSize , double height ,double depth){
        this();
        mesh.setSmallSize(smallSize);
        mesh.setBigSize(bigSize);
        mesh.setheight(height); 
        mesh.setDepth(depth);
    }
	
	public Trapezoid(Color c){
        this();
        this.setDiffuseColor(c);
    }
	
	public Trapezoid(double smallSize , double bigSize , double height ,double depth , Color c){
        super(new TrapezoidMesh(smallSize ,bigSize , height , depth));
        this.mesh = getShape();
        this.setDiffuseColor(c);
    }
	
	public final void setSmallSize(double value) {
        mesh.setSmallSize(value);
    }
	public final void setBigSize(double value) {
        mesh.setBigSize(value);
    }
    public final void setHeight(double value) {
        mesh.setheight(value);
    }
    public final void setDepth(int value) {
        mesh.setDepth(value);
    }
    public final void setMaterial(Material value) {
        mesh.setMaterial(value);
    }
    public final void setDrawMode(DrawMode value) {
        mesh.setDrawMode(value);
    }
    public final void setCullFace(CullFace value) {
        mesh.setCullFace(value);
    }
    public final double getSmallSize() {
        return mesh.getSmallSize();
    }
    public final double getBigSize() {
        return mesh.getBigSize();
    }
    public final double getHeight() {
        return mesh.getHeight();
    }
    public final double getDepth() {
        return mesh.getDepth();
    }
    public DoubleProperty smallSizeProperty() {
        return mesh.sizeSmallProperty();
    }
    public DoubleProperty bigSizeProperty() {
        return mesh.sizeBigProperty();
    }
    public DoubleProperty heightProperty() {
        return mesh.heightProperty();
    }
    public DoubleProperty depthProperty() {
        return mesh.depthProperty();
    }

}
