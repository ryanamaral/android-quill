package name.vbraun.lib.pen;

import android.util.Log;
import android.view.MotionEvent;

public class Hardware {
	private final static String TAG = "PenHardware";
	
	private final static Hardware instance = new Hardware();
	public static Hardware getHardware() {
		return instance;
	}
	
	private final String model;
	private final boolean mHasDedicatedPen;
	
	private final PenEvent mPenEvent;
	
	private Hardware() {
		model = android.os.Build.MODEL;
        Log.v(TAG, "Model = >"+model+"<");
		if (model == "ThinkPad Tablet") {
			mHasDedicatedPen = true;
			mPenEvent = new PenEventThinkPadTablet();
		} else {
			// defaults
			mHasDedicatedPen = false;
			mPenEvent = new PenEvent();
		}
	}
	
	
	public boolean hasDedicatedPen() {
		return mHasDedicatedPen;
	}
	
	public boolean isPenEvent(MotionEvent event) {
		return mPenEvent.isPenEvent(event);
	}
}
