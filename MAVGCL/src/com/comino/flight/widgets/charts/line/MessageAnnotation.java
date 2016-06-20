package com.comino.flight.widgets.charts.line;

import java.util.concurrent.TimeUnit;

import com.comino.msp.model.segment.LogMessage;
import com.comino.msp.utils.ExecutorService;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.animation.Animation.Status;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

public class MessageAnnotation  implements XYAnnotation {

	private Circle     circle = null;
	private Pane       pane   = new Pane();
	private LogMessage msg    = null;
	private float      xpos   = 0;
	private Label      label  = null;

	private FadeTransition     out =  null;
	private long       tms    = 0;

	public MessageAnnotation(float xpos, LogMessage message) {
		this.xpos = xpos;
		this.msg  = message;

		this.circle = new Circle(5);
		this.circle.setFill(Color.RED);
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.label = new Label(msg.msg);
		this.label.setVisible(false);

		pane.getChildren().add(circle);
		pane.getChildren().add(label);

		out = new FadeTransition(Duration.millis(100), label);
		out.setFromValue(1.0);
		out.setToValue(0.0);

		out.setOnFinished(value -> {
			label.setVisible(false);
		});

		this.circle.setOnMouseEntered(event -> {
			this.label.setVisible(true);
		});

		this.circle.setOnMouseExited(event -> {
			  ExecutorService.get().schedule(() -> {
				  this.out.play();
			  }, 2, TimeUnit.SECONDS);
		});

	}

	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos)-10);
		pane.setLayoutY(2);
	}

}
