package name.vbraun.view.write;

import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.Preferences;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics.Tool;

import junit.framework.Assert;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

public class HandwriterView extends ViewGroup {
	private static final String TAG = "Handwrite";
	
	public static final String KEY_LIST_PEN_INPUT_MODE = "pen_input_mode";
	public static final String KEY_DOUBLE_TAP_WHILE_WRITE = "double_tap_while_write";
	public static final String KEY_MOVE_GESTURE_WHILE_WRITING = "move_gesture_while_writing";
	public static final String KEY_PALM_SHIELD = "palm_shield";
	public static final String KEY_TOOLBOX_IS_ON_LEFT = "toolbox_left";
	private static final String KEY_TOOLBOX_IS_VISIBLE = "toolbox_is_visible";
	private static final String KEY_PEN_TYPE = "pen_type";
	private static final String KEY_PEN_COLOR = "pen_color";
	private static final String KEY_PEN_THICKNESS = "pen_thickness";
	private static final String KEY_ONLY_PEN_INPUT_OBSOLETE = "only_pen_input";
	
	// values for the preferences key KEY_LIST_PEN_INPUT_MODE
    public static final String STYLUS_ONLY = "STYLUS_ONLY";
    public static final String STYLUS_WITH_GESTURES = "STYLUS_WITH_GESTURES";
    public static final String STYLUS_AND_TOUCH = "STYLUS_AND_TOUCH";

	private TouchHandlerABC touchHandler;

	private Bitmap bitmap;
	protected Canvas canvas;
	private Toast toast;
	private final Rect mRect = new Rect();
	private final RectF mRectF = new RectF();
	private boolean automaticRedraw = true;
	private final RectF clip = new RectF();  
	private final Paint pen;
	private int penID = -1;
	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldPressure, newPressure; 
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private float oldX1, oldY1, newX1, newY1;  // for 1st finger
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;
	
	private boolean palmShield = false;
	private RectF palmShieldRect;
	private Paint palmShieldPaint;
	
	private Toolbox toolbox;
	public Toolbox getToolBox() {
		return toolbox;
	}
	
	private ToolHistory toolHistory = ToolHistory.getToolHistory();
	
	private Overlay overlay = null;
	public void setOverlay(Overlay overlay) {
		this.overlay = overlay;
		invalidate();
	}
	
	private GraphicsModifiedListener graphicsListener = null;
  
	
	public interface OnStrokeFinishedListener {
		void onStrokeFinishedListener();
	}
	
	private OnStrokeFinishedListener strokeFinishedListener = null;
	
	public void setOnStrokeFinishedListener(OnStrokeFinishedListener listener) {
		strokeFinishedListener = listener;
	}
	
	protected void callOnStrokeFinishedListener() {
		if (strokeFinishedListener != null)
			strokeFinishedListener.onStrokeFinishedListener();
	}
	
    private int N = 0;
	private static final int Nmax = 1024;
	private float[] position_x = new float[Nmax];
	private float[] position_y = new float[Nmax];
	private float[] pressure = new float[Nmax];

	// actual data
	private Page page;
	
	// preferences
	private int pen_thickness = 2;
	private Tool tool_type = Tool.FOUNTAINPEN;
	private int pen_color = Color.BLACK;
	protected boolean onlyPenInput = true;
	protected boolean moveGestureWhileWriting = true;
	protected int moveGestureMinDistance = 400; // pixels
	protected boolean doubleTapWhileWriting = true;
	
	public void setOnGraphicsModifiedListener(GraphicsModifiedListener newListener) {
		graphicsListener = newListener;
	}
	
	public void add(Graphics graphics) {
		if (graphics instanceof Stroke) {
			Stroke s = (Stroke)graphics;
			page.addStroke(s);
			if (automaticRedraw) {
				page.draw(canvas, s.getBoundingBox());
				s.getBoundingBox().round(mRect);
				invalidate(mRect);
			}
		} else
			Assert.fail("Unknown graphics object");
	}
	
	public void remove(Graphics graphics) {
		if (graphics instanceof Stroke) { 
			Stroke s = (Stroke)graphics;
			page.removeStroke(s);
			if (automaticRedraw) {
				page.draw(canvas, s.getBoundingBox());
				s.getBoundingBox().round(mRect);
				invalidate(mRect);
			}
		} else
			Assert.fail("Unknown graphics object");
	}
	
	public void interrupt() {
		if (page==null || canvas==null)
			return;
		Log.d(TAG, "Interrupting current interaction");
		N = 0;
		penID = fingerId1 = fingerId2 = -1;
		page.draw(canvas);
		invalidate();
	}
	
