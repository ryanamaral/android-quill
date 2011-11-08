package com.write.Quill;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import junit.framework.Assert;

import com.write.Quill.Stroke.PenType;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path;
import android.util.FloatMath;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.os.AsyncTask;

public class HandwriterView extends View {
	private static final String TAG = "Handwrite";

	private Bitmap bitmap;
	private Canvas canvas;
	private Toast toast;
	private final Rect mRect = new Rect();
	private final RectF mRectF = new RectF();
	private final Paint pen;
	private int penID = -1;
	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldPressure, newPressure; 
	private float oldX, oldY, newX, newY;  // main pointer
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;
	private TagOverlay overlay = null;
	
    private int N = 0;
	private static final int Nmax = 1024;
	private float[] position_x = new float[Nmax];
	private float[] position_y = new float[Nmax];
	private float[] pressure = new float[Nmax];

	// persistent data
	Page page;
	
	// preferences
	protected int pen_thickness = 2;
	protected PenType pen_type = PenType.FOUNTAINPEN;
	protected int pen_color = Color.BLACK;
	protected boolean only_pen_input = true;
	protected boolean doubleTapWhileWriting = true;
	
	public void setPenType(PenType t) {
		pen_type = t;
	}

	public void setPenColor(int c) {
		pen_color = c;
		pen.setARGB(Color.alpha(c), Color.red(c), Color.green(c), Color.blue(c));
	}
	
	public void setPenThickness(int thickness) {
		pen_thickness = thickness;
	}
	
	public void setPagePaperType(Page.PaperType paper_type) {
		page.setPaperType(paper_type);
		page.draw(canvas);
		invalidate();
	}

	public void setPageAspectRatio(float aspect_ratio) {
		page.setAspectRatio(aspect_ratio);
		setPageAndZoomOut(page);
		invalidate();
	}

	public HandwriterView(Context c) {
		super(c);
		setFocusable(true);
		pen = new Paint();
		pen.setAntiAlias(true);
		pen.setARGB(0xff, 0, 0, 0);	
		pen.setStrokeCap(Paint.Cap.ROUND);
	}

	public void setPageAndZoomOut(Page new_page) {
		if (new_page == null) return;
		page = new_page;
		updateOverlay();
		if (canvas == null) return;
		// if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) 
		float H = canvas.getHeight();
		float W = canvas.getWidth();
		float dimension = Math.min(H, W/page.aspect_ratio);
		float h = dimension; 
		float w = dimension*page.aspect_ratio;
		if (h<H)
			page.setTransform(0, (H-h)/2, dimension);
		else if (w<W)
			page.setTransform((W-w)/2, 0, dimension);
		else
			page.setTransform(0, 0, dimension);
		Log.v(TAG, "set_page at scale "+page.transformation.scale+" canvas w="+W+" h="+H);
		page.draw(canvas);
		invalidate();
	}
	
	public void updateOverlay() {
		overlay = new TagOverlay(page.tags);
		invalidate();
	}

	public void centerAndFillScreen(float xCenter, float yCenter) {
		float page_offset_x = page.transformation.offset_x;
		float page_offset_y = page.transformation.offset_y;
		float page_scale = page.transformation.scale;
		float W = canvas.getWidth();
		float H = canvas.getHeight();
		float scaleToFill = Math.max(H, W / page.aspect_ratio);
		float scaleToSeeAll = Math.min(H, W / page.aspect_ratio);
		float scale;
		boolean seeAll = (page_scale == scaleToFill); // toggle
		if (seeAll) 
			scale = scaleToSeeAll;
		else
			scale = scaleToFill;
		float x = (xCenter - page_offset_x) / page_scale * scale;
		float y = (yCenter - page_offset_y) / page_scale * scale;
		float dx, dy;
		if (seeAll) {
			dx = (W-scale*page.aspect_ratio)/2;
			dy = (H-scale)/2;
		} else if (scale == H) {
			dx = W/2-x;// + (-scale*page.aspect_ratio)/2;
			dy = 0;
		} else {
			dx = 0;
			dy = H/2-y;// + (-scale)/2;
		}
		page.setTransform(dx, dy, scale, canvas);
		page.draw(canvas);
		invalidate();
	}

	
	public void clear() {
		if (canvas == null || page == null) return;		
		page.strokes.clear();	
		page.draw(canvas);	
		invalidate();
	}
	
	protected void addStrokes(Object data) {
		Assert.assertTrue("unknown data", data instanceof LinkedList<?>);
		LinkedList<Stroke> new_strokes = (LinkedList<Stroke>)data;
		page.strokes.addAll(new_strokes);
	}
	
