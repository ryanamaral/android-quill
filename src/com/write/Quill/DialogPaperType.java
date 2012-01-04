package com.write.Quill;

import name.vbraun.view.write.Paper;

public class DialogPaperType extends SingleChoiceDialog<Paper.Type> {
	public static final String TAG = "DialogPaperType";
	
	final CharSequence[] items = new CharSequence[Paper.Table.length];
	final Paper.Type[] values = new Paper.Type[Paper.Table.length];
	
	public DialogPaperType() {
    	for (int i=0; i<Paper.Table.length; i++) {
    		items[i] = Paper.Table[i].getName();
    		values[i] = Paper.Table[i].getValue();
    	}
	}
	
	public String getTitle() {
		return "Paper type";
	}
	
	public CharSequence[] getItems() {
		return items;
	}
	
	public Paper.Type[] getValues() {
		return values;
	}
}
