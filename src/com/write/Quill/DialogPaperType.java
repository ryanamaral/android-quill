package com.write.Quill;

import com.write.Quill.Page.PaperType;

public class DialogPaperType extends SingleChoiceDialog<PaperType> {
	public static final String TAG = "DialogPaperType";
	
	final CharSequence[] items = new CharSequence[Page.PaperTypes.length];
	final PaperType[] values = new PaperType[Page.PaperTypes.length];
	
	public DialogPaperType() {
    	for (int i=0; i<Page.PaperTypes.length; i++) {
    		items[i] = Page.PaperTypes[i].name;
    		values[i] = Page.PaperTypes[i].type;
    	}
	}
	
	public String getTitle() {
		return "Paper type";
	}
	
	public CharSequence[] getItems() {
		return items;
	}
	
	public PaperType[] getValues() {
		return values;
	}
}
