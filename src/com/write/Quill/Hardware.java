package com.write.Quill;

import android.util.Log;

public class Hardware {
	private final static String TAG = "PenHardware";
	
	private final static Hardware instance = new Hardware();
	public static Hardware getHardware() {
		return instance;
	}
	
	private final String model;
	protected final boolean hasDedicatedPen;
	
	private Hardware() {
		model = android.os.Build.MODEL;
        Log.v(TAG, "Model = >"+model+"<");
		if (model == "ThinkPad Tablet") {
			hasDedicatedPen = true;
		} else {
			// defaults
			hasDedicatedPen = false;
		}
	}
	
}