	public void setToolType(Tool tool) {
		if (touchHandler != null) {
			touchHandler.destroy();
			touchHandler = null;
		}
		switch (tool) {
		case FOUNTAINPEN:
		case PENCIL:
			toolHistory.setTool(tool);
			break;
		case ARROW:
			break;
		case LINE:
			break;
		case MOVE:
			touchHandler = new TouchHandlerMoveZoom(this);
			break;
		case ERASER:
			touchHandler = new TouchHandlerEraser(this);
			break;
		case TEXT:
			touchHandler = new TouchHandlerText(this);
			break;
		default:
			touchHandler = null;
		}
		toolbox.setActiveTool(tool);
		tool_type = tool;
	}

	public Tool getToolType() {
		return tool_type;
	}

	public int getPenThickness() {
		return pen_thickness;
	}

	public void setPenThickness(int thickness) {
		pen_thickness = thickness;
		toolHistory.setThickness(thickness);
		toolbox.setThickness(thickness);
	}
	
	public int getPenColor() {
		return pen_color;
	}
	
	public void setPenColor(int c) {
		pen_color = c;
		pen.setARGB(Color.alpha(c), Color.red(c), Color.green(c), Color.blue(c));
		toolHistory.setColor(c);
	}
	
	public Page getPage() {
		return page;
	}
	
	public Paper.Type getPagePaperType() {
		return page.paper_type;
	}
	
	public void setPagePaperType(Paper.Type paper_type) {
		page.setPaperType(paper_type);
		page.draw(canvas);
		invalidate();
	}

	public float getPageAspectRatio() {
		return page.aspect_ratio;
	}
	
	public void setPageAspectRatio(float aspect_ratio) {
		page.setAspectRatio(aspect_ratio);
		setPageAndZoomOut(page);
		invalidate();
	}

	public boolean getOnlyPenInput() {
		return onlyPenInput;
	}

	public void setOnlyPenInput(boolean onlyPenInput) {
		this.onlyPenInput = onlyPenInput;
	}

	public boolean getDoubleTapWhileWriting() {
		return doubleTapWhileWriting;
	}

	public void setDoubleTapWhileWriting(boolean doubleTapWhileWriting) {
		this.doubleTapWhileWriting = doubleTapWhileWriting;
	}

	public boolean getMoveGestureWhileWriting() {
		return moveGestureWhileWriting;
	}

	public void setMoveGestureWhileWriting(boolean moveGestureWhileWriting) {
		this.moveGestureWhileWriting = moveGestureWhileWriting;
	}

	public int getMoveGestureMinDistance() {
		return moveGestureMinDistance;
	}

	public void setMoveGestureMinDistance(int moveGestureMinDistance) {
		this.moveGestureMinDistance = moveGestureMinDistance;
	}

	public void setPalmShieldEnabled(boolean enabled) {
		palmShield = enabled;
		initPalmShield();
		invalidate();
	}
	
	private void initPalmShield() {
		if (!palmShield) return;
		if (toolboxIsOnLeft)  // for right-handed user
			palmShieldRect = new RectF(0, getHeight()/2, getWidth(), getHeight());
		else  // for left-handed user
			palmShieldRect = new RectF(0, 0, getWidth(), getHeight()/2);
		palmShieldPaint = new Paint();
		palmShieldPaint.setARGB(0x22, 0, 0, 0);		
	}
	
