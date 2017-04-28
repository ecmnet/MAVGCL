package com.comino.flight.file;

public class KeyFigurePreset {
    private int group;
    private int id;
    private int[] keyFigures;
    private String name;

    public KeyFigurePreset() {
    	this.keyFigures = new int[4];
    }

    public void setName(String name) {
    	this.name = name;
    }

    public KeyFigurePreset(int id, int group, int...hash) {
    	this.keyFigures = new int[4];
    	for(int i=0; i< 4 && i< hash.length;i++)
    		keyFigures[i] = hash[i];
    	this.id = id;
    	this.group = group;
    }

    public void set(int id, int group, int...hash) {
    	for(int i=0; i< 4 && i< hash.length;i++)
    		keyFigures[i] = hash[i];
    	this.id = id;
    	this.group = group;
    }

    public int getId() {
    	return this.id;
    }

    public int getKeyFigure(int i) {
    	return keyFigures[i];
    }

    public int getGroup() {
    	return this.group;
    }

    public String getName() {
    	return name;
    }

}
