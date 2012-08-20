package name.vbraun.lib.pen;

import android.annotation.TargetApi;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

public class PenEventSamsungNoteHoneycomb extends PenEvent {
	private static final String TAG = "PenEventSamsungNoteHoneycomb";

	/**
	 * Hello Samsung, why are you not setting SOURCE_STYLUS on your pen?
	 */
	final static int SOURCE_S_PEN = InputDevice.SOURCE_KEYBOARD
			| InputDevice.SOURCE_CLASS_POINTER;

	public boolean isPenEvent(MotionEvent event) {
		// if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
		// InputDevice dev = event.getDevice();
		// Log.v(TAG,
		// "Touch: "+dev.getId()+" "+dev.getName()+" "+dev.getKeyboardType()+" "+dev.getSources()+" ");
		// Log.v(TAG, "Touch: "+event.getDevice().getName()
		// +" action="+event.getActionMasked()
		// +" pressure="+event.getPressure()
		// +" fat="+event.getTouchMajor());
		// }
		// InputDevice dev = event.getDevice();
		// Log.v(TAG,
		// "Touch: "+dev.getId()+" "+dev.getName()+" "+event.getSource()+" "+dev.getSources()+" ");
		
		return (event.getDevice().getSources() & SOURCE_S_PEN) == SOURCE_S_PEN;
	}

}
