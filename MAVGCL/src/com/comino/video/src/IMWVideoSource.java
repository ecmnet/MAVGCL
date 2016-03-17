package com.comino.video.src;

public interface IMWVideoSource {

	void addProcessListener(IMWStreamVideoProcessListener listener);

	Thread start();
	void stop();
	boolean isAvailable();
	int getFPS();


}