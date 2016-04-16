package com.comino.flight.tabs.parameters;

public class ParameterAttributes {

	public float   default_val = 0;
	public float   min_val = 0;
	public float   max_val = 0;
	public int     decimals = 0;
	public String  name = null;
	public String  type = null;
	public String  description = null;
	public String  description_long = null;
	public String  unit = "";
	public String  group_name = null;

	public ParameterAttributes(String group_name) {
		this.group_name = group_name;
	}

	public ParameterAttributes(String name, String group_name) {
		this.group_name = group_name;
		this.name = name;
		this.type = "none";
		this.description = "none";
		this.description_long = "none";
	}

	public String toString() {
		return "group="+group_name+" name="+name+" description="+description+" type="+type;
	}




}
