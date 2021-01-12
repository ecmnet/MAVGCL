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
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;


/**
 * User: hansolo
 * Date: 02.04.14
 * Time: 07:40
 */
public class AirCompass extends Region {
    private static final double   PREFERRED_WIDTH  = 320;
    private static final double   PREFERRED_HEIGHT = 320;
    private static final double   MINIMUM_WIDTH    = 5;
    private static final double   MINIMUM_HEIGHT   = 5;
    private static final double   MAXIMUM_WIDTH    = 1024;
    private static final double   MAXIMUM_HEIGHT   = 1024;
    private double                size;
    private double                width;
    private double                height;
    private Canvas                bearingCanvas;
    private Region                background;
    private GraphicsContext       bearingCtx;
    private Region                centerKnob;
    private Canvas                airplaneCanvas;
    private GraphicsContext       airplaneCtx;
    private Pane                  pane;
    private DoubleProperty        bearing;
    private DoubleProperty        bearingAngle;
    private ObjectProperty<Color> planeColor;
    private ObjectProperty<Color> orientationColor;
    private Timeline              timeline;
    private String                fontName;


    // ******************** Constructors **************************************
    public AirCompass() {
        getStylesheets().add(getClass().getResource("aircompass.css").toExternalForm());
        getStyleClass().add("air-compass");
        bearing          = new SimpleDoubleProperty(this, "bearing", 0);
        bearingAngle     = new SimpleDoubleProperty(this, "bearingAngle", 0);
        planeColor       = new SimpleObjectProperty<>(this, "planeColor", Color.CYAN.darker());
        orientationColor = new SimpleObjectProperty<>(this, "orientationColor", Color.CYAN.darker());
        timeline         = new Timeline();
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
        fontName      = Font.loadFont(AirCompass.class.getResourceAsStream("Verdana.ttf"), 10).getName();

        background    = new Region();
        background.getStyleClass().add("background");
        background.setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);

        bearingCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        bearingCtx    = bearingCanvas.getGraphicsContext2D();

        centerKnob    = new Region();
        centerKnob.getStyleClass().add("center-knob");
        centerKnob.setPrefSize(32, 32);

        airplaneCanvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        airplaneCtx    = airplaneCanvas.getGraphicsContext2D();

        pane = new Pane(background, bearingCanvas, centerKnob, airplaneCanvas);
        pane.getStyleClass().add("frame");

