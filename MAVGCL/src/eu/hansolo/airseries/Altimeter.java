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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;


/**
 * User: hansolo
 * Date: 17.04.14
 * Time: 20:15
 */
public class Altimeter extends Region {
    private static final double  PREFERRED_WIDTH    = 320;
    private static final double  PREFERRED_HEIGHT   = 320;
    private static final double  MINIMUM_WIDTH      = 5;
    private static final double  MINIMUM_HEIGHT     = 5;
    private static final double  MAXIMUM_WIDTH      = 1024;
    private static final double  MAXIMUM_HEIGHT     = 1024;   
    private static final double  MAX_VALUE          = 10;
    private static final double  ANGLE_STEP_100FT   = 360.0 / MAX_VALUE;
    private static final double  ANGLE_STEP_1000FT  = ANGLE_STEP_100FT / 10.0;
    private static final double  ANGLE_STEP_10000FT = ANGLE_STEP_1000FT / 10.0;

    private double                size;
    private double                width;
    private double                height;
    private Canvas                tickmarkCanvas;
    private GraphicsContext       tickmarkCtx;

    private Canvas                pointer10000Ft;
    private GraphicsContext       pointer10000FtCtx;
    private Canvas                pointer1000Ft;
    private GraphicsContext       pointer1000FtCtx;
    private Canvas                pointer100Ft;
    private GraphicsContext       pointer100FtCtx;

    private Region                background;
    private Region                centerKnob;
    private Pane                  pane;    
    private DoubleProperty        targetValue;
    private DoubleProperty        value;
    private double                oldValue;
    private DoubleProperty        value100;    
    private DoubleProperty        value1000;    
    private DoubleProperty        value10000;    

    private BooleanProperty       animated;
    private ObjectProperty<Color> textColor;
    private Timeline              timeline;
    private String                fontName;


    // ******************** Constructors **************************************
    public Altimeter() {
        getStylesheets().add(getClass().getResource("altimeter.css").toExternalForm());
        getStyleClass().add("altimeter");
        targetValue     = new DoublePropertyBase(0) {
            @Override public void set(final double VALUE) {
                oldValue = get();
                super.set(VALUE);
            }
            @Override public Object getBean() { return Altimeter.this; }
            @Override public String getName() { return "targetValue"; }
        };
        value           = new SimpleDoubleProperty(this, "value", 0);        
        oldValue        = 0;
        value100        = new SimpleDoubleProperty(this, "value100", 0);       
        value1000       = new SimpleDoubleProperty(this, "value100", 0);        
        value10000      = new SimpleDoubleProperty(this, "value100", 0);        
        animated        = new SimpleBooleanProperty(this, "animated", true);
        textColor       = new SimpleObjectProperty<>(this, "textColor", Color.web("#ffffff"));
        timeline        = new Timeline();
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
        fontName      = Font.loadFont(Altimeter.class.getResourceAsStream("Verdana.ttf"), 10).getName();

        background    = new Region();
        background.getStyleClass().add("background");
        background.setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);

        tickmarkCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        tickmarkCtx = tickmarkCanvas.getGraphicsContext2D();

        pointer10000Ft = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        pointer10000FtCtx = pointer10000Ft.getGraphicsContext2D();

        pointer1000Ft = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        pointer1000FtCtx = pointer1000Ft.getGraphicsContext2D();

        pointer100Ft = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        pointer100FtCtx = pointer100Ft.getGraphicsContext2D();
        
        centerKnob    = new Region();
        centerKnob.getStyleClass().add("center-knob");
        centerKnob.setPrefSize(32, 32);
       
        pane = new Pane(background, tickmarkCanvas, pointer10000Ft, pointer1000Ft, pointer100Ft, centerKnob);
        pane.getStyleClass().add("frame");

