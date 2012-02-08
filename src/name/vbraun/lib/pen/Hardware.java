package name.vbraun.lib.pen;

import java.util.ArrayList;

import junit.framework.Assert;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

public class Hardware {
	private final static String TAG = "PenHardware";
	
	private final String model;
	private final boolean mHasPenDigitizer;
	private final boolean mHasPressureSensor;
	private final PenEvent mPenEvent;
	
	private static Hardware cachedInstance = null;
	
	private static ArrayList<String> tabletMODELwithoutPressure = new ArrayList<String>() {
		private static final long serialVersionUID = 1868225200818950866L; 	{
	    add("K1");   // Lenovo K1
	    add("A500");  // Acer Iconia A500
	    add("A501");  // Acer Iconia A501
	    add("AT1S0");
	    add("Toshiba Thrive");
	    add("GT-P1000"); add("GT-P1000L"); add("GT-P1000N"); add("SGH-T849"); // Galaxy tab 10.1"
	    add("GT-P7510");  // Galaxy tab 10.1"
	    add("GT-P7501");  // Galaxy tab 10.1N"
	    add("GT-P6810");  // Samsung Galaxy Tab 7.7
	    add("GT-P6210");  // Samsung Galaxy Tab 7.0 Plus
	    add("Galaxy Nexus");  // Google Galaxy Nexus
	}};
	
	public Hardware(Context context) {
		if (cachedInstance != null) { 
			model = cachedInstance.model;
			mHasPenDigitizer = cachedInstance.mHasPenDigitizer;
			mPenEvent = cachedInstance.mPenEvent;
			mHasPressureSensor = cachedInstance.mHasPressureSensor;
			return; 
		}
		model = android.os.Build.MODEL;
		if (model.equalsIgnoreCase("ThinkPad Tablet")) {  // Lenovo ThinkPad Tablet
			mHasPenDigitizer = true;
			mHasPressureSensor = true;
			mPenEvent = new PenEventThinkPadTablet();
		} else {
			// defaults; this works on HTC devices but might be more general
			mHasPenDigitizer = context.getPackageManager().hasSystemFeature("android.hardware.touchscreen.pen");
			mHasPressureSensor = !tabletMODELwithoutPressure.contains(model);
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
		return cachedInstance.mHasPressureSensor;
	}

	public static boolean isPenEvent(MotionEvent event) {
		Assert.assertNotNull(cachedInstance);
		return cachedInstance.mPenEvent.isPenEvent(event);
	}
}
