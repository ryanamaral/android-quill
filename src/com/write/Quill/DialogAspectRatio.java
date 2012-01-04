package com.write.Quill;

import name.vbraun.view.write.AspectRatio;

public class DialogAspectRatio extends SingleChoiceDialog<Float> {
	public static final String TAG = "DialogAspectRatio";

	final CharSequence[] items = new CharSequence[AspectRatio.Table.length];
	final Float[] values = new Float[AspectRatio.Table.length];

	public DialogAspectRatio() {
		for (int i=0; i<AspectRatio.Table.length; i++) {
			items[i] = AspectRatio.Table[i].getName();
			values[i] = AspectRatio.Table[i].getValue();
		}
	}

	public boolean areValuesEqual(Float value1, Float value2) {
		return value1.equals(value2);
	}


	public String getTitle() {
		return "Paper type";
	}

	public CharSequence[] getItems() {
		return items;
	}

	public Float[] getValues() {
		return values;
	}
}
