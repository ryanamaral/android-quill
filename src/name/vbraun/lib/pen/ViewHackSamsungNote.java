package name.vbraun.lib.pen;

import name.vbraun.lib.pen.HardwareButtonListener.Type;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

@TargetApi(14)
public class ViewHackSamsungNote extends View {
	private final static String TAG = "ViewHackSamsungNote";
	
	public ViewHackSamsungNote(Context context) {
		super(context);
	}
	
	private static boolean pressed = false;
	
	@Override
	public boolean onHoverEvent(MotionEvent event) {
		boolean now = (event.getButtonState() == 2);
		boolean changed = (now ^ this.pressed); 
		if (changed && now) {
			Hardware hw = Hardware.getInstance(getContext());
			hw.callOnHardwareButtonListener(Type.DELETE);
		}
		this.pressed = now;
		// Log.e(TAG, "Hover "+event.getButtonState());
		return super.onHoverEvent(event);
	}
	
}
