package com.comino.flight.widgets.charts.line;

import com.comino.msp.model.segment.LogMessage;
import com.emxsys.chart.extension.XYAnnotation;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class MessageAnnotation  implements XYAnnotation {

	private Circle     circle = null;
	private GridPane   pane   = new GridPane();
	private LogMessage msg    = null;
	private float      xpos   = 0;
	private Label      label  = null;

	public MessageAnnotation(float xpos, LogMessage message) {
		this.xpos = xpos;
		this.msg  = message;

		this.circle = new Circle(5);
		this.circle.setFill(Color.RED);
		this.circle.setStrokeType(StrokeType.INSIDE);
		this.label = new Label(msg.msg);
		this.label.setVisible(true);
		this.label.setRotate(180);

		pane.setHgap(3);
		GridPane.setMargin(circle,new Insets(5,2,5,0));

		pane.addRow(0,circle, label);
		pane.getTransforms().add(new Rotate(90,0,0));

	}

	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos));
		pane.setLayoutY(2);
	}

}
