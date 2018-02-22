/*
 * Copyright (c) 2014 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.airseries;

import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;


/**
 * User: hansolo
 * Date: 07.04.14
 * Time: 07:51
 */
public class Horizon extends Region {
    private static final double ANIMATION_DURATION = 800;

    private static final double PREFERRED_WIDTH  = 320;
    private static final double PREFERRED_HEIGHT = 320;
    private static final double MINIMUM_WIDTH    = 5;
    private static final double MINIMUM_HEIGHT   = 5;
    private static final double MAXIMUM_WIDTH    = 1024;
    private static final double MAXIMUM_HEIGHT   = 1024;
    private double                size;
    private double                width;
    private double                height;
    private Region                background;
    private Canvas                horizonCanvas;
    private GraphicsContext       horizonCtx;
    private Canvas                indicatorCanvas;
    private GraphicsContext       indicatorCtx;
    private Pane                  pane;

    private Path                  arrow;
    private MoveTo                moveTo;
    private LineTo                lineTo;
    private HLineTo               hLineTo;

    private DoubleProperty        roll;
    private DoubleProperty        pitch;
    private double                pitchPixel;
//    private BooleanProperty       upsidedown;
    private ObjectProperty<Color> skyColor;
    private ObjectProperty<Color> earthColor;
    private ObjectProperty<Color> indicatorColor;

    private Timeline              timelineRoll;
    private Timeline              timelinePitch;
    private String                fontName;

    private Rotate                horizonRotate;
    private Affine                horizonAffine;
    private DoubleProperty        currentPitch;


    // ******************** Constructors **************************************
    public Horizon() {
        getStylesheets().add(getClass().getResource("horizon.css").toExternalForm());
        getStyleClass().add("horizon");
        roll = new DoublePropertyBase(0) {
            @Override public void set(final double ROLL) { super.set(360 - ROLL % 360); }
            @Override public Object getBean() { return Horizon.this; }
            @Override public String getName() { return "roll"; }
        };
        pitch = new DoublePropertyBase(0) {
            @Override public void set(final double PITCH) {
                double pitch = 0;
                if(PITCH>180)
                  pitch = ( PITCH - 360) % 180;
                else
                	 pitch = PITCH % 180;
                super.set(pitch);
            }
            @Override public Object getBean() { return Horizon.this; }
            @Override public String getName() { return "pitch"; }
        };

        skyColor         = new SimpleObjectProperty<>(this, "skyColor", Color.rgb(127, 213, 240));
        earthColor       = new SimpleObjectProperty<>(this, "earthColor", Color.rgb(60, 68, 57));
        indicatorColor   = new SimpleObjectProperty<>(this, "indicatorcolor", Color.web("#fd7e24"));
        horizonRotate    = new Rotate(0, Rotate.Z_AXIS);
        horizonAffine    = new Affine();
        currentPitch     = new SimpleDoubleProperty(this, "currentPitch", 0);
        timelineRoll     = new Timeline();
        timelinePitch    = new Timeline();
        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        if (Double.compare(getMinWidth(), 0.0) <= 0 || Double.compare(getMinHeight(), 0.0) <= 0) {
            setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getMaxWidth(), 0.0) <= 0 || Double.compare(getMaxHeight(), 0.0) <= 0) {
            setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        fontName      = Font.loadFont(Horizon.class.getResourceAsStream("Verdana.ttf"), 10).getName();

        background    = new Region();
        background.getStyleClass().add("background");
        background.setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);

        horizonCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        horizonCtx    = horizonCanvas.getGraphicsContext2D();
        horizonCanvas.getTransforms().addAll(horizonRotate);

        indicatorCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        indicatorCtx    = indicatorCanvas.getGraphicsContext2D();

        moveTo  = new MoveTo(0.5 * PREFERRED_WIDTH, 0.1037850467 * PREFERRED_WIDTH);
        lineTo  = new LineTo(0.5 * PREFERRED_WIDTH + 0.0175 * PREFERRED_WIDTH, 0.1037850467 * size + 0.055 * PREFERRED_WIDTH);
        hLineTo = new HLineTo(0.5 * PREFERRED_WIDTH - 0.0175 * PREFERRED_WIDTH);
        arrow   = new Path();
        arrow.getElements().addAll(moveTo, lineTo, hLineTo, new ClosePath());
        arrow.setFill(getIndicatorColor());
        arrow.setStroke(getIndicatorColor().darker());
        arrow.getTransforms().add(horizonRotate);

