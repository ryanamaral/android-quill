package name.vbraun.view.write;

import name.vbraun.lib.pen.Hardware;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * Abstract base class for touch handlers.
 * A touch handler handles input to HandwriterView. It can also draw temporary stuff on top.
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
	 * @param event The MotionEvent
	 * @return Boolean. True if the event should be considered as coming from the pen
	 */
	protected boolean useForWriting(MotionEvent event) {
		return !view.onlyPenInput || Hardware.isPenEvent(event);
	}

	/** 
	 * Whether to use the MotionEvent as finger touch
	 * @param event The MotionEvent
	 * @return Boolean. True if the event should be considered as coming from the user's hand, e.g. for move/zoom
	 */
	protected boolean useForTouch(MotionEvent event) {
		return !view.onlyPenInput || (view.onlyPenInput && !Hardware.isPenEvent(event));
	}
	
	/**
	 * This will be called before we switch to another touch handler. It 
	 * is important that all non-persistent decoration (extra widgets or 
	 * temporary stuff drawn on the page) is removed.
	 */
	protected abstract void destroy();
	
	protected abstract boolean onTouchEvent(MotionEvent event);
	
	
	/**
	 * The onDraw handle. Will be called by TouchHandlerView.onView
	 * @param canvas The canvas to draw to
	 * @param bitmap The bitmap of the currently shown part of the page. Usually you'll want to call
	 * 				 canvas.drawBitmap(bitmap, 0, 0, null) to paint it onto the canvas, but for example 
	 *               move/zoom paints it only after an affine transformation.
	 */
	protected abstract void onDraw(Canvas canvas, Bitmap bitmap);
	
	/**
	 * Interrupt the current action, e.g. while drawing a line the user pressed the back key.
	 */
	protected void interrupt() {};
}
