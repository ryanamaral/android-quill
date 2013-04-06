package name.vbraun.view.write;

import junit.framework.Assert;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;


/**
 * Touch handler for a passive (capacitive) pen.
 * @author vbraun
 *
 */
public class TouchHandlerPassivePen 
	extends TouchHandlerPenABC {
	private final static String TAG = "TouchHandlerPassivePen";

	private int penID = -1;
	private float oldPressure, newPressure; 
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private long oldT, newT;
	
	protected TouchHandlerPassivePen(HandwriterView view) {
		super(view);
	}

	@Override
	protected void destroy() {
	}
	
	@Override
	protected void interrupt() {
		super.interrupt();
		penID = -1;
	}
	
	@Override
	protected boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (penID == -1 || N == 0) return true;
			int penIdx = event.findPointerIndex(penID);
			if (penIdx == -1) return true;
			oldT = newT;
			newT = System.currentTimeMillis();
			// Log.v(TAG, "ACTION_MOVE index="+pen+" pointerID="+penID);
			oldX = newX;
			oldY = newY;
			oldPressure = newPressure;
			newX = event.getX(penIdx);
			newY = event.getY(penIdx);
			newPressure = event.getPressure(penIdx);
			if (newT-oldT > 300) { // sometimes ACTION_UP is lost, why?
				Log.v(TAG, "Timeout in ACTION_MOVE, "+(newT-oldT));
				oldX = newX; oldY = newY;
				saveStroke();
				position_x[0] = newX;
				position_y[0] = newY;
				pressure[0] = newPressure;
				N = 1;
			}
			drawOutline(oldX, oldY, newX, newY, oldPressure, newPressure);
			
			int n = event.getHistorySize();
			if (N+n+1 >= Nmax) saveStroke();
			for (int i = 0; i < n; i++) {
				position_x[N+i] = event.getHistoricalX(penIdx, i);
				position_y[N+i] = event.getHistoricalY(penIdx, i);
				pressure[N+i] = event.getHistoricalPressure(penIdx, i);
			}
			position_x[N+n] = newX;
			position_y[N+n] = newY;
			pressure[N+n] = newPressure;
			N = N+n+1;
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {
			Assert.assertTrue(event.getPointerCount() == 1);
			newT = System.currentTimeMillis();
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				return true;
			}
			if (view.isOnPalmShield(event))
				return true;
			if (getPage().is_readonly) {
				view.toastIsReadonly();
				return true;
			}
			position_x[0] = newX = event.getX();
			position_y[0] = newY = event.getY();
			pressure[0] = newPressure = event.getPressure();
			N = 1;
			penID = event.getPointerId(0);
			initPenStyle();
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				// Log.v(TAG, "ACTION_UP: Got "+N+" points.");
				saveStroke();
				view.callOnStrokeFinishedListener();
			}
			penID = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			N = 0;
			penID = -1;
			redraw();
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			newT = System.currentTimeMillis();
			if (view.isOnPalmShield(event))
				return true;
			if (getPage().is_readonly) {
				view.toastIsReadonly();
				return true;
			}
			if (penID != -1)
				return true;
			int idx = event.getActionIndex();
			position_x[0] = newX = event.getX(idx);
			position_y[0] = newY = event.getY(idx);
			pressure[0] = newPressure = event.getPressure(idx);
			N = 1;
			penID = event.getPointerId(idx);
			initPenStyle();
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_UP) {
			int idx = event.getActionIndex();
			int id = event.getPointerId(idx);
			if (id == penID) {
				Log.v(TAG, "ACTION_POINTER_UP: Got "+N+" points.");
				saveStroke();
				N = 0;
				view.callOnStrokeFinishedListener();
			}
			penID = -1;
			return true;
		}
		return false;
	}

	@Override
	protected void draw(Canvas canvas, Bitmap bitmap) {
		canvas.drawBitmap(bitmap, 0, 0, null);
	}
	
	

}
