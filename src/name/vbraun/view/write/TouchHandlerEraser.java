package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

public class TouchHandlerEraser extends TouchHandlerABC {

	private int penID = -1;
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private final RectF mRectF = new RectF();

	protected TouchHandlerEraser(HandwriterView view) {
		super(view);
	}
	
	@Override
	protected void destroy() {
	}

	@Override
	protected boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (penID == -1) return true;
			int idx = event.findPointerIndex(penID);
			if (idx == -1) return true;
			newX = event.getX(idx);
			newY = event.getY(idx);
			mRectF.set(oldX, oldY, newX, newY);
			mRectF.sort();
			mRectF.inset(-15, -15);
			view.eraseStrokesIn(mRectF);
			oldX = newX;
			oldY = newY;
			return true;
		} else if (action == MotionEvent.ACTION_DOWN) {  // start move
			if (getPage().is_readonly) {
				view.toastIsReadonly();
				return true;
			}
			if (view.isOnPalmShield(event)) 
				return true;
			if (!useForWriting(event)) 
				return true;   // eat non-pen events
			penID = event.getPointerId(0);
			oldX = newX = event.getX();
			oldY = newY = event.getY();
			return true;
		} else if (action == MotionEvent.ACTION_UP) { 
			if (penID == event.getPointerId(0))
				view.callOnStrokeFinishedListener();
			penID = -1;
		}
		return false;
	}

	@Override
	protected void onDraw(Canvas canvas, Bitmap bitmap) {
		canvas.drawBitmap(bitmap, 0, 0, null);
	}

}
