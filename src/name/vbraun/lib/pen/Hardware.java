package name.vbraun.lib.pen;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

public class Hardware {
	private final static String TAG = "PenHardware";
	
	private final String model;
	private final boolean mHasPenDigitizer;
	private final PenEvent mPenEvent;
	
	public Hardware(Context context) {
		model = android.os.Build.MODEL;
		if (model.equalsIgnoreCase("ThinkPad Tablet")) {
			mHasPenDigitizer = true;
			mPenEvent = new PenEventThinkPadTablet();
		} else {
			// defaults; this works on HTC devices but might be more general
			mHasPenDigitizer = context.getPackageManager().hasSystemFeature("android.hardware.touchscreen.pen");
			if (mHasPenDigitizer) 
				mPenEvent = new PenEventICS();
			else
				mPenEvent = new PenEvent();
		}
        Log.v(TAG, "Model = >"+model+"<, pen digitizer: "+mHasPenDigitizer);
	}
	
	// whether the device has an active pen
	public boolean hasPenDigitizer() {
		return mHasPenDigitizer;
	}
	
	public boolean isPenEvent(MotionEvent event) {
		return mPenEvent.isPenEvent(event);
	}
}
