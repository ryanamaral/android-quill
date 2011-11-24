package name.vbraun.lib.pen;

import android.view.MotionEvent;

public class PenEventHTC extends PenEvent {

	@Override
	public boolean isPenEvent(MotionEvent event) {
		return true;
	}
	
	
}