	@Override protected void onSizeChanged(int w, int h, int oldw,
			int oldh) {
		int curW = bitmap != null ? bitmap.getWidth() : 0;
		int curH = bitmap != null ? bitmap.getHeight() : 0;
		if (curW >= w && curH >= h) {
			return;
		}
		if (curW < w) curW = w;
		if (curH < h) curH = h;

		Bitmap newBitmap = Bitmap.createBitmap(curW, curH,
				Bitmap.Config.RGB_565);
		Canvas newCanvas = new Canvas();
		newCanvas.setBitmap(newBitmap);
		if (bitmap != null) {
			newCanvas.drawBitmap(bitmap, 0, 0, null);
		}
		bitmap = newBitmap;
		canvas = newCanvas;
		setPageAndZoomOut(page);
	}

	private float pinchZoomScaleFactor() {
		float dx, dy;
		dx = oldX-oldX2;
		dy = oldY-oldY2;
		float old_distance = FloatMath.sqrt(dx*dx + dy*dy);
		if (old_distance < 10) {
			// Log.d("TAG", "old_distance too small "+old_distance);
			return 1;
		}
		dx = newX-newX2;
		dy = newY-newY2;
		float new_distance = FloatMath.sqrt(dx*dx + dy*dy);
		float scale = new_distance / old_distance;
		if (scale < 0.1f || scale > 10f) {
			// Log.d("TAG", "ratio out of bounds "+new_distance);
			return 1;
		}
		return scale;
	}
	
	public float getScaledPenThickness() {
		return Stroke.getScaledPenThickness(page.transformation, pen_thickness);
	}
	
	@Override protected void onDraw(Canvas canvas) {
		if (bitmap == null) return;
		if (pen_type == Stroke.PenType.MOVE && fingerId2 != -1) {
			// pinch-to-zoom preview by scaling bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float W = canvas.getWidth();
			float H = canvas.getHeight();
			float scale = pinchZoomScaleFactor();
			float x0 = (oldX + oldX2)/2;
			float y0 = (oldY + oldY2)/2;
			float x1 = (newX + newX2)/2;
			float y1 = (newY + newY2)/2;
			mRectF.set(-x0*scale+x1, -y0*scale+y1, (-x0+W)*scale+x1, (-y0+H)*scale+y1);
			mRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			canvas.drawBitmap(bitmap, mRect, mRectF, (Paint)null);
		} else if (pen_type == Stroke.PenType.MOVE && fingerId1 != -1) {
			// move preview by translating bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float x = newX-oldX;
			float y = newY-oldY; 
			canvas.drawBitmap(bitmap, x, y, null);
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
		if (overlay != null) 
			overlay.draw(canvas);
	}

	@Override public boolean onTouchEvent(MotionEvent event) {
//		InputDevice dev = event.getDevice();
//		Log.v(TAG, "Touch: "+dev.getId()+" "+dev.getName()+" "+dev.getKeyboardType()+" "+dev.getSources()+" ");
//		Log.v(TAG, "Touch: "+event.getDevice().getName()
//				+" action="+event.getActionMasked()
//				+" pressure="+event.getPressure()
//				+" fat="+event.getTouchMajor()
//				+" penID="+penID+" ID="+event.getPointerId(0)+" N="+N);
		switch (pen_type) {
		case FOUNTAINPEN:
		case PENCIL:	
			return touchHandlerPen(event);
		case MOVE:
			return touchHandlerMoveZoom(event);
		case ERASER:
			return touchHandlerEraser(event);
		}
		return false;
	}
		
	private boolean touchHandlerEraser(MotionEvent event) {
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
			boolean erased = page.eraseStrokesIn(mRectF, canvas);
			if (erased)
				invalidate();
			oldX = newX;
			oldY = newY;
			return true;
		} else if (action == MotionEvent.ACTION_DOWN) {  // start move
			if (page.is_readonly) {
				toastIsReadonly();
				return true;
			}
			penID = event.getPointerId(0);
			oldX = newX = event.getX();
			oldY = newY = event.getY();
			return true;
		} else if (action == MotionEvent.ACTION_UP) {  
			penID = -1;
		}
		return false;
	}
	
	
	private boolean touchHandlerMoveZoom(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (fingerId1 == -1) return true;
			if (fingerId2 == -1) {  // move
				int idx = event.findPointerIndex(fingerId1);
				if (idx == -1) return true;
				newX = event.getX(idx);
				newY = event.getY(idx);
			} else { // pinch-to-zoom
				int idx1 = event.findPointerIndex(fingerId1);
				int idx2 = event.findPointerIndex(fingerId2);
				if (idx1 == -1 || idx2 == -1) return true;
				newX = event.getX(idx1);
				newY = event.getY(idx1);
				newX2 = event.getX(idx2);
				newY2 = event.getY(idx2);
			}
			invalidate();
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {  // start move
			oldX = newX = event.getX();
			oldY = newY = event.getY();
			newT = System.currentTimeMillis();
			if (Math.abs(newT-oldT) < 250) { // double-tap
				centerAndFillScreen(newX, newY);
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
				invalidate();
				return true;		
			}
			newX = event.getX();
			newY = event.getY();
			float dx = newX-oldX;
			float dy = newY-oldY; 
			// Log.v(TAG, "ACTION_UP "+fingerId1+" dx="+dx+", dy="+dy);
			page.setTransform(page.transformation.offset(dx,dy), canvas);
			page.draw(canvas);
			invalidate();
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
			float page_offset_x = page.transformation.offset_x;
			float page_offset_y = page.transformation.offset_y;
			float page_scale = page.transformation.scale;
			float scale = pinchZoomScaleFactor();
			float new_page_scale = page_scale * scale;
			// clamp scale factor
			float W = canvas.getWidth();
			float H = canvas.getHeight();
			float max_WH = Math.max(W, H);
			float min_WH = Math.min(W, H);
			new_page_scale = Math.min(new_page_scale, 5*max_WH);
			new_page_scale = Math.max(new_page_scale, 0.4f*min_WH);
			scale = new_page_scale / page_scale;
			// compute offset
			float x0 = (oldX + oldX2)/2;
			float y0 = (oldY + oldY2)/2;
			float x1 = (newX + newX2)/2;
			float y1 = (newY + newY2)/2;
			float new_offset_x = page_offset_x*scale-x0*scale+x1;
			float new_offset_y = page_offset_y*scale-y0*scale+y1;
			// perform pinch-to-zoom here
			page.setTransform(new_offset_x, new_offset_y, new_page_scale, canvas);
			page.draw(canvas);
			invalidate();
			fingerId1 = fingerId2 = -1;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			fingerId1 = fingerId2 = -1;
			return true;
		}
		return false;
	}
	
