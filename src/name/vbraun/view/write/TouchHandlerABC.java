package name.vbraun.view.write;

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
	
	protected Context getContext() {
		return view.getContext();
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
	
}
