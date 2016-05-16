package com.comino.flight.widgets.charts.line;

import com.emxsys.chart.extension.XYAnnotation;

import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;

public class MessageAnnotation  implements XYAnnotation {

	 private final Label label = new Label("Message");
	 private int   xpos  = 0;

	public MessageAnnotation(int xpos, String message) {
		this.xpos = xpos;
		this.label.setText(message);
	//	this.label.setStyle("message_annotation");
		this.label.setRotate(-90);
	}

	@Override
	public Node getNode() {
		return label;
	}

	@Override
	public void layoutAnnotation(ValueAxis arg0, ValueAxis arg1) {

		label.setLayoutY(50);
		label.setLayoutX(arg0.getDisplayPosition(xpos));

	}

}
