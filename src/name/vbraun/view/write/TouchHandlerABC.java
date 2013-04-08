package name.vbraun.view.write;

import name.vbraun.lib.pen.Hardware;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;
import android.view.MotionEvent;

/**
 * Abstract base class for touch handlers. A touch handler handles input to
 * HandwriterView. It can also draw temporary stuff on top.
 * 
 * @author vbraun
 * 
 */
public abstract class TouchHandlerABC {

	protected HandwriterView view;

	protected TouchHandlerABC(HandwriterView view) {
		this.view = view;
	}

	// Convenience functions to access the view's settings

	protected Context getContext() {
		return view.getContext();
	}

	protected Page getPage() {
		return view.getPage();
	}

	public boolean getOnlyPenInput() {
		return view.getOnlyPenInput();
	}

	public boolean getDoubleTapWhileWriting() {
		return view.getDoubleTapWhileWriting();
	}

	public boolean getMoveGestureWhileWriting() {
		return view.getMoveGestureWhileWriting();
	}

	public int getMoveGestureMinDistance() {
		return view.getMoveGestureMinDistance();
	}

	public float getScaledPenThickness() {
		return Stroke.getScaledPenThickness(view.getPage().transformation, view.getPenThickness());
	}

	/**
	 * Redraw the page
	 */
	protected void redraw() {
		getPage().draw(view.canvas);
		view.invalidate();
	}

	/**
	 * Whether to use the MotionEvent for writing
	 * 
	 * @param event
	 *            The MotionEvent
	 * @return Boolean. True if the event should be considered as coming from
	 *         the pen
	 */
	protected boolean useForWriting(MotionEvent event) {
		return !view.onlyPenInput || Hardware.isPenEvent(event);
	}

	/**
	 * Whether to use the MotionEvent as finger touch
	 * 
	 * @param event
	 *            The MotionEvent
	 * @return Boolean. True if the event should be considered as coming from
	 *         the user's hand, e.g. for move/zoom
	 */
	protected boolean useForTouch(MotionEvent event) {
		return !view.onlyPenInput || (view.onlyPenInput && !Hardware.isPenEvent(event));
	}

	/**
	 * This will be called before we switch to another touch handler. It is
	 * important that all non-persistent decoration (extra widgets or temporary
	 * stuff drawn on the page) is removed.
	 */
	protected abstract void destroy();

	protected abstract boolean onTouchEvent(MotionEvent event);

	/**
	 * Called from the HandWriterView's onDraw() method while the touch
	 * interaction is in progress
	 * 
	 * @param canvas
	 *            The canvas to draw to
	 * @param bitmap
	 *            The bitmap of the currently shown part of the page. Usually
	 *            you'll want to call canvas.drawBitmap(bitmap, 0, 0, null) to
	 *            paint it onto the canvas. But the touch handler may want to
	 *            draw some user feedback in addition (or instead). For example,
	 *            the pen touch handler draws a rough preview of the pen stroke.
	 */
	protected abstract void draw(Canvas canvas, Bitmap bitmap);

	/**
	 * Interrupt the current action, e.g. the user pressed the back key while drawing a line.
	 */
	protected void interrupt() {
	};
	
	/**
	 * Compute new transform after a pinch-zoom gesture
	 */
	protected Transformation pinchZoomTransform(final Transformation transformation, 
			float oldX1, float newX1, float oldX2, float newX2, 
			float oldY1, float newY1, float oldY2, float newY2) {
		
		float page_offset_x = transformation.offset_x;
		float page_offset_y = transformation.offset_y;
		float page_scale = transformation.scale;
		float scale = pinchZoomScaleFactor(oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
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
		return new Transformation(new_offset_x, new_offset_y, new_page_scale);
	}
	
	/**
	 * Compute the new scale factor for a pinch-zoom gesture 	
	 */
	protected float pinchZoomScaleFactor(
			float oldX1, float newX1, float oldX2, float newX2, 
			float oldY1, float newY1, float oldY2, float newY2) {
		if (view.getMoveGestureFixZoom())
			return 1f;
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
			return 1f;
		}
		return scale;
	}
	
	private RectF mRectF = new RectF();
	private Rect  mRect  = new Rect();

	/**
	 * Draw a preview of the pinch-zoom gesture
	 * 
	 * This method just paints the region in the bitmap to the canvas where it
	 * would be after a pinch-zoom gesture. This is much faster than actually
	 * redrawing the bitmap with the new transformation, so we use this shortcut
	 * during the gesture. Only when the gesture is finished do we redraw the
	 * bitmap with the new zoom and offset.
	 * 
	 */
	protected void drawPinchZoomPreview(Canvas canvas, Bitmap bitmap,
			float oldX1, float newX1, float oldX2, float newX2, 
			float oldY1, float newY1, float oldY2, float newY2) {			
		canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
		float W = canvas.getWidth();
		float H = canvas.getHeight();
		float scale = pinchZoomScaleFactor(oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
		float x0 = (oldX1 + oldX2)/2;
		float y0 = (oldY1 + oldY2)/2;
		float x1 = (newX1 + newX2)/2;
		float y1 = (newY1 + newY2)/2;
		mRectF.set(-x0*scale+x1, -y0*scale+y1, (-x0+W)*scale+x1, (-y0+H)*scale+y1);
		mRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.drawBitmap(bitmap, mRect, mRectF, (Paint)null);
	}
}
