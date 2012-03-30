package com.write.Quill;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public abstract class SingleChoiceDialog<T> {
	public static final String TAG = "SingleChoiceDialog";
	
	protected Context context = null;
	
	abstract public String getTitle(); 
	abstract public CharSequence[] getItems();
	abstract public T[] getValues();	
	
	protected AlertDialog dialog = null;
	
	public CharSequence getItem(int position) {
		return getItems()[position];
	}
	
	public T getValue(int position) {
		return getValues()[position];
	}
	
	public boolean areValuesEqual(T value1, T value2) {
		return value1 == value2;
	}
	
	public void setSelectionByValue(T value) {
		int index = 0;
		for (int i=0; i<getValues().length; i++) {
    		if (areValuesEqual(getValue(i), value))
    			index = i;
		}
		setSelection(index);
	}
	
	public void setSelection(int index) {
		dialog.getListView().setItemChecked(index, true);
	}
	
	public AlertDialog create(Context context, DialogInterface.OnClickListener listener) {
		this.context = context;
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(getTitle());
    	builder.setSingleChoiceItems(getItems(), -1, listener); 
    	dialog = builder.create();
    	return dialog;
	}
	
}
