package name.vbraun.lib.pen;

import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

public class PenEventSamsungNote extends PenEvent {
	private static final String TAG = "PenEventSamsungNote";
	
	/**
	 * Hello Samsung, why are you not setting SOURCE_STYLUS on your pen?
	 */
	final static int SOURCE_S_PEN = InputDevice.SOURCE_KEYBOARD | InputDevice.SOURCE_CLASS_POINTER; 
	
	public boolean isPenEvent(MotionEvent event) {
		//		InputDevice dev = event.getDevice();
		//		Log.v(TAG, "Touch: "+dev.getId()+" "+dev.getName()+" "+event.getSource()+" "+dev.getSources()+" ");
		return (event.getDevice().getSources() & SOURCE_S_PEN) == SOURCE_S_PEN;
	}
	
}