	// whether to use the MotionEvent for writing
	private boolean useForWriting(MotionEvent event) {
		return !only_pen_input || event.getTouchMajor() == 0.0f;
	}

	// whether to use the MotionEvent for move/zoom
	private boolean useForTouch(MotionEvent event) {
		return !only_pen_input || !useForWriting(event); 
	}

	private boolean touchHandlerPen(MotionEvent event) {
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
			drawOutline();
			
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
			newT = System.currentTimeMillis();
			if (useForTouch(event) && doubleTapWhileWriting && Math.abs(newT-oldT) < 250) {
				// double-tap
				centerAndFillScreen(event.getX(), event.getY());
				return true;
			}
			oldT = newT;
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				return true;
			}
			if (!useForWriting(event)) 
				return true;   // eat non-pen events
			// Log.v(TAG, "ACTION_DOWN");
			if (page.is_readonly) {
				toastIsReadonly();
				return true;
			}
			position_x[0] = newX = event.getX();
			position_y[0] = newY = event.getY();
			pressure[0] = newPressure = event.getPressure();
			N = 1;
			penID = event.getPointerId(0);
			pen.setStrokeWidth(getScaledPenThickness());
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			if (event.getPointerId(0) != penID) return true;
			// Log.v(TAG, "ACTION_UP: Got "+N+" points.");
			saveStroke();
			N = 0;
			penID = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			// e.g. you start with finger and use pen
			if (event.getPointerId(0) != penID) return true;
			// Log.v(TAG, "ACTION_CANCEL");
			N = 0;
			penID = -1;
			page.draw(canvas);
			invalidate();
			return true;
		}
		return false;
	}

	private void toastIsReadonly() {
		String s = "Page is readonly";
	   	if (toast == null)
        	toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
    	else {
    		toast.setText(s);
    	}
	   	toast.show();
	}
	
	private void saveStroke() {
		if (N==0) return;
		Stroke s = new Stroke(position_x, position_y, pressure, 0, N);
		PenHistory.add(pen_type, pen_thickness, pen_color);
		s.setPen(pen_type, pen_thickness, pen_color);
		if (page != null) {
			page.addStroke(s);
			page.draw(canvas, s.get_bounding_box());
		}
		N = 0;
		s.get_bounding_box().round(mRect);
		int extra = -(int)(getScaledPenThickness()/2) - 1;
		mRect.inset(extra, extra);
		invalidate(mRect);
	}
	
	
	private void drawOutline() {
		if (pen_type==PenType.FOUNTAINPEN) {
			float scaled_pen_thickness = getScaledPenThickness() * (oldPressure+newPressure)/2f;
			pen.setStrokeWidth(scaled_pen_thickness);
		}
		canvas.drawLine(oldX, oldY, newX, newY, pen);
		mRect.set((int)oldX, (int)oldY, (int)newX, (int)newY);
		mRect.sort();
		int extra = -(int)(pen.getStrokeWidth()/2) - 1;
		mRect.inset(extra, extra);
		invalidate(mRect);
	}
}
