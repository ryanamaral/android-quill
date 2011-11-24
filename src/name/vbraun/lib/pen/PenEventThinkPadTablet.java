package name.vbraun.lib.pen;

import android.view.MotionEvent;

public class PenEventThinkPadTablet extends PenEvent {

	@Override
	public boolean isPenEvent(MotionEvent event) {
		return event.getTouchMajor() == 0.0f;
	}
	
	
}
