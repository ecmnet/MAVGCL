/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.widgets;

import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class FadePane extends Pane {

	private FadeTransition in = null;
	private FadeTransition out = null;


	private BooleanProperty fade = new SimpleBooleanProperty();

	private final BooleanProperty dragModeActiveProperty =
			new SimpleBooleanProperty(this, "dragModeActive", true);

	public FadePane() {
		this(150);
		makeDraggable(this);

	}

	public FadePane(int duration_ms) {

		in = new FadeTransition(Duration.millis(duration_ms), this);
		in.setFromValue(0.0);
		in.setToValue(1.0);

		out = new FadeTransition(Duration.millis(duration_ms), this);
		out.setFromValue(1.0);
		out.setToValue(0.0);

		out.setOnFinished(value -> {
			setVisible(false);
		});

		this.fade.addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue.booleanValue()) {
					setVisible(true);
					in.play();
				}
				else {
					out.play();
				}
			}

		});

		makeDraggable(this);



	}

	public BooleanProperty fadeProperty() {
		return fade;
	}

	private Node makeDraggable(final Node node) {
		final DragContext dragContext = new DragContext();

		//        node.addEventFilter(
		//                MouseEvent.ANY,
		//                new EventHandler<MouseEvent>() {
		//                    public void handle(final MouseEvent mouseEvent) {
		//                        if (dragModeActiveProperty.get()) {
		//                            // disable mouse events for all children
		//                            mouseEvent.consume();
		//                        }
		//                    }
		//                });



		node.setOnMousePressed(me -> {
			if (!dragModeActiveProperty.get()) {
				dragModeActiveProperty.set(true);
				node.getScene().setCursor(Cursor.HAND);
				// remember initial mouse cursor coordinates
				// and node position
				dragContext.mouseAnchorX = me.getX();
				dragContext.mouseAnchorY = me.getY();

			}
		});

		node.setOnMouseReleased(me -> {
			if (!me.isPrimaryButtonDown()) {
				node.getScene().setCursor(Cursor.DEFAULT);
				dragModeActiveProperty.set(false);
			}
		});

		node.setOnMouseDragged(me -> {
			if (dragModeActiveProperty.get()) {
				node.setLayoutX(node.getLayoutX() + me.getX() - dragContext.mouseAnchorX);
				node.setLayoutY(node.getLayoutY() + me.getY() - dragContext.mouseAnchorY);
			}
		});



		return node;
	}

	private static final class DragContext {
		public double mouseAnchorX;
		public double mouseAnchorY;

	}
}
