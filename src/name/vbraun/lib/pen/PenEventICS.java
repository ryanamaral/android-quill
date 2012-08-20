package name.vbraun.lib.pen;

import android.annotation.TargetApi;
import android.view.MotionEvent;

@TargetApi(14)
public class PenEventICS extends PenEvent {
	
	public boolean isPenEvent(MotionEvent event) {
		return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
	}

	@Override
	public boolean isPenButtonPressed(MotionEvent event) {
		// doesn't work, Note uses button for gestures 
		return event.getButtonState() == MotionEvent.BUTTON_PRIMARY;
	}
	
	@Override
	public boolean isPenButtonAltPressed(MotionEvent event) {
		// doesn't work, Note uses button for gestures 
		return event.getButtonState() == MotionEvent.BUTTON_SECONDARY;
	}

}