	/**
	 * Whether the point (x,y) is on the palm shield and hence should be ignored.
	 * @param event
	 * @return whether the touch point is to be ignored.
	 */
	protected boolean isOnPalmShield(MotionEvent event) {
		if (!palmShield)
			return false;
		int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			return palmShieldRect.contains(event.getX(), event.getY());
		case MotionEvent.ACTION_POINTER_DOWN:
			int idx = event.getActionIndex();
			return palmShieldRect.contains(event.getX(idx), event.getY(idx));
		}
		return false;
	}
	
	public HandwriterView(Context context) {
		super(context);
		setFocusable(true);
		pen = new Paint();
		pen.setAntiAlias(true);
		pen.setARGB(0xff, 0, 0, 0);	
		pen.setStrokeCap(Paint.Cap.ROUND);
		setAlwaysDrawnWithCacheEnabled(false);
		setDrawingCacheEnabled(false);
		setWillNotDraw(false);
		setBackgroundDrawable(null);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    	boolean left = settings.getBoolean(KEY_TOOLBOX_IS_ON_LEFT, true);
    	setToolbox(left);
	}

	/**
	 * To be called from the onResume method of the activity. Update appearance according to preferences etc.
	 */
	public void loadSettings(SharedPreferences settings) {
    	boolean toolbox_left = settings.getBoolean(KEY_TOOLBOX_IS_ON_LEFT, true);
    	setToolbox(toolbox_left);
    	boolean toolbox_is_visible = settings.getBoolean(KEY_TOOLBOX_IS_VISIBLE, false);
        getToolBox().setToolboxVisible(toolbox_is_visible);
    	setMoveGestureMinDistance(settings.getInt("move_gesture_min_distance", 400));
       	
    	int penColor = settings.getInt(KEY_PEN_COLOR, getPenColor());
    	int penThickness = settings.getInt(KEY_PEN_THICKNESS, getPenThickness());
    	int penTypeInt = settings.getInt(KEY_PEN_TYPE, getToolType().ordinal());
    	Stroke.Tool penType = Stroke.Tool.values()[penTypeInt];
    	if (penType == Tool.ERASER)  // don't start with sharp whirling blades 
    		penType = Tool.MOVE;
    	setPenColor(penColor);
    	setPenThickness(penThickness);
    	setToolType(penType);
    	
    	ToolHistory history = ToolHistory.getToolHistory();
    	history.restoreFromSettings(settings);
    	getToolBox().onToolHistoryChanged(false);
    	
    	final boolean hwPen = Hardware.hasPenDigitizer();
        String pen_input_mode;
		if (settings.contains(KEY_ONLY_PEN_INPUT_OBSOLETE)) { 
			// import obsoleted setting
			if (settings.getBoolean(KEY_ONLY_PEN_INPUT_OBSOLETE, false)) 
				pen_input_mode = STYLUS_WITH_GESTURES;
			else 
				pen_input_mode = STYLUS_AND_TOUCH;
		} else if (hwPen)
			pen_input_mode = settings.getString(KEY_LIST_PEN_INPUT_MODE, STYLUS_WITH_GESTURES);
		else
			pen_input_mode = STYLUS_AND_TOUCH;
		Log.d(TAG, "pen input mode "+pen_input_mode);
		if (pen_input_mode.equals(STYLUS_ONLY)) {
			setOnlyPenInput(true);
			setDoubleTapWhileWriting(false);
			setMoveGestureWhileWriting(false);
			setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(STYLUS_WITH_GESTURES)) {
			setOnlyPenInput(true);
			setDoubleTapWhileWriting(settings.getBoolean(
					KEY_DOUBLE_TAP_WHILE_WRITE, hwPen));
    		setMoveGestureWhileWriting(settings.getBoolean(
    				KEY_MOVE_GESTURE_WHILE_WRITING, hwPen));
    		setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(STYLUS_AND_TOUCH)) {
			setOnlyPenInput(false);
			setDoubleTapWhileWriting(false);
			setMoveGestureWhileWriting(false);
			setPalmShieldEnabled(settings.getBoolean(KEY_PALM_SHIELD, false));
		}
		else Assert.fail();
	}
	
	
	/**
	 * To be called from the onPause method of the activity. Save preferences etc.
	 * Note: Settings that can only be changed in preferences need not be saved, they
	 * are saved by the preferences.
	 */
	public void saveSettings(SharedPreferences.Editor editor) {    
    	editor.putBoolean(KEY_TOOLBOX_IS_VISIBLE, getToolBox().isToolboxVisible());
        editor.putInt(KEY_PEN_TYPE, getToolType().ordinal());
        editor.putInt(KEY_PEN_COLOR, getPenColor());
        editor.putInt(KEY_PEN_THICKNESS, getPenThickness());

		ToolHistory history = ToolHistory.getToolHistory();
    	history.saveToSettings(editor);
    	
        editor.remove(KEY_ONLY_PEN_INPUT_OBSOLETE);  // obsoleted
	}
	
	
	private boolean toolboxIsOnLeft;
	
	public boolean isToolboxOnLeft() {
		return toolboxIsOnLeft;
	}
	
	public void setToolbox(boolean left) {
		if (toolbox != null && toolboxIsOnLeft == left) return;
		if (toolbox != null) 
			removeView(toolbox);
		toolbox = new Toolbox(getContext(), left);
		addView(toolbox);
		toolboxIsOnLeft = left;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		toolbox.layout(l, t, r, b);
//		if (editText != null) {
//			editText.layout(100, 70, 400, 200);
//		}
		if (palmShield) 
			initPalmShield();
 	}
	
	public void setOnToolboxListener(Toolbox.OnToolboxListener listener) {
		toolbox.setOnToolboxListener(listener);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		toolbox.measure(widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public void setPageAndZoomOut(Page new_page) {
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
			page.setTransform(0, (H-h)/2, dimension);
		else if (w<W)
			page.setTransform((W-w)/2, 0, dimension);
		else
			page.setTransform(0, 0, dimension);
		Log.v(TAG, "set_page at scale "+page.transformation.scale+" canvas w="+W+" h="+H);
		page.draw(canvas);
		invalidate();
	}
	
	protected void centerAndFillScreen(float xCenter, float yCenter) {
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
	
	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
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

	public float getScaledPenThickness() {
		return Stroke.getScaledPenThickness(page.transformation, getPenThickness());
	}
	
	@Override 
	protected void onDraw(Canvas canvas) {
		if (bitmap == null) return;
		if (touchHandler != null) 
			touchHandler.onDraw(canvas, bitmap);
		if ((getToolType() == Stroke.Tool.FOUNTAINPEN || getToolType() == Stroke.Tool.PENCIL)
					&& fingerId2 != -1) {
			// move preview by translating bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float x = (newX1-oldX1+newX2-oldX2)/2;
			float y = (newY1-oldY1+newY2-oldY2)/2; 
			canvas.drawBitmap(bitmap, x, y, null);
		} 
		if (overlay != null) 
			overlay.draw(canvas);
		if (palmShield) {
			canvas.drawRect(palmShieldRect, palmShieldPaint);
		}
	}

	@Override public boolean onTouchEvent(MotionEvent event) {
//		InputDevice dev = event.getDevice();
//		Log.v(TAG, "Touch: "+dev.getId()+" "+dev.getName()+" "+dev.getKeyboardType()+" "+dev.getSources()+" ");
//		Log.v(TAG, "Touch: "+event.getDevice().getName()
//				+" action="+event.getActionMasked()
//				+" pressure="+event.getPressure()
//				+" fat="+event.getTouchMajor()
//				+" penID="+penID+" ID="+event.getPointerId(0)+" N="+N);
		switch (getToolType()) {
		case FOUNTAINPEN:
		case PENCIL:	
			if (onlyPenInput)
				return touchHandlerPen(event);
			else
				return touchHandlerPenStylusAndTouch(event);
		case MOVE:
		case ERASER:
		case TEXT:
			return touchHandler.onTouchEvent(event);
		}
		return false;
	}
		

	
	
	// whether to use the MotionEvent for writing
	private boolean useForWriting(MotionEvent event) {
		return !onlyPenInput || Hardware.isPenEvent(event);
	}

	// whether to use the MotionEvent for move/zoom
	private boolean useForTouch(MotionEvent event) {
		return !onlyPenInput || (onlyPenInput && !Hardware.isPenEvent(event));
	}

	private boolean touchHandlerPen(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (getMoveGestureWhileWriting() && fingerId1 != -1 && fingerId2 == -1) {
				int idx1 = event.findPointerIndex(fingerId1);
				if (idx1 != -1) {
					oldX1 = newX1 = event.getX(idx1);
					oldY1 = newY1 = event.getY(idx1);
				}
			}
			if (getMoveGestureWhileWriting() && fingerId2 != -1) {
				Assert.assertTrue(fingerId1 != -1);
				int idx1 = event.findPointerIndex(fingerId1);
				int idx2 = event.findPointerIndex(fingerId2);
				if (idx1 == -1 || idx2 == -1) return true;
				newX1 = event.getX(idx1);
				newY1 = event.getY(idx1);
				newX2 = event.getX(idx2);
				newY2 = event.getY(idx2);		
				invalidate();
				return true;
			}
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
			Assert.assertTrue(event.getPointerCount() == 1);
			newT = System.currentTimeMillis();
			if (useForTouch(event) && getDoubleTapWhileWriting() && Math.abs(newT-oldT) < 250) {
				// double-tap
				centerAndFillScreen(event.getX(), event.getY());
				penID = fingerId1 = fingerId2 = -1;
				return true;
			}
			oldT = newT;
			if (useForTouch(event) && getMoveGestureWhileWriting() && event.getPointerCount()==1) {
				fingerId1 = event.getPointerId(0); 
				fingerId2 = -1;
				newX1 = oldX1 = event.getX(); 
				newY1 = oldY1 = event.getY();
			}
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				return true;
			}
			// Log.v(TAG, "ACTION_DOWN");
			if (!useForWriting(event)) 
				return true;   // eat non-pen events
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
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				// Log.v(TAG, "ACTION_UP: Got "+N+" points.");
				saveStroke();
				N = 0;
				callOnStrokeFinishedListener();
			} else if (getMoveGestureWhileWriting() && 
						(id == fingerId1 || id == fingerId2) &&
						fingerId1 != -1 && fingerId2 != -1) {
				float dx = page.transformation.offset_x + (newX1-oldX1+newX2-oldX2)/2;
				float dy = page.transformation.offset_y + (newY1-oldY1+newY2-oldY2)/2; 
				page.setTransform(dx, dy, page.transformation.scale, canvas);
				page.draw(canvas);
				invalidate();				
			}
			penID = fingerId1 = fingerId2 = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			// e.g. you start with finger and use pen
			// if (event.getPointerId(0) != penID) return true;
			Log.v(TAG, "ACTION_CANCEL");
			N = 0;
			penID = fingerId1 = fingerId2 = -1;
			page.draw(canvas);
			invalidate();
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_DOWN) {  // start move gesture
			if (fingerId1 == -1) return true; // ignore after move finished
			if (fingerId2 != -1) return true; // ignore more than 2 fingers
			int idx2 = event.getActionIndex();
			oldX2 = newX2 = event.getX(idx2);
			oldY2 = newY2 = event.getY(idx2);
			float dx = newX2-newX1;
			float dy = newY2-newY1;
			float distance = FloatMath.sqrt(dx*dx+dy*dy);
			if (distance >= getMoveGestureMinDistance()) {
				fingerId2 = event.getPointerId(idx2);
			}
			// Log.v(TAG, "ACTION_POINTER_DOWN "+fingerId2+" + "+fingerId1+" "+oldX1+" "+oldY1+" "+oldX2+" "+oldY2);
		}
		return false;
	}

	/**
	 * Touch handler in the onlyPenInput == false mode
	 * No gestures, but allow input from touch points other than the first one
	 * @param event
	 * @return Whether the event was used
	 */
	private boolean touchHandlerPenStylusAndTouch(MotionEvent event) {
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
			Assert.assertTrue(event.getPointerCount() == 1);
			newT = System.currentTimeMillis();
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				return true;
			}
			if (isOnPalmShield(event))
				return true;
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
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				// Log.v(TAG, "ACTION_UP: Got "+N+" points.");
				saveStroke();
				N = 0;
				callOnStrokeFinishedListener();
			}
			penID = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			N = 0;
			penID = -1;
			page.draw(canvas);
			invalidate();
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			newT = System.currentTimeMillis();
			if (isOnPalmShield(event))
				return true;
			if (page.is_readonly) {
				toastIsReadonly();
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
			pen.setStrokeWidth(getScaledPenThickness());
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_UP) {
			int idx = event.getActionIndex();
			int id = event.getPointerId(idx);
			if (id == penID) {
				Log.v(TAG, "ACTION_POINTER_UP: Got "+N+" points.");
				saveStroke();
				N = 0;
				callOnStrokeFinishedListener();
			}
			penID = -1;
			return true;
		}
		return false;
	}

	protected void toastIsReadonly() {
		String s = "Page is readonly";
	   	if (toast == null)
        	toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
    	else {
    		toast.setText(s);
    	}
	   	toast.show();
	}

	public boolean eraseStrokesIn(RectF r) {
		LinkedList<Stroke> toRemove = new LinkedList<Stroke>();
	    ListIterator<Stroke> siter = page.strokes.listIterator();
	    while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
			if (!RectF.intersects(r, s.getBoundingBox())) continue;
			if (s.intersects(r)) {
				toRemove.add(s);
			}
		}
	    siter = toRemove.listIterator();
	    while (siter.hasNext())
	    	graphicsListener.onGraphicsEraseListener(page, siter.next());
		if (toRemove.isEmpty())
			return false;
		else {
			invalidate();
			return true;
		}
	}
	
	private void saveStroke() {
		if (N==0) return;
		if (N==1) {
			N = 2;
			position_x[1] = position_x[0];
			position_y[1] = position_y[0];
			pressure[1] = pressure[0];
		}
		Stroke s = new Stroke(getToolType(), position_x, position_y, pressure, 0, N);
		toolHistory.commit();
		s.setPen(getPenThickness(), pen_color);
		s.setTransform(page.getTransform());
		s.applyInverseTransform();
		s.simplify();
		if (page != null && graphicsListener != null) {
			graphicsListener.onGraphicsCreateListener(page, s);
		}
		N = 0;
	}
	
	
	private void drawOutline() {
		if (getToolType()==Tool.FOUNTAINPEN) {
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
