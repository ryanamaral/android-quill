package name.vbraun.view.write;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;
import android.view.MotionEvent;


public class TouchHandlerMoveZoom extends TouchHandlerABC {

	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldX1, oldY1, newX1, newY1;  // for 1st finger
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;
	private final RectF mRectF = new RectF();
	private final Rect mRect = new Rect();

	protected TouchHandlerMoveZoom(HandwriterView view) {
		super(view);
	}

	@Override
	protected void destroy() {
	}

	private float pinchZoomScaleFactor() {
		float dx, dy;
		dx = oldX1-oldX2;
		dy = oldY1-oldY2;
		float old_distance = FloatMath.sqrt(dx*dx + dy*dy);
		if (old_distance < 10) {
			// Log.d("TAG", "old_distance too small "+old_distance);
			return 1;
		}
		dx = newX1-newX2;
		dy = newY1-newY2;
		float new_distance = FloatMath.sqrt(dx*dx + dy*dy);
		float scale = new_distance / old_distance;
		if (scale < 0.1f || scale > 10f) {
			// Log.d("TAG", "ratio out of bounds "+new_distance);
			return 1;
		}
		return scale;
	}
	
	@Override
	protected boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (fingerId1 == -1) return true;
			if (fingerId2 == -1) {  // move
				int idx = event.findPointerIndex(fingerId1);
				if (idx == -1) return true;
				newX1 = event.getX(idx);
				newY1 = event.getY(idx);
			} else { // pinch-to-zoom
				int idx1 = event.findPointerIndex(fingerId1);
				int idx2 = event.findPointerIndex(fingerId2);
				if (idx1 == -1 || idx2 == -1)
					return true;
				newX1 = event.getX(idx1);
				newY1 = event.getY(idx1);
				newX2 = event.getX(idx2);
				newY2 = event.getY(idx2);					
			}
			view.invalidate();
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {  // start move
			oldX1 = newX1 = event.getX();
			oldY1 = newY1 = event.getY();
			newT = System.currentTimeMillis();
			if (Math.abs(newT-oldT) < 250) { // double-tap
				view.centerAndFillScreen(newX1, newY1);
				return true;
			}
			oldT = newT;
			fingerId1 = event.getPointerId(0); 
			fingerId2 = -1;
			// Log.v(TAG, "ACTION_DOWN "+fingerId1);
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {  // stop move
			if (fingerId1 == -1) return true;  // ignore after pinch-to-zoom
			if (fingerId2 != -1) { // undelivered ACTION_POINTER_UP
				fingerId1 = fingerId2 = -1;
				view.invalidate();
				return true;		
			}
			newX1 = event.getX();
			newY1 = event.getY();
			float dx = newX1-oldX1;
			float dy = newY1-oldY1; 
			// Log.v(TAG, "ACTION_UP "+fingerId1+" dx="+dx+", dy="+dy);
			Page page = getPage();
			page.setTransform(page.transformation.offset(dx,dy), view.canvas);
			page.draw(view.canvas);
			view.invalidate();
			fingerId1 = fingerId2 = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_DOWN) {  // start pinch
			if (fingerId1 == -1) return true; // ignore after pinch-to-zoom finished
			if (fingerId2 != -1) return true; // ignore more than 2 fingers
			int idx2 = event.getActionIndex();
			oldX2 = newX2 = event.getX(idx2);
			oldY2 = newY2 = event.getY(idx2);
			fingerId2 = event.getPointerId(idx2);
			// Log.v(TAG, "ACTION_POINTER_DOWN "+fingerId2+" + "+fingerId1);
		}
		else if (action == MotionEvent.ACTION_POINTER_UP) {  // stop pinch
			if (fingerId1 == -1) return true; // ignore after pinch-to-zoom finished
			int idx = event.getActionIndex();
			int Id = event.getPointerId(idx);
			if (fingerId1 != Id && fingerId2 != Id) // third finger up?
				return true;
			// Log.v(TAG, "ACTION_POINTER_UP "+fingerId2+" + "+fingerId1);
			// compute scale factor
			Page page = getPage();
			float page_offset_x = page.transformation.offset_x;
			float page_offset_y = page.transformation.offset_y;
			float page_scale = page.transformation.scale;
			float scale = pinchZoomScaleFactor();
			float new_page_scale = page_scale * scale;
			// clamp scale factor
			float W = view.canvas.getWidth();
			float H = view.canvas.getHeight();
			float max_WH = Math.max(W, H);
			float min_WH = Math.min(W, H);
			new_page_scale = Math.min(new_page_scale, 5*max_WH);
			new_page_scale = Math.max(new_page_scale, 0.4f*min_WH);
			scale = new_page_scale / page_scale;
			// compute offset
			float x0 = (oldX1 + oldX2)/2;
			float y0 = (oldY1 + oldY2)/2;
			float x1 = (newX1 + newX2)/2;
			float y1 = (newY1 + newY2)/2;
			float new_offset_x = page_offset_x*scale-x0*scale+x1;
			float new_offset_y = page_offset_y*scale-y0*scale+y1;
			// perform pinch-to-zoom here
			page.setTransform(new_offset_x, new_offset_y, new_page_scale, view.canvas);
			page.draw(view.canvas);
			view.invalidate();
			fingerId1 = fingerId2 = -1;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			fingerId1 = fingerId2 = -1;
			return true;
		}
		return false;
	}

	@Override
	protected void draw(Canvas canvas, Bitmap bitmap) {
		if (fingerId2 != -1) {
			// pinch-to-zoom preview by scaling bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float W = canvas.getWidth();
			float H = canvas.getHeight();
			float scale = pinchZoomScaleFactor();
			float x0 = (oldX1 + oldX2)/2;
			float y0 = (oldY1 + oldY2)/2;
			float x1 = (newX1 + newX2)/2;
			float y1 = (newY1 + newY2)/2;
			mRectF.set(-x0*scale+x1, -y0*scale+y1, (-x0+W)*scale+x1, (-y0+H)*scale+y1);
			mRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			canvas.drawBitmap(bitmap, mRect, mRectF, (Paint)null);
		} else if (fingerId1 != -1) {
			// move preview by translating bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float x = newX1-oldX1;
			float y = newY1-oldY1; 
			canvas.drawBitmap(bitmap, x, y, null);
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
	}

}
