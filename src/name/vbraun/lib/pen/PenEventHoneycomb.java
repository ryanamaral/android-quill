package name.vbraun.lib.pen;

import android.view.MotionEvent;

public class PenEventHoneycomb extends PenEvent {
	// The device reports pen events like Ice Cream Sandwich
	// E.g. HTC Honeycomb devices
	
	final static int SOURCE_STYLUS = 0x00004002;

	@Override
	public boolean isPenEvent(MotionEvent event) {
		// == InputDevice.SOURCE_STYLUS  in ICS
		return (event.getSource() & SOURCE_STYLUS) == SOURCE_STYLUS;
	}
	
}