        pane = new Pane(background, horizonCanvas, indicatorCanvas, arrow);
        pane.getStyleClass().add("frame");

        getChildren().setAll(pane);
        resize();
    }

    private void registerListeners() {
        widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        rollProperty().addListener(observable -> handleControlPropertyChanged("ROLL"));
        pitchProperty().addListener(observable -> handleControlPropertyChanged("PITCH"));
    }


    // ******************** Methods *******************************************
    private void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("ROLL".equals(PROPERTY)) {
                horizonRotate.setAngle(getRoll());
        } else if ("PITCH".equals(PROPERTY)) {
                currentPitch.set(getPitch());
                resize();
        }
    }

    public final double getRoll() { return roll.get(); }
    public final void setRoll(final double ROLL) { roll.set(ROLL); }
    public final DoubleProperty rollProperty() { return roll; }

    public final double getPitch() { return pitch.get(); }
    public final void setPitch(final double PITCH) { pitch.set(PITCH); }
    public final DoubleProperty pitchProperty() { return pitch; }

    public final Color getSkyColor() { return skyColor.get(); }
    public final void setSkyColor(final Color SKY_COLOR) {
        indicatorColor.set(SKY_COLOR);
        resize();
    }
    public final ObjectProperty<Color> skyColorProperty() { return skyColor; }

    public final Color getEarthColor() { return earthColor.get(); }
    public final void setEarthColor(final Color EARTH_COLOR) {
        indicatorColor.set(EARTH_COLOR);
        resize();
    }
    public final ObjectProperty<Color> earthColorProperty() { return earthColor; }

    public final Color getIndicatorColor() { return indicatorColor.get(); }
    public final void setIndicatorColor(final Color INDICATOR_COLOR) {
        indicatorColor.set(INDICATOR_COLOR);
        resize();
    }
    public final ObjectProperty<Color> indicatorColorProperty() { return indicatorColor; }

    private Font getRegularFontAt(final double SIZE) { return Font.font(fontName, FontWeight.NORMAL, SIZE); }
    private Font getBoldFontAt(final double SIZE) { return Font.font(fontName, FontWeight.BOLD, SIZE); }


    // ******************** Resizing ******************************************
    private void drawHorizonCanvas() {
        horizonCtx.clearRect(0, 0, size, horizonCanvas.getHeight());

        pitchPixel = Math.PI * size / 360;
        horizonAffine.setTy(pitchPixel * currentPitch.get());
        horizonCtx.setTransform(horizonAffine);

        // Draw background with sky and earth
        horizonCtx.save();
        horizonCtx.setFill(new LinearGradient(0, 0, 0, horizonCanvas.getHeight(), false, CycleMethod.NO_CYCLE,
                                              new Stop(0.0, getSkyColor()),
                                              new Stop(0.5, getSkyColor()),
                                              new Stop(0.5, getEarthColor()),
                                              new Stop(1.0, getEarthColor())));
        horizonCtx.translate(0, -0.5 * (horizonCanvas.getHeight() - size));
        horizonCtx.fillRect(0, 0, size, horizonCanvas.getHeight());
        // Draw horizontal lines
        horizonCtx.setFill(getSkyColor().deriveColor(0, 1, 0.5, 1));
        horizonCtx.setStroke(getSkyColor().deriveColor(0, 1, 0.5, 1));
        double  stepSizeY = horizonCanvas.getHeight() / 360 * 5;
        boolean stepTen   = false;
        int     step      = 0;
        horizonCtx.setFont(getRegularFontAt(0.035 * size));
        horizonCtx.setTextBaseline(VPos.CENTER);
        horizonCtx.setTextAlign(TextAlignment.RIGHT);
        for (double y = 0.5 * horizonCanvas.getHeight() - stepSizeY ; y > 0 ; y -= stepSizeY) {
            if (step <= 80) {
                if (stepTen) {
                    horizonCtx.strokeLine(size * 0.4, y, size * 0.6, y);
                    step += 10;
                    horizonCtx.fillText(Integer.toString(step), size * 0.35, y);
                    horizonCtx.fillText(Integer.toString(step), size * 0.7, y);
                } else {
                    horizonCtx.strokeLine(size * 0.45, y, size * 0.55, y);
                }
            }
            stepTen ^= true;
        }

        horizonCtx.setFill(Color.WHITE);
        horizonCtx.setStroke(Color.WHITE);

        horizonCtx.strokeLine(0, 0.5 * horizonCanvas.getHeight(), size, 0.5 * horizonCanvas.getHeight());

        stepTen = false;
        step = 0;
        for (double y = 0.5 * horizonCanvas.getHeight() + stepSizeY ; y <= horizonCanvas.getHeight() ; y += stepSizeY) {
            if (step >= -80) {
                if (stepTen) {
                    horizonCtx.strokeLine(size * 0.4, y, size * 0.6, y);
                    step -= 10;
                    horizonCtx.fillText(Integer.toString(step), size * 0.35, y);
                    horizonCtx.fillText(Integer.toString(step), size * 0.7, y);
                } else {
                    horizonCtx.strokeLine(size * 0.45, y, size * 0.55, y);
                }
            }
            stepTen ^= true;
        }
        horizonCtx.translate(0, 0.5 * (horizonCanvas.getHeight() - size));
        horizonCtx.restore();
    }

    private void drawIndicatorCanvas() {
        indicatorCtx.clearRect(0, 0, indicatorCanvas.getWidth(), indicatorCanvas.getHeight());

        final Point2D OUTER_POINT         = new Point2D(0.5 * size, 0.08878504672897196 * size);
        final Point2D INNER_POINT_SMALL   = new Point2D(0.5 * size, 0.0937850467 * size);
        final Point2D INNER_POINT_MEDIUM  = new Point2D(0.5 * size, 0.1037850467 * size);
        final Point2D INNER_POINT_BIG     = new Point2D(0.5 * size, 0.113 * size);
        final double  SMALL_STROKE_WIDTH  = 0.0025 * size;
        final double  MEDIUM_STROKE_WIDTH = 0.005 * size;
        final double  BIG_STROKE_WIDTH    = 0.01 * size;
        final int     STEP                = 5;

        indicatorCtx.setLineJoin(StrokeLineJoin.ROUND);
        indicatorCtx.setLineCap(StrokeLineCap.ROUND);

        indicatorCtx.save();
        // PreRotate the GraphicsContext
        indicatorCtx.translate(0.5 * size, 0.5 * size);
        indicatorCtx.rotate(-90);
        indicatorCtx.translate(-0.5 * size, -0.5 * size);

        for (int angle = 0; angle < 185; angle += STEP) {
            if (angle % 45 == 0 || angle == 0) {
                indicatorCtx.setStroke(getIndicatorColor());
                indicatorCtx.setLineWidth(BIG_STROKE_WIDTH);
                indicatorCtx.strokeLine(OUTER_POINT.getX(), OUTER_POINT.getY(), INNER_POINT_BIG.getX(), INNER_POINT_BIG.getY());
            } else if (angle % 15 == 0) {
                indicatorCtx.setStroke(Color.WHITE);
                indicatorCtx.setLineWidth(MEDIUM_STROKE_WIDTH);
                indicatorCtx.strokeLine(OUTER_POINT.getX(), OUTER_POINT.getY(), INNER_POINT_MEDIUM.getX(), INNER_POINT_MEDIUM.getY());
            } else {
                indicatorCtx.setStroke(Color.WHITE);
                indicatorCtx.setLineWidth(SMALL_STROKE_WIDTH);
                indicatorCtx.strokeLine(OUTER_POINT.getX(), OUTER_POINT.getY(), INNER_POINT_SMALL.getX(), INNER_POINT_SMALL.getY());
            }
            indicatorCtx.translate(0.5 * size, 0.5 * size);
            indicatorCtx.rotate(STEP);
            indicatorCtx.translate(-0.5 * size, -0.5 * size);
        }
        indicatorCtx.restore();

        indicatorCtx.setFillRule(FillRule.EVEN_ODD);
        indicatorCtx.beginPath();

        indicatorCtx.moveTo(size * 0.4766355140186916, size * 0.5);
        indicatorCtx.bezierCurveTo(size * 0.4766355140186916, size * 0.514018691588785, size * 0.48598130841121495, size * 0.5233644859813084, size * 0.5, size * 0.5233644859813084);
        indicatorCtx.bezierCurveTo(size * 0.514018691588785, size * 0.5233644859813084, size * 0.5233644859813084, size * 0.514018691588785, size * 0.5233644859813084, size * 0.5);
        indicatorCtx.bezierCurveTo(size * 0.5233644859813084, size * 0.48598130841121495, size * 0.514018691588785, size * 0.4766355140186916, size * 0.5, size * 0.4766355140186916);
        indicatorCtx.bezierCurveTo(size * 0.48598130841121495, size * 0.4766355140186916, size * 0.4766355140186916, size * 0.48598130841121495, size * 0.4766355140186916, size * 0.5);
        indicatorCtx.closePath();
        indicatorCtx.moveTo(size * 0.4158878504672897, size * 0.5046728971962616);
        indicatorCtx.lineTo(size * 0.4158878504672897, size * 0.4953271028037383);
        indicatorCtx.bezierCurveTo(size * 0.4158878504672897, size * 0.4953271028037383, size * 0.4672897196261682, size * 0.4953271028037383, size * 0.4672897196261682, size * 0.4953271028037383);
        indicatorCtx.bezierCurveTo(size * 0.4719626168224299, size * 0.48130841121495327, size * 0.48130841121495327, size * 0.4719626168224299, size * 0.4953271028037383, size * 0.4672897196261682);
        indicatorCtx.bezierCurveTo(size * 0.4953271028037383, size * 0.4672897196261682, size * 0.4953271028037383, size * 0.4158878504672897, size * 0.4953271028037383, size * 0.4158878504672897);
        indicatorCtx.lineTo(size * 0.5046728971962616, size * 0.4158878504672897);
        indicatorCtx.bezierCurveTo(size * 0.5046728971962616, size * 0.4158878504672897, size * 0.5046728971962616, size * 0.4672897196261682, size * 0.5046728971962616, size * 0.4672897196261682);
        indicatorCtx.bezierCurveTo(size * 0.5186915887850467, size * 0.4719626168224299, size * 0.5280373831775701, size * 0.48130841121495327, size * 0.5327102803738317, size * 0.4953271028037383);
        indicatorCtx.bezierCurveTo(size * 0.5327102803738317, size * 0.4953271028037383, size * 0.5841121495327103, size * 0.4953271028037383, size * 0.5841121495327103, size * 0.4953271028037383);
        indicatorCtx.lineTo(size * 0.5841121495327103, size * 0.5046728971962616);
        indicatorCtx.bezierCurveTo(size * 0.5841121495327103, size * 0.5046728971962616, size * 0.5327102803738317, size * 0.5046728971962616, size * 0.5327102803738317, size * 0.5046728971962616);
        indicatorCtx.bezierCurveTo(size * 0.5280373831775701, size * 0.5186915887850467, size * 0.5186915887850467, size * 0.5327102803738317, size * 0.5, size * 0.5327102803738317);
        indicatorCtx.bezierCurveTo(size * 0.48130841121495327, size * 0.5327102803738317, size * 0.4719626168224299, size * 0.5186915887850467, size * 0.4672897196261682, size * 0.5046728971962616);
        indicatorCtx.bezierCurveTo(size * 0.4672897196261682, size * 0.5046728971962616, size * 0.4158878504672897, size * 0.5046728971962616, size * 0.4158878504672897, size * 0.5046728971962616);
        indicatorCtx.closePath();
        indicatorCtx.setFill(getIndicatorColor());
        indicatorCtx.fill();
    }

    private void resize() {
        size   = getWidth() < getHeight() ? getWidth() : getHeight();
        width  = getWidth();
        height = getHeight();

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.relocate((width - size) * 0.5, (height - size) * 0.5);

            background.setPrefSize(size, size);
            background.relocate((size - background.getPrefWidth()) * 0.5, (size - background.getPrefHeight()) * 0.5);
            background.setClip(new Circle(0.5 * size, 0.5 * size, size * 0.45));

            horizonRotate.setPivotX(size * 0.5);
            horizonRotate.setPivotY(size * 0.5);

            horizonCanvas.setWidth(size);
            horizonCanvas.setHeight(Math.PI * size);
            horizonCanvas.setClip(new Circle(0.5 * size, 0.5 * size, size * 0.45));

            indicatorCanvas.setWidth(size);
            indicatorCanvas.setHeight(size);

            drawHorizonCanvas();
            drawIndicatorCanvas();

            moveTo.setX(0.5 * size);
            moveTo.setY(0.1037850467 * size);
            lineTo.setX(0.5 * size + 0.0175 * size);
            lineTo.setY(0.1037850467 * size + 0.055 * size);
            hLineTo.setX(0.5 * size - 0.0175 * size);
        }
    }
}
