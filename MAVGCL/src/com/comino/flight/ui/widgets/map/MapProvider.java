package com.comino.flight.ui.widgets.map;

public enum MapProvider {
	BW("blackwhite"),
	STREET("street"),
	BRIGHT("bright"),
	DARK("dark"),
	SAT("sat"),
	TOPO("topo");

	public final String name;

	MapProvider(final String NAME) {
		name = NAME;
	}
}
