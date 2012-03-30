package com.write.Quill;

import name.vbraun.view.write.AspectRatio;

public class DialogAspectRatio extends SingleChoiceDialog<Float> {
	public static final String TAG = "DialogAspectRatio";

	private CharSequence[] items = null;
	private Float[] values = null;

	public boolean areValuesEqual(Float value1, Float value2) {
		return value1.equals(value2);
	}


	public String getTitle() {
		return context.getString(R.string.dlg_aspect_title);
	}

	public CharSequence[] getItems() {
		if (items == null) {
			items = new CharSequence[AspectRatio.Table.length];
			for (int i=0; i<AspectRatio.Table.length; i++)
				items[i] = AspectRatio.Table[i].getName(context);
		}
		return items;
	}

	public Float[] getValues() {
		if (values == null) {
			values = new Float[AspectRatio.Table.length];
			for (int i=0; i<AspectRatio.Table.length; i++)
				values[i] = AspectRatio.Table[i].getValue();
		}
		return values;
	}
}
