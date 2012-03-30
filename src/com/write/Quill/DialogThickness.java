package com.write.Quill;

import android.content.res.Resources;


public class DialogThickness extends SingleChoiceDialog<Integer> {
	public static final String TAG = "DialogThickness";
	
	public String getTitle() {
		return context.getString(R.string.dlg_thickness_title);
	}
	
	public CharSequence[] getItems() {
		Resources res = context.getResources();
		final CharSequence[] items = res.getStringArray(R.array.dlg_thickness_entries);
		//	{"Single pixel", "Ultra-fine", "Thin", "Medium", "Thick", "Giant"};
		return items;
	}
	
	public Integer[] getValues() {
		Resources res = context.getResources();
		final int[] val = res.getIntArray(R.array.dlg_thickness_values);
		final Integer[] values = new Integer[val.length];
		for (int i=0; i<val.length; i++)
			values[i] = val[i];
		// {0, 1, 2, 5, 12, 40};
		return values;
	}
}
