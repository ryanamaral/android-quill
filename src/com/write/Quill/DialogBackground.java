package com.write.Quill;

import name.vbraun.view.write.Paper;

public class DialogBackground extends SingleChoiceDialog<Paper.Type> {
	public static final String TAG = "DialogBackground";
	
	private CharSequence[] items = null; 
	private Paper.Type[] values = null;
	
	public String getTitle() {
		return context.getString(R.string.dlg_background_title);
	}
	
	public CharSequence[] getItems() {
		if (items == null) {
			items = new CharSequence[Paper.Table.length];
	    	for (int i=0; i<Paper.Table.length; i++)
	    		items[i] = Paper.Table[i].getName(context);
		}
		return items;
	}
	
	public Paper.Type[] getValues() {
		if (values == null) {
			values = new Paper.Type[Paper.Table.length];
	    	for (int i=0; i<Paper.Table.length; i++)
	    		values[i] = Paper.Table[i].getType();
		}
		return values;
	}
}