        getChildren().setAll(pane);
        resize();
    }

    private void registerListeners() {
        widthProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        heightProperty().addListener(observable -> handleControlPropertyChanged("RESIZE"));
        bearingProperty().addListener(observable -> handleControlPropertyChanged("ROTATE"));
    }


    // ******************** Methods *******************************************
    private void handleControlPropertyChanged(final String PROPERTY) {
        if ("RESIZE".equals(PROPERTY)) {
            resize();
        } else if ("ROTATE".equals(PROPERTY)) {
                airplaneCanvas.setRotate(getBearing());
        }
    }

    public final double getBearing() { return bearing.get(); }
    public final void setBearing(final double BEARING) { bearing.set(BEARING); }
    public final DoubleProperty bearingProperty() { return bearing; }

    public final Color getPlaneColor() { return planeColor.get(); }
    public final void setPlaneColor(final Color PLANE_COLOR) {
        planeColor.set(PLANE_COLOR);
        resize();
    }
    public final ObjectProperty<Color> planeColorProperty() { return planeColor; }

    public final Color getOrientationColor() { return orientationColor.get(); }
    public final void setOrientationColor(final Color ORIENTATION_COLOR) {
        orientationColor.set(ORIENTATION_COLOR);
        resize();
    }
    public final ObjectProperty<Color> orientationColorProperty() { return orientationColor; }

    private Font getRegularFontAt(final double SIZE) { return Font.font(fontName, FontWeight.NORMAL, SIZE); }
    private Font getBoldFontAt(final double SIZE) { return Font.font(fontName, FontWeight.BOLD, SIZE); }


    // ******************** Resizing ******************************************
    private void drawCompassCanvas() {
        bearingCtx.clearRect(0, 0, bearingCanvas.getWidth(), bearingCanvas.getHeight());

        // Draw the direction bearingCanvas
        String[] directions       = { "N", "E", "S", "W" };
        int      directionCounter = 0;
        double   sinValue;
        double   cosValue;
        double   offset = 180;
        boolean  toggle = true;
        Point2D  center = new Point2D(size * 0.5, size * 0.5);

        for (double angle = 0, counter = 0 ; Double.compare(counter, 360) < 0 ; angle -= 1, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + offset));
            cosValue = Math.cos(Math.toRadians(angle + offset));

            Point2D innerMinorPoint = new Point2D(center.getX() + size * 0.36 * sinValue, center.getY() + size * 0.36 * cosValue);
            Point2D outerMainPoint  = new Point2D(center.getX() + size * 0.41 * sinValue, center.getY() + size * 0.41 * cosValue);
            Point2D outerPoint      = new Point2D(center.getX() + size * 0.36 * sinValue, center.getY() + size * 0.36 * cosValue);
            Point2D textPoint       = new Point2D(center.getX() + size * 0.30 * sinValue, center.getY() + size * 0.30 * cosValue);

            bearingCtx.setStroke(Color.WHITE);
            if (counter % 90 == 0) {
                // Draw direction text
                bearingCtx.save();
                bearingCtx.setFont(getBoldFontAt(0.075 * size));
                bearingCtx.setTextAlign(TextAlignment.CENTER);
                bearingCtx.setTextBaseline(VPos.CENTER);
                bearingCtx.setFill(getOrientationColor());
                bearingCtx.save();
                bearingCtx.translate(textPoint.getX(), textPoint.getY());
                bearingCtx.rotate(-angle + offset + 180);
                bearingCtx.fillText(directions[directionCounter], 0, 0);
                directionCounter++;
                bearingCtx.translate(-textPoint.getX(), -textPoint.getY());
                bearingCtx.restore();
                bearingCtx.restore();
            } else if (counter % 30 == 0) {
                // Draw bearing text
                bearingCtx.save();
                bearingCtx.setFont(getRegularFontAt(0.073 * size));
                bearingCtx.setTextAlign(TextAlignment.CENTER);
                bearingCtx.setTextBaseline(VPos.CENTER);
                bearingCtx.setFill(Color.WHITE);
                bearingCtx.save();
                bearingCtx.translate(textPoint.getX(), textPoint.getY());
                bearingCtx.rotate(-angle + offset + 180);
                bearingCtx.fillText(Integer.toString((int) counter / 10), 0, 0);
                bearingCtx.translate(-textPoint.getX(), -textPoint.getY());
                bearingCtx.restore();
                bearingCtx.restore();
            }
            if (counter % 5 == 0) {
                // Draw tickmarks
                toggle ^= true;
                bearingCtx.setLineWidth(size * 0.01);
                bearingCtx.setLineCap(StrokeLineCap.ROUND);
                bearingCtx.strokeLine(innerMinorPoint.getX(), innerMinorPoint.getY(), toggle ? outerPoint.getX() : outerMainPoint.getX(), toggle ? outerPoint.getY() : outerMainPoint.getY());
            }
        }
    }

    private void drawAirplaneCanvas() {
        airplaneCtx.clearRect(0, 0, airplaneCanvas.getWidth(), airplaneCanvas.getHeight());

        DropShadow dropShadow = new DropShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 0.015 * size, 0, 0, 0.015 * size);
        airplaneCtx.save();
        airplaneCtx.setEffect(dropShadow);
        // Draw the airplain
        airplaneCtx.setFillRule(FillRule.EVEN_ODD);
        airplaneCtx.beginPath();
        airplaneCtx.moveTo(size * 0.4953271028037383, size * 0.2523364485981308);
        airplaneCtx.bezierCurveTo(size * 0.4953271028037383, size * 0.2523364485981308, size * 0.4766355140186916, size * 0.2850467289719626, size * 0.4719626168224299, size * 0.3130841121495327);
        airplaneCtx.bezierCurveTo(size * 0.4672897196261682, size * 0.32710280373831774, size * 0.4672897196261682, size * 0.38317757009345793, size * 0.4672897196261682, size * 0.38317757009345793);
        airplaneCtx.lineTo(size * 0.32710280373831774, size * 0.5186915887850467);
        airplaneCtx.lineTo(size * 0.32710280373831774, size * 0.5700934579439252);
        airplaneCtx.lineTo(size * 0.4719626168224299, size * 0.48598130841121495);
        airplaneCtx.lineTo(size * 0.4719626168224299, size * 0.6121495327102804);
        airplaneCtx.lineTo(size * 0.4252336448598131, size * 0.6635514018691588);
        airplaneCtx.lineTo(size * 0.4252336448598131, size * 0.7149532710280374);
        airplaneCtx.lineTo(size * 0.48130841121495327, size * 0.6822429906542056);
        airplaneCtx.lineTo(size * 0.4953271028037383, size * 0.6962616822429907);
        airplaneCtx.lineTo(size * 0.5, size * 0.6962616822429907);
        airplaneCtx.lineTo(size * 0.5186915887850467, size * 0.6822429906542056);
        airplaneCtx.lineTo(size * 0.5747663551401869, size * 0.7149532710280374);
        airplaneCtx.lineTo(size * 0.5747663551401869, size * 0.6635514018691588);
        airplaneCtx.lineTo(size * 0.5280373831775701, size * 0.6121495327102804);
        airplaneCtx.lineTo(size * 0.5280373831775701, size * 0.48598130841121495);
        airplaneCtx.lineTo(size * 0.6728971962616822, size * 0.5700934579439252);
        airplaneCtx.lineTo(size * 0.6728971962616822, size * 0.5186915887850467);
        airplaneCtx.lineTo(size * 0.5327102803738317, size * 0.38317757009345793);
        airplaneCtx.bezierCurveTo(size * 0.5327102803738317, size * 0.38317757009345793, size * 0.5327102803738317, size * 0.32710280373831774, size * 0.5280373831775701, size * 0.3130841121495327);
        airplaneCtx.bezierCurveTo(size * 0.5233644859813084, size * 0.2897196261682243, size * 0.5046728971962616, size * 0.2570093457943925, size * 0.5046728971962616, size * 0.2523364485981308);
        airplaneCtx.bezierCurveTo(size * 0.5046728971962616, size * 0.2523364485981308, size * 0.5046728971962616, size * 0.2336448598130841, size * 0.5046728971962616, size * 0.2336448598130841);
        airplaneCtx.lineTo(size * 0.5, size * 0.16822429906542055);
        airplaneCtx.lineTo(size * 0.4953271028037383, size * 0.2336448598130841);
        airplaneCtx.bezierCurveTo(size * 0.4953271028037383, size * 0.2336448598130841, size * 0.4953271028037383, size * 0.2523364485981308, size * 0.4953271028037383, size * 0.2523364485981308);
        airplaneCtx.closePath();
        airplaneCtx.setLineCap(StrokeLineCap.ROUND);
        airplaneCtx.setLineJoin(StrokeLineJoin.MITER);
        airplaneCtx.setLineWidth(0.01 * size);
        airplaneCtx.setStroke(getPlaneColor());
        airplaneCtx.stroke();
        airplaneCtx.restore();
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

            bearingCanvas.setWidth(size);
            bearingCanvas.setHeight(size);

            centerKnob.setPrefSize(size * 0.1, size * 0.1);
            centerKnob.relocate((size - centerKnob.getPrefWidth()) * 0.5, (size - centerKnob.getPrefHeight()) * 0.5);

            airplaneCanvas.setWidth(size);
            airplaneCanvas.setHeight(size);

            drawCompassCanvas();
            drawAirplaneCanvas();
        }
    }
}
