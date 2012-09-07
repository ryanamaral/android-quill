package name.vbraun.lib.pen;

import android.annotation.TargetApi;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewGroup;

@TargetApi(14)
public class PenEventSamsungNote extends PenEvent {
	private static final String TAG = "PenEventSamsungNote";

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

	public void addViewHack(ViewGroup viewGroup) {
		ViewHackSamsungNote v = new ViewHackSamsungNote(viewGroup.getContext());
		viewGroup.addView(v);
	}
	
}
