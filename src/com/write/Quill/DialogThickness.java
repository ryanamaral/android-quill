package com.write.Quill;


public class DialogThickness extends SingleChoiceDialog<Integer> {
	public static final String TAG = "DialogThickness";
	
	public String getTitle() {
		return "Pen thickness";
	}
	
	public CharSequence[] getItems() {
		final CharSequence[] items = 
			{"Single pixel", "Ultra-fine", "Thin", "Medium", "Thick", "Giant"};
		return items;
	}
	
	public Integer[] getValues() {
		final Integer[] values = {0, 1, 2, 5, 12, 40};
		return values;
	}
}
