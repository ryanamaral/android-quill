package name.vbraun.lib.pen;

import junit.framework.Assert;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Hack for the ThinkPad Tablet to detect the pen button
 * @author vbraun
 *
 */
public class ViewHackThinkpad 
	extends View 
	implements View.OnLongClickListener {
	
	private final static String TAG = "ViewHackThinkpad";
	private final Handler handler = new Handler();
	
	public ViewHackThinkpad(Context context) {
		super(context);
		setLongClickable(true);
		setOnLongClickListener(this);
	}
	
	private boolean afterActionDown = false;
	protected static boolean buttonPressed = false;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.e(TAG, "onTouchEvent");
		// this will only be called for ACTION_DOWN because we return false
		Assert.assertTrue(event.getActionMasked() == MotionEvent.ACTION_DOWN);
		afterActionDown = true;
		handler.postDelayed(timer, 200);
		buttonPressed = false;
		super.onTouchEvent(event);   // important to trigger the long click
		return false;
	}
	
	/**
	 * Timer to cancel a real long click before it is delivered
	 */
	private Runnable timer = new Runnable() {
			public void run() {
				// Log.e(TAG,"timer");
				afterActionDown = false;
				cancelLongPress();
			};
		};
	
	/* 
	 * The ThinkPad Tablet will call this immediately after ACTION_DOWN if the button is pressed
	 * @see android.view.View.OnLongClickListener#onLongClick(android.view.View)
	 */
	public boolean onLongClick(View v) {
		handler.removeCallbacks(timer);
		if (!afterActionDown) return false;
		// Log.e(TAG, "button");
		buttonPressed = true;
		return true;
	}

	
}
