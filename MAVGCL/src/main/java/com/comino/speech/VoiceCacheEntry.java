package com.comino.speech;

public class VoiceCacheEntry {

	private String text   = null;
	private byte[] buffer = null;


	public VoiceCacheEntry(String text, byte[] buffer) {
		this.text = text;
		this.buffer = buffer;
	}

	public boolean isValid(String text) {
		return text.equalsIgnoreCase(this.text);
	}

	public byte[] get(String text) {
		if(text.equalsIgnoreCase(this.text))
			return buffer;
		else
			return null;
	}




}
