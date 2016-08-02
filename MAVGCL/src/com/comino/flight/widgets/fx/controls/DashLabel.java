package com.comino.flight.widgets.fx.controls;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class DashLabel extends GridPane {

	private Line  line = null;
	private Label label     = null;

	@SuppressWarnings("restriction")
	public DashLabel() {
		super();
		this.setPadding(new Insets(3,0,3,0));
	    this.setHgap(4);

	    label = new Label(); label.setTextFill(Color.DARKCYAN.brighter());
	    line = new Line(); line.setStroke(Color.DARKCYAN.darker());

	    this.addColumn(0, label);
	    this.addColumn(1, line);
	    this.setPrefWidth(999);

	    final FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();

	    this.widthProperty().addListener((v,ov,nv) -> {
	    	line.setStartX(0.0f);
		    line.setStartY(this.prefHeightProperty().floatValue()/2);
		    line.setEndX(this.getWidth()-fontLoader.computeStringWidth(label.getText(), label.getFont())-10);
		    line.setEndY(this.prefHeightProperty().floatValue()/2);
	    });
	}

	public DashLabel(String text) {
		this();
		label.setText(text);
	}


	public String getText() {
        return label.getText();
    }

    public void setText(String value) {
        label.setText(value);
    }


}
