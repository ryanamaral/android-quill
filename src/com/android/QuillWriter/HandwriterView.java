package com.android.QuillWriter;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import com.android.QuillWriter.Stroke.PenType;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path;
import android.util.Log;
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
	private float oldX, oldY, newX, newY;
	private long oldT, newT;
	
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
	
	public void set_pen_type(PenType t) {
		pen_type = t;
	}

	public void set_pen_color(int c) {
		pen_color = c;
		pen.setARGB(Color.alpha(c), Color.red(c), Color.green(c), Color.blue(c));
	}
	
	public void set_pen_thickness(int thickness) {
		pen_thickness = thickness;
		pen.setStrokeWidth(pen_thickness);
	}
	
	public void set_page_paper_type(Page.PaperType paper_type) {
		page.set_paper_type(paper_type);
		page.draw(canvas);
		invalidate();
	}

	public void set_page_aspect_ratio(float aspect_ratio) {
		page.set_aspect_ratio(aspect_ratio);
		set_page_and_zoom_out(page);
		invalidate();
	}

	public HandwriterView(Context c) {
		super(c);
		setFocusable(true);
		pen = new Paint();
		pen.setAntiAlias(true);
		pen.setStrokeWidth(pen_thickness);
		pen.setARGB(0xff, 0, 0, 0);	
		pen.setStrokeCap(Paint.Cap.ROUND);
	}

	public void set_page_and_zoom_out(Page new_page) {
		if (new_page == null) return;
		page = new_page;
		if (canvas == null) return;
		// if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) 
		float H = canvas.getHeight();
		float W = canvas.getWidth();
		float dimension = Math.min(H, W/page.aspect_ratio);
		float h = dimension; 
		float w = dimension*page.aspect_ratio;
		if (h<H)
			page.set_transform(0, (H-h)/2, dimension);
		else if (w<W)
			page.set_transform((W-w)/2, 0, dimension);
		else
			page.set_transform(0, 0, dimension);
		Log.v(TAG, "set_page at scale "+page.scale+" canvas w="+W+" h="+H);
		page.draw(canvas);
		invalidate();
	}
	
	public void clear() {
		if (canvas == null || page == null) return;		
		page.strokes.clear();	
		page.draw(canvas);	
		invalidate();
	}
	
	protected void add_strokes(Object data) {
		assert data instanceof LinkedList<?>: "unknown data";
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

		if (bitmap != null) bitmap.recycle();
		Bitmap newBitmap = Bitmap.createBitmap(curW, curH,
				Bitmap.Config.RGB_565);
		Canvas newCanvas = new Canvas();
		newCanvas.setBitmap(newBitmap);
		if (bitmap != null) {
			newCanvas.drawBitmap(bitmap, 0, 0, null);
		}
		bitmap = newBitmap;
		canvas = newCanvas;
		set_page_and_zoom_out(page);
	}

	@Override protected void onDraw(Canvas canvas) {
		if (bitmap == null) return;
		if (pen_type == Stroke.PenType.MOVE && fingerId1 != -1) {
			float dx = newX-oldX;
			float dy = newY-oldY; 
			canvas.drawBitmap(bitmap, dx, dy, null);
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
	}

	@Override public boolean onTouchEvent(MotionEvent event) {
		Log.v(TAG, "Touch: "+event.getDevice().getName()
				+" action="+event.getActionMasked()
				+" pressure="+event.getPressure()
				+" fat="+event.getTouchMajor()
				+" penID="+penID+" ID="+event.getPointerId(0)+" N="+N);
		switch (pen_type) {
		case FOUNTAINPEN:
		case PENCIL:	
			return touch_handler_pen(event);
		case MOVE:
			return touch_handler_move_zoom(event);
		}
		return false;
	}
		
	private boolean touch_handler_move_zoom(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			newX = event.getX();
			newY = event.getY();
			invalidate();
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {
			oldX = newX = event.getX();
			oldY = newY = event.getY();
			fingerId1 = event.getPointerId(0);
			Log.v(TAG, "ACTION_DOWN "+fingerId1);
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			newX = event.getX();
			newY = event.getY();
			float dx = newX-oldX;
			float dy = newY-oldY; 
			Log.v(TAG, "ACTION_UP dx="+dx+", dy="+dy);
			page.set_transform(page.offset_x+dx, page.offset_y+dy, page.scale);
			page.draw(canvas);
			invalidate();
			fingerId1 = fingerId2 = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			fingerId1 = fingerId2 = -1;
			return true;
		}
		return false;
	}
	
	private boolean touch_handler_pen(MotionEvent event) {
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
			newX = event.getX(penIdx);
			newY = event.getY(penIdx);
			if (newT-oldT > 300) { // sometimes ACTION_UP is lost, why?
				Log.v(TAG, "Timeout in ACTION_MOVE, "+(newT-oldT));
				oldX = newX; oldY = newY;
				save_stroke();
				position_x[0] = newX;
				position_y[0] = newY;
				pressure[0] = event.getPressure(penIdx);
				N = 1;
			}
			drawOutline();
			
			int n = event.getHistorySize();
			if (N+n+1 >= Nmax) save_stroke();
			for (int i = 0; i < n; i++) {
				position_x[N+i] = event.getHistoricalX(penIdx, i);
				position_y[N+i] = event.getHistoricalY(penIdx, i);
				pressure[N+i] = event.getHistoricalPressure(penIdx, i);
			}
			position_x[N+n] = newX;
			position_y[N+n] = newY;
			pressure[N+n] = event.getPressure(penIdx);
			N = N+n+1;
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {
			if (penID != -1) return true;
			if (only_pen_input && event.getTouchMajor() != 0.0f)
					return true;   // eat non-pen events
			Log.v(TAG, "ACTION_DOWN");
			if (page.is_readonly) {
				toast_is_readonly();
				return true;
			}
			position_x[0] = newX = event.getX();
			position_y[0] = newY = event.getY();
			pressure[0] = event.getPressure();
			newT = System.currentTimeMillis();
			N = 1;
			penID = event.getPointerId(0);
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			if (event.getPointerId(0) != penID) return true;
			Log.v(TAG, "ACTION_UP: Got "+N+" points.");
			save_stroke();
			N = 0;
			penID = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			// e.g. you start with finger and use pen
			if (event.getPointerId(0) != penID) return true;
			Log.v(TAG, "ACTION_CANCEL");
			N = 0;
			penID = -1;
			page.draw(canvas);
			invalidate();
			return true;
		}
		return false;
	}

	private void toast_is_readonly() {
		String s = "Page is readonly";
	   	if (toast == null)
        	toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
    	else {
    		toast.setText(s);
    	}
	   	toast.show();
	}
	
	private void save_stroke() {
		if (N==0) return;
		Stroke s = new Stroke(position_x, position_y, pressure, 0, N);
		s.set_pen(pen_type, pen_thickness, pen_color);
		if (page != null) {
			page.add_stroke(s);
			page.draw(canvas, s.get_bounding_box());
		}
		s.get_bounding_box().round(mRect);
		invalidate(mRect);
		N = 0;
	}
	
	
	private void drawOutline() {
		canvas.drawLine(oldX, oldY, newX, newY, pen);
		mRect.set((int)oldX, (int)oldY, (int)newX, (int)newY);
		mRect.sort();
		mRect.inset(-pen_thickness/2-1, -pen_thickness/2-1);
		invalidate(mRect);
	}
}
