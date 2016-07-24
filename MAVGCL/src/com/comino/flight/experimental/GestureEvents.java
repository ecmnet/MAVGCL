package com.comino.flight.experimental;


import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.effect.Lighting;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

/**
 *
 * Sample that shows how gesture events are generated. The UI consists of
 * two shapes and a log. The shapes respond to scroll, zoom, rotate and
 * swipe events. The log contains information for the last 50 events that
 * were generated and captured for the rectangle and ellipse object.
 */
public class GestureEvents extends Application {

    private int gestureCount;
    private ObservableList<String> events = FXCollections.observableArrayList();


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        AnchorPane root = new AnchorPane();

        // Create the shapes that respond to gestures and use a VBox to
        // organize them
        VBox shapes = new VBox();
        shapes.setAlignment(Pos.CENTER);
        shapes.setPadding(new Insets(15.0));
        shapes.setSpacing(30.0);
        shapes.setPrefWidth(500);
        shapes.getChildren().addAll(createRectangle(), createEllipse());
        AnchorPane.setTopAnchor(shapes, 15.0);

        // Create the log that shows events
        ListView<String> log = createLog(events);
        AnchorPane.setBottomAnchor(log, 5.0);
        AnchorPane.setLeftAnchor(log, 5.0);
        AnchorPane.setRightAnchor(log, 5.0);

        root.getChildren().addAll(shapes, log);
        Scene scene = new Scene(root, 500, 500);

        primaryStage.setTitle("Gesture Events Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

/**
 * Creates a rectangle that responds to gestures on a touch screen or
 * trackpad and logs the events that are handled.
 *
 * @return Rectangle to show
 *
 */
    private Rectangle createRectangle() {

        final Rectangle rect = new Rectangle(100, 100, 100, 100);
        rect.setFill(Color.DARKMAGENTA);

        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                if (!event.isInertia()) {
                    rect.setTranslateX(rect.getTranslateX() + event.getDeltaX());
                    rect.setTranslateY(rect.getTranslateY() + event.getDeltaY());
                }
                log("Rectangle: Scroll event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());
                event.consume();
            }
        });

        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                rect.setScaleX(rect.getScaleX() * event.getZoomFactor());
                rect.setScaleY(rect.getScaleY() * event.getZoomFactor());
                log("Rectangle: Zoom event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());

                event.consume();
            }
        });

        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rect.setRotate(rect.getRotate() + event.getAngle());
                log("Rectangle: Rotate event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());
                event.consume();
            }
        });

        rect.setOnScrollStarted(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                inc(rect);
                log("Rectangle: Scroll started event");
                event.consume();
            }
        });

        rect.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                dec(rect);
                log("Rectangle: Scroll finished event");
                event.consume();
            }
        });

        rect.setOnZoomStarted(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                inc(rect);
                log("Rectangle: Zoom event started");
                event.consume();
            }
        });

        rect.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                dec(rect);
                log("Rectangle: Zoom event finished");
                event.consume();
            }
        });

        rect.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                inc(rect);
                log("Rectangle: Rotate event started");
                event.consume();
            }
        });

        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                dec(rect);
                log("Rectangle: Rotate event finished");
                event.consume();
            }
        });

        rect.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                log("Rectangle: Mouse pressed event" +
                        ", synthesized: " + event.isSynthesized());
                event.consume();
            }
        });

        rect.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                log("Rectangle: Mouse released event" +
                        ", synthesized: " + event.isSynthesized());
                event.consume();
            }
        });

        rect.setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent event) {
                log("Rectangle: Touch pressed event");
                event.consume();
            }
        });

        rect.setOnTouchReleased(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent event) {
                log("Rectangle: Touch released event");
                event.consume();
            }
        });

        rect.setOnSwipeRight(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                log("Rectangle: Swipe right event");
                event.consume();
            }
        });

        rect.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                log("Rectangle: Swipe left event");
                event.consume();
            }
        });

        return rect;
    }

/**
 * Creates an ellipse that responds to gestures on a touch screen or
 * trackpad and logs the events that are handled.
 *
 * @return Ellipse to show
 *
 */
    private Ellipse createEllipse() {

        final Ellipse oval = new Ellipse(100, 50);
        oval.setFill(Color.STEELBLUE);

        oval.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                oval.setTranslateX(oval.getTranslateX() + event.getDeltaX());
                oval.setTranslateY(oval.getTranslateY() + event.getDeltaY());
                log("Ellipse: Scroll event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());
                event.consume();
            }
        });

        oval.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                oval.setScaleX(oval.getScaleX() * event.getZoomFactor());
                oval.setScaleY(oval.getScaleY() * event.getZoomFactor());
                log("Ellipse: Zoom event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());
                event.consume();
            }
        });

        oval.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                oval.setRotate(oval.getRotate() + event.getAngle());
                log("Ellipse: Rotate event" +
                        ", inertia: " + event.isInertia() +
                        ", direct: " + event.isDirect());
                event.consume();
            }
        });

        oval.setOnScrollStarted(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                inc(oval);
                log("Ellipse: Scroll started event");
                event.consume();
            }
        });

        oval.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                dec(oval);
                log("Ellipse: Scroll finished event");
                event.consume();
            }
        });

        oval.setOnZoomStarted(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                inc(oval);
                log("Ellipse: Zoom event started");
                event.consume();
            }
        });

        oval.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                dec(oval);
                log("Ellipse: Zoom event finished");
                event.consume();
            }
        });

        oval.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                inc(oval);
                log("Ellipse: Rotate event started");
                event.consume();
            }
        });

        oval.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                dec(oval);
                log("Ellipse: Rotate event finished");
                event.consume();
            }
        });

// Respond to mouse pressed only if it is in response to a screen touch
        oval.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isSynthesized()) {
                    log("Ellipse: Mouse pressed event from touch" +
                            ", synthesized: " + event.isSynthesized());
                }
                event.consume();
            }
        });

// Respond to mouse released only if it is in response to a screen touch
        oval.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isSynthesized()) {
                    log("Ellipse: Mouse released event from touch" +
                            ", synthesized: " + event.isSynthesized());
                }
                event.consume();
            }
        });

        return oval;
    }

    /**
     * Creates a log that shows the events.
     */
    private ListView<String> createLog(ObservableList<String> messages){
        final ListView<String> log = new ListView<String>();
        log.setPrefSize(500, 200);
        log.setItems(messages);

        return log;
    }

/**
 * Uses lighting to visually change the object for the duration of
 * the gesture.
 *
 * @param shape Target of the gesture
 */
    private void inc(Shape shape) {
        if (gestureCount == 0) {
            shape.setEffect(new Lighting());
        }
        gestureCount++;
    }

/**
 * Restores the object to its original state when the gesture completes.
 *
 * @param shape Target of the gesture
 */
    private void dec(Shape shape) {
        gestureCount--;
        if (gestureCount == 0) {
            shape.setEffect(null);
        }
    }

    /**
     * Adds a message to the log.
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        // Limit log to 50 entries, delete from bottom and add to top
        if (events.size() == 50) {
            events.remove(49);
        }
        events.add(0, message);
    }
}
