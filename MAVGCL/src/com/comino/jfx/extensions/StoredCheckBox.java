package com.comino.jfx.extensions;

import com.comino.flight.prefs.MAVPreferences;

import javafx.scene.control.CheckBox;

public class StoredCheckBox extends CheckBox {

	public StoredCheckBox() {
		super();
		setOnMouseReleased(event -> store_selection_state());
	}

	@Override
	public void requestLayout() {
		this.setSelected(MAVPreferences.getInstance().getBoolean(this.getId(),false));
		super.requestLayout();
	}




	public StoredCheckBox(String text) {
		super(text);
		setOnMouseReleased(event -> store_selection_state());
	}



	private void store_selection_state() {
		MAVPreferences.getInstance().putBoolean(this.getId(), isSelected());
	}

}
