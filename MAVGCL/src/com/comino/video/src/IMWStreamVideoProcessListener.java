package com.comino.video.src;

import java.awt.image.BufferedImage;

import javafx.scene.image.Image;


public interface IMWStreamVideoProcessListener {

	public void process(Image image) throws Exception;

}
