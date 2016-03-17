package com.comino.video.src;

import javafx.scene.image.Image;


public interface IMWStreamVideoProcessListener {

	public void process(Image image, byte[] buffer) throws Exception;

}
