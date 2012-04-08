package name.vbraun.lib.pen;

import android.view.MotionEvent;
import android.view.ViewGroup;

public class PenEventThinkPadTablet extends PenEvent {

	@Override
	public boolean isPenEvent(MotionEvent event) {
		return event.getTouchMajor() == 0.0f;
	}
	
	@Override
	public boolean isPenButtonPressed(MotionEvent event) {
		return InterceptorView.buttonPressed;
	}
	
	public void addViewHack(ViewGroup viewGroup) {
		InterceptorView v = new InterceptorView(viewGroup.getContext());
		viewGroup.addView(v);
	}

}
