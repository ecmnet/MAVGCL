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
package org.fxyz.cameras;
// CameraTransformer is based on Xform, a class that extends the Group class. It is used in the 
// MoleculeSampleApp application that is built using the Getting Started with JavaFX
// 3D Graphics tutorial. The method allows you to add your own transforms and rotation.
// 
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public class CameraTransformer extends Group {

    public enum RotateOrder {
        XYZ, XZY, YXZ, YZX, ZXY, ZYX
    }

    public Translate t  = new Translate(); 
    public Translate p  = new Translate(); 
    public Translate ip = new Translate(); 
    public Rotate rx = new Rotate();
    { rx.setAxis(Rotate.X_AXIS); }
    public Rotate ry = new Rotate();
    { ry.setAxis(Rotate.Y_AXIS); }
    public Rotate rz = new Rotate();
    { rz.setAxis(Rotate.Z_AXIS); }
    public Scale s = new Scale();

    public CameraTransformer() { 
        super(); 
        getTransforms().addAll(t, rz, ry, rx, s); 
    }

    public CameraTransformer(CameraTransformer.RotateOrder rotateOrder) { 
        super(); 
        // choose the order of rotations based on the rotateOrder
        switch (rotateOrder) {
        case XYZ:
            getTransforms().addAll(t, p, rz, ry, rx, s, ip); 
            break;
        case XZY:
            getTransforms().addAll(t, p, ry, rz, rx, s, ip); 
            break;
        case YXZ:
            getTransforms().addAll(t, p, rz, rx, ry, s, ip); 
            break;
        case YZX:
            getTransforms().addAll(t, p, rx, rz, ry, s, ip);  // For Camera
            break;
        case ZXY:
            getTransforms().addAll(t, p, ry, rx, rz, s, ip); 
            break;
        case ZYX:
            getTransforms().addAll(t, p, rx, ry, rz, s, ip); 
            break;
        }
    }

    public void setTranslate(double x, double y, double z) {
        t.setX(x);
        t.setY(y);
        t.setZ(z);
    }

    public void setTranslate(double x, double y) {
        t.setX(x);
        t.setY(y);
    }

    public void setTx(double x) { t.setX(x); }
    public void setTy(double y) { t.setY(y); }
    public void setTz(double z) { t.setZ(z); }

    public void setRotate(double x, double y, double z) {
        rx.setAngle(x);
        ry.setAngle(y);
        rz.setAngle(z);
    }

    public void setRotateX(double x) { rx.setAngle(x); }
    public void setRotateY(double y) { ry.setAngle(y); }
    public void setRotateZ(double z) { rz.setAngle(z); }
    public void setRx(double x) { rx.setAngle(x); }
    public void setRy(double y) { ry.setAngle(y); }
    public void setRz(double z) { rz.setAngle(z); }

    public void setScale(double scaleFactor) {
        s.setX(scaleFactor);
        s.setY(scaleFactor);
        s.setZ(scaleFactor);
    }

    public void setScale(double x, double y, double z) {
        s.setX(x);
        s.setY(y);
        s.setZ(z);
    }

    public void setSx(double x) { s.setX(x); }
    public void setSy(double y) { s.setY(y); }
    public void setSz(double z) { s.setZ(z); }

    public void setPivot(double x, double y, double z) {
        p.setX(x);
        p.setY(y);
        p.setZ(z);
        ip.setX(-x);
        ip.setY(-y);
        ip.setZ(-z);
    }

    public void reset() {
        t.setX(0.0);
        t.setY(0.0);
        t.setZ(0.0);
        rx.setAngle(0.0);
        ry.setAngle(0.0);
        rz.setAngle(0.0);
        s.setX(1.0);
        s.setY(1.0);
        s.setZ(1.0);
        p.setX(0.0);
        p.setY(0.0);
        p.setZ(0.0);
        ip.setX(0.0);
        ip.setY(0.0);
        ip.setZ(0.0);
    }

    public void resetTSP() {
        t.setX(0.0);
        t.setY(0.0);
        t.setZ(0.0);
        s.setX(1.0);
        s.setY(1.0);
        s.setZ(1.0);
        p.setX(0.0);
        p.setY(0.0);
        p.setZ(0.0);
        ip.setX(0.0);
        ip.setY(0.0);
        ip.setZ(0.0);
    }

    public void transitionCameraTo(double milliseconds, double tx, double ty, double tz, double rx, double ry, double rz) {
        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame[]{
            new KeyFrame(Duration.millis(milliseconds), new KeyValue[]{// Frame End                
                new KeyValue(xRotateProperty(), rx, Interpolator.EASE_BOTH),
                new KeyValue(yRotateProperty(), ry, Interpolator.EASE_BOTH),
                new KeyValue(zRotateProperty(), rz, Interpolator.EASE_BOTH),
                new KeyValue(xTranslateProperty(), tx, Interpolator.EASE_BOTH),
                new KeyValue(yTranslateProperty(), ty, Interpolator.EASE_BOTH),
                new KeyValue(zTranslateProperty(), tz, Interpolator.EASE_BOTH)
            })
        });
        timeline.playFromStart(); 
    }
    
    private void updateTransforms() {
        t.setX(getxTranslate());
        t.setY(getyTranslate());
        t.setZ(getzTranslate());        
        
        rx.setAngle(getxRotate());
        ry.setAngle(getyRotate());
        rz.setAngle(getzRotate());
    }
    
    private final DoubleProperty xRotate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getxRotate() {  return xRotate.get();   }
    public void setxRotate(double value) { xRotate.set(value);  }
    public DoubleProperty xRotateProperty() { return xRotate;   }    

    private final DoubleProperty yRotate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getyRotate() { return yRotate.get();   }
    public void setyRotate(double value) { yRotate.set(value); }
    public DoubleProperty yRotateProperty() { return yRotate;  }    
    
    private final DoubleProperty zRotate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getzRotate() { return zRotate.get(); }
    public void setzRotate(double value) { zRotate.set(value); }
    public DoubleProperty zRotateProperty() { return zRotate; }    

    
    private final DoubleProperty xTranslate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getxTranslate() {  return xTranslate.get();   }
    public void setxTranslate(double value) { xTranslate.set(value);  }
    public DoubleProperty xTranslateProperty() { return xTranslate;   }    

    private final DoubleProperty yTranslate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getyTranslate() { return yTranslate.get();   }
    public void setyTranslate(double value) { yTranslate.set(value); }
    public DoubleProperty yTranslateProperty() { return yTranslate;  }    
    
    private final DoubleProperty zTranslate = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            updateTransforms();
        }
    };
    public final double getzTranslate() { return zTranslate.get(); }
    public void setzTranslate(double value) { zTranslate.set(value); }
    public DoubleProperty zTranslateProperty() { return zTranslate; }    
}
