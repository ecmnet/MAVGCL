package com.comino.flight.widgets.charts.xy;

import org.mavlink.messages.MAV_SEVERITY;

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

public class XYMessageAnnotation  implements XYAnnotation {

	private Circle     circle = null;
	private GridPane   pane   = new GridPane();
	private LogMessage msg    = null;
	private float      xpos   = 0;
	private float      ypos   = 0;
	private Label      label  = null;

	public XYMessageAnnotation(float xpos, float ypos, LogMessage message) {
		this.xpos = xpos;
		this.ypos = ypos;
		this.msg  = message;

		this.circle = new Circle(5);
		switch(message.severity) {
		case MAV_SEVERITY.MAV_SEVERITY_DEBUG:
			this.circle.setFill(Color.CADETBLUE); break;
		case MAV_SEVERITY.MAV_SEVERITY_INFO:
			this.circle.setFill(Color.GREEN); break;
		case MAV_SEVERITY.MAV_SEVERITY_WARNING:
			this.circle.setFill(Color.YELLOW); break;
		case MAV_SEVERITY.MAV_SEVERITY_CRITICAL:
			this.circle.setFill(Color.RED); break;
		default:
			this.circle.setFill(Color.DARKGREY);
		}
		this.circle.setStrokeType(StrokeType.INSIDE);
//		this.label = new Label(msg.msg);
//		this.label.setVisible(true);
//		this.label.setRotate(180);
//
//		label.setStyle("-fx-background-color: rgba(30, 30, 30, 0.2);");

		pane.setHgap(3);
		GridPane.setMargin(circle,new Insets(4,2,4,0));

		pane.addRow(0,circle);
//		pane.getTransforms().add(new Rotate(90,0,0));

	}

	@Override
	public Node getNode() {
		return pane;
	}

	@Override
	public void layoutAnnotation(ValueAxis xAxis, ValueAxis yAxis) {
		pane.setLayoutX(xAxis.getDisplayPosition(xpos));
		pane.setLayoutY(yAxis.getDisplayPosition(ypos));
	}

}
