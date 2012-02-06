package name.vbraun.lib.pen;

import junit.framework.Assert;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

public class Hardware {
	private final static String TAG = "PenHardware";
	
	private final String model;
	private final boolean mHasPenDigitizer;
	private final PenEvent mPenEvent;
	
	private static Hardware cachedInstance = null;
	
	public Hardware(Context context) {
		if (cachedInstance != null) { 
			model = cachedInstance.model;
			mHasPenDigitizer = cachedInstance.mHasPenDigitizer;
			mPenEvent = cachedInstance.mPenEvent;
			return; 
		}
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
		cachedInstance = this;
        Log.v(TAG, "Model = >"+model+"<, pen digitizer: "+mHasPenDigitizer);
	}
	
	// whether the device has an active pen
	public static boolean hasPenDigitizer() {
		Assert.assertNotNull(cachedInstance);
		return cachedInstance.mHasPenDigitizer;
	}
	
	public static boolean hasPressureSensor() {
		Assert.assertNotNull(cachedInstance);
		return cachedInstance.mHasPenDigitizer;
	}

	public static boolean isPenEvent(MotionEvent event) {
		Assert.assertNotNull(cachedInstance);
		return cachedInstance.mPenEvent.isPenEvent(event);
	}
}