        getChildren().setAll(pane);
        resize();
    }

    private void registerListeners() {
        widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));        
        value.addListener(observable -> handleControlPropertyChanged("VALUE"));
        targetValue.addListener(observable -> handleControlPropertyChanged("TARGET"));
    }


    // ******************** Methods *******************************************
    private void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("VALUE".equals(PROPERTY)) {                                                      
            if (isAnimated()) {
                double range    = Math.abs(value100.get() - getValue());
                double fraction = range / 1000;
                                
                KeyValue kvTargetValueStart = new KeyValue(targetValue, oldValue, Interpolator.EASE_BOTH);
                KeyValue kvTargetValueStop  = new KeyValue(targetValue, value.get(), Interpolator.EASE_BOTH);
                KeyFrame kfStart = new KeyFrame(Duration.ZERO, kvTargetValueStart);
                KeyFrame kfStop  = new KeyFrame(Duration.millis(1000 * fraction), kvTargetValueStop);
                timeline.getKeyFrames().setAll(kfStart, kfStop);
                timeline.play();
            } else {
                value100.set((getValue() % 1000) / 100d);
                value1000.set((getValue() % 10000) / 100d);
                value10000.set((getValue() % 100000) / 100d);

                // Draw the 10000ft pointer
                pointer10000Ft.setRotate((value10000.get() * ANGLE_STEP_10000FT));

                // Draw the 1000ft pointer
                pointer1000Ft.setRotate((value1000.get() * ANGLE_STEP_1000FT));

                // Draw the 100ft pointer
                pointer100Ft.setRotate((value100.get() * ANGLE_STEP_100FT));
            }
        } else if ("TARGET".equals(PROPERTY)) {
            value100.set((targetValue.get() % 1000) / 100d);
            value1000.set((targetValue.get() % 10000) / 100d);
            value10000.set((targetValue.get() % 100000) / 100d);

            // Draw the 10000ft pointer
            pointer10000Ft.setRotate((value10000.get() * ANGLE_STEP_10000FT));

            // Draw the 1000ft pointer
            pointer1000Ft.setRotate((value1000.get() * ANGLE_STEP_1000FT));

            // Draw the 100ft pointer
            pointer100Ft.setRotate((value100.get() * ANGLE_STEP_100FT));
        }
    }

    public final double getValue() { return value.get(); }
    public final void setValue(final double VALUE) { value.set(VALUE); }
    public final DoubleProperty valueProperty() { return value; }

    public final boolean isAnimated() { return animated.get(); }
    public final void setAnimated(final boolean ANIMATED) { animated.set(ANIMATED); }
    public final BooleanProperty animatedProperty() { return animated; }
    
    public final Color getTextColor() { return textColor.get(); }
    public final void setTextColor(final Color ORIENTATION_COLOR) {
        textColor.set(ORIENTATION_COLOR);
        resize();
    }
    public final ObjectProperty<Color> textColorProperty() { return textColor; }

    private Font getRegularFontAt(final double SIZE) { return Font.font(fontName, FontWeight.NORMAL, SIZE); }
    private Font getBoldFontAt(final double SIZE) { return Font.font(fontName, FontWeight.BOLD, SIZE); }


    // ******************** Resizing ******************************************
    private void drawPointer10000FtCanvas() {
        pointer10000FtCtx.clearRect(0, 0, pointer10000Ft.getWidth(), pointer10000Ft.getHeight());
        
        DropShadow dropShadow = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 0.015 * size, 0, 0, 0);
        pointer10000FtCtx.save();
        pointer10000FtCtx.setEffect(dropShadow);
        
        pointer10000FtCtx.setFillRule(FillRule.EVEN_ODD);
        pointer10000FtCtx.beginPath();

        pointer10000FtCtx.moveTo(0.465 * width, 0.5 * height);
        pointer10000FtCtx.bezierCurveTo(0.465 * width, 0.52 * height, 0.48 * width, 0.535 * height, 0.5 * width, 0.535 * height);
        pointer10000FtCtx.bezierCurveTo(0.52 * width, 0.535 * height, 0.535 * width, 0.52 * height, 0.535 * width, 0.5 * height);
        pointer10000FtCtx.bezierCurveTo(0.535 * width, 0.485 * height, 0.525 * width, 0.475 * height, 0.515 * width, 0.47 * height);
        pointer10000FtCtx.bezierCurveTo(0.515 * width, 0.47 * height, 0.515 * width, 0.315 * height, 0.515 * width, 0.315 * height);
        pointer10000FtCtx.lineTo(0.505 * width, 0.3 * height);
        pointer10000FtCtx.lineTo(0.505 * width, 0.125 * height);
        pointer10000FtCtx.lineTo(0.535 * width, 0.08 * height);
        pointer10000FtCtx.lineTo(0.465 * width, 0.08 * height);
        pointer10000FtCtx.lineTo(0.495 * width, 0.125 * height);
        pointer10000FtCtx.lineTo(0.495 * width, 0.3 * height);
        pointer10000FtCtx.lineTo(0.485 * width, 0.315 * height);
        pointer10000FtCtx.bezierCurveTo(0.485 * width, 0.315 * height, 0.485 * width, 0.47 * height, 0.485 * width, 0.47 * height);
        pointer10000FtCtx.bezierCurveTo(0.475 * width, 0.475 * height, 0.465 * width, 0.485 * height, 0.465 * width, 0.5 * height);
        pointer10000FtCtx.closePath();
        pointer10000FtCtx.setFill(Color.WHITE);
        pointer10000FtCtx.fill();
        pointer10000FtCtx.restore();
    }
    
    private void drawPointer1000FtCanvas() {
        pointer1000FtCtx.clearRect(0, 0, pointer1000Ft.getWidth(), pointer1000Ft.getHeight());

        DropShadow dropShadow = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 0.015 * size, 0, 0, 0);
        pointer1000FtCtx.save();
        pointer1000FtCtx.setEffect(dropShadow);
        
        pointer1000FtCtx.setFillRule(FillRule.EVEN_ODD);
        pointer1000FtCtx.beginPath();

        pointer1000FtCtx.moveTo(0.465 * size, 0.5 * size);
        pointer1000FtCtx.bezierCurveTo(0.465 * size, 0.51 * size, 0.47 * size, 0.52 * size, 0.48 * size, 0.53 * size);
        pointer1000FtCtx.bezierCurveTo(0.48 * size, 0.53 * size, 0.465 * size, 0.595 * size, 0.465 * size, 0.595 * size);
        pointer1000FtCtx.bezierCurveTo(0.465 * size, 0.595 * size, 0.46 * size, 0.615 * size, 0.5 * size, 0.615 * size);
        pointer1000FtCtx.bezierCurveTo(0.54 * size, 0.615 * size, 0.535 * size, 0.595 * size, 0.535 * size, 0.595 * size);
        pointer1000FtCtx.bezierCurveTo(0.535 * size, 0.595 * size, 0.52 * size, 0.53 * size, 0.52 * size, 0.53 * size);
        pointer1000FtCtx.bezierCurveTo(0.53 * size, 0.52 * size, 0.535 * size, 0.51 * size, 0.535 * size, 0.5 * size);
        pointer1000FtCtx.bezierCurveTo(0.535 * size, 0.49 * size, 0.53 * size, 0.48 * size, 0.52 * size, 0.47 * size);
        pointer1000FtCtx.bezierCurveTo(0.52 * size, 0.47 * size, 0.535 * size, 0.355 * size, 0.535 * size, 0.355 * size);
        pointer1000FtCtx.lineTo(0.5 * size, 0.28 * size);
        pointer1000FtCtx.lineTo(0.465 * size, 0.355 * size);
        pointer1000FtCtx.bezierCurveTo(0.465 * size, 0.355 * size, 0.48 * size, 0.47 * size, 0.48 * size, 0.47 * size);
        pointer1000FtCtx.bezierCurveTo(0.47 * size, 0.48 * size, 0.465 * size, 0.49 * size, 0.465 * size, 0.5 * size);
        pointer1000FtCtx.closePath();
        
        pointer1000FtCtx.setFill(new LinearGradient(0, size * 0.3317757009345794, 0, size * 0.6121495327102804, 
                                                    false, CycleMethod.NO_CYCLE,
                                                    new Stop(0.0, Color.WHITE),
                                                    new Stop(0.52, Color.WHITE),
                                                    new Stop(0.52, Color.rgb(32, 32, 32)), 
                                                    new Stop(1.0, Color.rgb(32, 32, 32))));
        pointer1000FtCtx.fill();
        pointer1000FtCtx.restore();
    }
    
    private void drawPointer100FtCanvas() {
        pointer100FtCtx.clearRect(0, 0, pointer100Ft.getWidth(), pointer100Ft.getHeight());

        DropShadow dropShadow = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 0.015 * size, 0, 0, 0);
        pointer100FtCtx.save();
        pointer100FtCtx.setEffect(dropShadow);
        
        pointer100FtCtx.setFillRule(FillRule.EVEN_ODD);
        pointer100FtCtx.beginPath();
        
        pointer100FtCtx.moveTo(0.465 * size, 0.5 * size);
        pointer100FtCtx.bezierCurveTo(0.465 * size, 0.515 * size, 0.475 * size, 0.53 * size, 0.49 * size, 0.535 * size);
        pointer100FtCtx.bezierCurveTo(0.49 * size, 0.535 * size, 0.49 * size, 0.59 * size, 0.49 * size, 0.59 * size);
        pointer100FtCtx.bezierCurveTo(0.485 * size, 0.59 * size, 0.48 * size, 0.6 * size, 0.48 * size, 0.605 * size);
        pointer100FtCtx.bezierCurveTo(0.48 * size, 0.615 * size, 0.49 * size, 0.625 * size, 0.5 * size, 0.625 * size);
        pointer100FtCtx.bezierCurveTo(0.51 * size, 0.625 * size, 0.52 * size, 0.615 * size, 0.52 * size, 0.605 * size);
        pointer100FtCtx.bezierCurveTo(0.52 * size, 0.6 * size, 0.515 * size, 0.59 * size, 0.51 * size, 0.59 * size);
        pointer100FtCtx.bezierCurveTo(0.51 * size, 0.59 * size, 0.51 * size, 0.535 * size, 0.51 * size, 0.535 * size);
        pointer100FtCtx.bezierCurveTo(0.525 * size, 0.53 * size, 0.535 * size, 0.515 * size, 0.535 * size, 0.5 * size);
        pointer100FtCtx.bezierCurveTo(0.535 * size, 0.485 * size, 0.525 * size, 0.47 * size, 0.51 * size, 0.465 * size);
        pointer100FtCtx.bezierCurveTo(0.51 * size, 0.465 * size, 0.51 * size, 0.145 * size, 0.51 * size, 0.145 * size);
        pointer100FtCtx.lineTo(0.5 * size, 0.11 * size);
        pointer100FtCtx.lineTo(0.49 * size, 0.145 * size);
        pointer100FtCtx.bezierCurveTo(0.49 * size, 0.145 * size, 0.49 * size, 0.465 * size, 0.49 * size, 0.465 * size);
        pointer100FtCtx.bezierCurveTo(0.475 * size, 0.47 * size, 0.465 * size, 0.485 * size, 0.465 * size, 0.5 * size);
        pointer100FtCtx.closePath();
                
        pointer100FtCtx.setFill(new LinearGradient(0, size * 0.16822429906542055, 0, size * 0.6261682242990654,
                                                    false, CycleMethod.NO_CYCLE,
                                                    new Stop(0.0, Color.WHITE),
                                                    new Stop(0.31, Color.WHITE),
                                                    new Stop(0.31, Color.rgb(32, 32, 32)),
                                                    new Stop(1.0, Color.rgb(32, 32, 32))));
        pointer100FtCtx.fill();
        pointer100FtCtx.restore();
    }
    
    private void drawTickmarksCanvas() {
        tickmarkCtx.clearRect(0, 0, tickmarkCanvas.getWidth(), tickmarkCanvas.getHeight());

        // Draw the direction tickmarkCanvas
        int      numberCounter = 0;
        double   sinValue;
        double   cosValue;
        double   offset = 180;
        Point2D  center = new Point2D(size * 0.5, size * 0.5);

        for (double angle = 0, counter = 0 , minTickCounter = 0 ; Double.compare(counter, 360) < 0 ; angle--, counter++, minTickCounter += 7.2) {
            sinValue = Math.sin(Math.toRadians(angle + offset));
            cosValue = Math.cos(Math.toRadians(angle + offset));

            Point2D innerPoint      = new Point2D(center.getX() + size * 0.35 * sinValue, center.getY() + size * 0.35 * cosValue);
            Point2D innerMinorPoint = new Point2D(center.getX() + size * 0.37 * sinValue, center.getY() + size * 0.37 * cosValue);
            Point2D outerMainPoint  = new Point2D(center.getX() + size * 0.41 * sinValue, center.getY() + size * 0.41 * cosValue);            
            Point2D textPoint       = new Point2D(center.getX() + size * 0.29 * sinValue, center.getY() + size * 0.29 * cosValue);

            tickmarkCtx.setStroke(Color.WHITE);
            if (counter % 36 == 0) {
                // Draw direction text
                tickmarkCtx.save();
                tickmarkCtx.setFont(getBoldFontAt(0.09 * size));
                tickmarkCtx.setTextAlign(TextAlignment.CENTER);
                tickmarkCtx.setTextBaseline(VPos.CENTER);
                tickmarkCtx.setFill(getTextColor());
                tickmarkCtx.save();
                tickmarkCtx.translate(textPoint.getX(), textPoint.getY());
                tickmarkCtx.fillText(Integer.toString(numberCounter), 0, 0);
                numberCounter++;
                tickmarkCtx.translate(-textPoint.getX(), -textPoint.getY());
                tickmarkCtx.restore();
                tickmarkCtx.setLineWidth(size * 0.01);
                tickmarkCtx.setLineCap(StrokeLineCap.ROUND);
                tickmarkCtx.strokeLine(innerPoint.getX(), innerPoint.getY(), outerMainPoint.getX(), outerMainPoint.getY());
            } else {
                // Draw tickmarks
                sinValue        = Math.sin(Math.toRadians(minTickCounter + offset));
                cosValue        = Math.cos(Math.toRadians(minTickCounter + offset));
                innerMinorPoint = new Point2D(center.getX() + size * 0.37 * sinValue, center.getY() + size * 0.37 * cosValue);
                outerMainPoint  = new Point2D(center.getX() + size * 0.41 * sinValue, center.getY() + size * 0.41 * cosValue);
                                                
                tickmarkCtx.setLineWidth(size * 0.007);
                tickmarkCtx.setLineCap(StrokeLineCap.ROUND);
                tickmarkCtx.strokeLine(innerMinorPoint.getX(), innerMinorPoint.getY(), outerMainPoint.getX(), outerMainPoint.getY());
            }
        }
    }
    
    private void resize() {
        size   = getWidth() < getHeight() ? getWidth() : getHeight();
        width  = getWidth();
        height = getHeight();

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.relocate((width - size) * 0.5, (height - size) * 0.5);

            background.setPrefSize(size * 0.9, size * 0.9);
            background.relocate((size - background.getPrefWidth()) * 0.5, (size - background.getPrefHeight()) * 0.5);

            tickmarkCanvas.setWidth(size);
            tickmarkCanvas.setHeight(size);
            
            pointer10000Ft.setWidth(size);
            pointer10000Ft.setHeight(size);

            pointer1000Ft.setWidth(size);
            pointer1000Ft.setHeight(size);

            pointer100Ft.setWidth(size);
            pointer100Ft.setHeight(size);

            centerKnob.setPrefSize(size * 0.06, size * 0.06);
            centerKnob.relocate((size - centerKnob.getPrefWidth()) * 0.5, (size - centerKnob.getPrefHeight()) * 0.5);
                                    
            drawTickmarksCanvas();
            drawPointer10000FtCanvas();
            drawPointer1000FtCanvas();
            drawPointer100FtCanvas();
        }
    }
}
