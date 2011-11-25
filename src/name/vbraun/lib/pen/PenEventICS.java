package name.vbraun.lib.pen;

import android.view.InputDevice;
import android.view.MotionEvent;

public class PenEventICS extends PenEvent {
	// The device reports pen events like Ice Cream Sandwich
	// E.g. HTC Honeycomb devices
	
	@Override
	public boolean isPenEvent(MotionEvent event) {
		return event.getSource() == 0x00004002;
		// == InputDevice.SOURCE_STYLUS  in ICS
	}
	
	
}
