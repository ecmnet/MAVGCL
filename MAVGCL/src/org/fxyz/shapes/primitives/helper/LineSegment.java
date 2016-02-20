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
package org.fxyz.shapes.primitives.helper;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.Path;
import org.fxyz.geometry.Point3D;

/**
 *
 * @author José Pereda 
 */
public class LineSegment {

    /*
    Given one single character in terms of Path, LineSegment stores a list of points that define 
    the exterior of one of its polygons (!isHole). It can contain reference to one or several 
    holes inside this polygon.
    Or it can define the perimeter of a hole (isHole), with no more holes inside.
    */
    
    private boolean hole;
    private List<Point3D> points;
    private Path path;
    private Point3D origen;   
    private List<LineSegment> holes=new ArrayList<>();
    private String letter;

    public LineSegment(String text) {
        letter=text;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public boolean isHole() {
        return hole;
    }

    public void setHole(boolean isHole) {
        this.hole = isHole;
    }

    public List<Point3D> getPoints() {
        return points;
    }

    public void setPoints(List<Point3D> points) {
        this.points = points;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Point3D getOrigen() {
        return origen;
    }

    public void setOrigen(Point3D origen) {
        this.origen = origen;
    }

    public List<LineSegment> getHoles() {
        return holes;
    }

    public void setHoles(List<LineSegment> holes) {
        this.holes = holes;
    }

    public void addHole(LineSegment hole) {
        holes.add(hole);
    }

    @Override
    public String toString() {
        return "Poly{" + "points=" + points + ", path=" + path + ", origen=" + origen + ", holes=" + holes + '}';
    }
}
