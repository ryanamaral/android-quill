package name.vbraun.lib.pen;

import android.view.KeyEvent;
import android.view.MotionEvent;

public class PenEventHTC extends PenEvent {
	// The device reports pen events like Ice Cream Sandwich
	// E.g. HTC Honeycomb devices
	
	final static int SOURCE_STYLUS = 0x00004002;

	@Override
	public boolean isPenEvent(MotionEvent event) {
		// == InputDevice.SOURCE_STYLUS  in ICS
		return (event.getSource() & SOURCE_STYLUS) == SOURCE_STYLUS;
	}
	
	@Override
	public boolean isPenButtonPressed(MotionEvent event) {
		if (!isPenEvent(event)) return false;
		final int meta = event.getMetaState();
		return (meta & KeyEvent.META_ALT_ON) == KeyEvent.META_ALT_ON;
	}
	
	@Override
	public boolean isPenButtonAltPressed(MotionEvent event) {
		if (!isPenEvent(event)) return false;
		final int meta = event.getMetaState();
		return (meta & KeyEvent.META_SHIFT_ON) == KeyEvent.META_SHIFT_ON;
	}
	

}
