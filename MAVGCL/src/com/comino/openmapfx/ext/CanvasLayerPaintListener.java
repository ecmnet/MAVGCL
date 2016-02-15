package com.comino.openmapfx.ext;

import javafx.scene.canvas.GraphicsContext;

public interface CanvasLayerPaintListener {

	public void redraw(GraphicsContext gc, double width, double height, boolean refresh);

}
