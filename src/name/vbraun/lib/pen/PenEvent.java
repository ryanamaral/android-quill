package name.vbraun.lib.pen;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class PenEvent {
	private final static String TAG = "PenEvent";
	
	public boolean isPenEvent(MotionEvent event) {
		return false;
	}
	
	public boolean isPenButtonPressed(MotionEvent event) {
		return false;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.e(TAG, "onKeyDown "+keyCode+" "+event);
		return false;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.e(TAG, "onKeyUp "+keyCode+" "+event);
		return false;
	}

	public void addViewHack(ViewGroup viewGroup) {
	}
}
