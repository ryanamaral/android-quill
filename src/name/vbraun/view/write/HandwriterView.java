package name.vbraun.view.write;

import java.io.File;
import java.util.LinkedList;
import java.util.UUID;

import com.write.Quill.R;

import name.vbraun.lib.pen.HardwareButtonListener;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.LinearFilter.Filter;

import junit.framework.Assert;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.WindowManager;


public class HandwriterView 
	extends ViewGroup 
	implements HardwareButtonListener {
	
	private static final String TAG = "Handwrite";
	
	public static final String KEY_LIST_PEN_INPUT_MODE = "pen_input_mode";
	public static final String KEY_DOUBLE_TAP_WHILE_WRITE = "double_tap_while_write";
	public static final String KEY_MOVE_GESTURE_WHILE_WRITING = "move_gesture_while_writing";
	public static final String KEY_MOVE_GESTURE_FIX_ZOOM = "move_gesture_fix_zoom";
	public static final String KEY_PALM_SHIELD = "palm_shield";
	public static final String KEY_TOOLBOX_IS_ON_LEFT = "toolbox_left";
	private static final String KEY_TOOLBOX_IS_VISIBLE = "toolbox_is_visible";
	private static final String KEY_PEN_TYPE = "pen_type";
	private static final String KEY_PEN_COLOR = "pen_color";
	private static final String KEY_PEN_THICKNESS = "pen_thickness";
	public static final String KEY_DEBUG_OPTIONS = "debug_options_enable";
	public static final String KEY_PEN_SMOOTH_FILTER = "pen_smooth_filter";
	
	// values for the preferences key KEY_LIST_PEN_INPUT_MODE
    public static final String STYLUS_ONLY = "STYLUS_ONLY";
    public static final String STYLUS_WITH_GESTURES = "STYLUS_WITH_GESTURES";
    public static final String STYLUS_AND_TOUCH = "STYLUS_AND_TOUCH";
    
    // values for the preferences key KEY_PEN_SMOOTH_FILTER
    public static final String SMOOTH_FILTER_NONE = "SMOOTH_FILTER_NONE";
    public static final String SMOOTH_FILTER_GAUSSIAN = "SMOOTH_FILTER_GAUSSIAN";
    public static final String SMOOTH_FILTER_GAUSSIAN_HQ = "SMOOTH_FILTER_GAUSSIAN_HQ";
    public static final String SMOOTH_FILTER_SAVITZKY_GOLAY = "SMOOTH_FILTER_SAVITZKY_GOLAY";
    public static final String SMOOTH_FILTER_SAVITZKY_GOLAY_HQ = "SMOOTH_FILTER_SAVITZKY_GOLAY_HQ";
    

    protected final float screenDensity;
	private TouchHandlerABC touchHandler;

	private Bitmap bitmap;
	protected Canvas canvas;
	private Toast toast;
	
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
  
	
	private InputListener inputListener = null;
	
	public void setOnInputListener(InputListener listener) {
		inputListener = listener;
	}
	
	protected void callOnStrokeFinishedListener() {
		if (inputListener != null)
			inputListener.onStrokeFinishedListener();
	}
	
	protected void callOnEditImageListener(GraphicsImage image) {
		File file = image.getFile();
		if (inputListener == null)
			return;
		if (file == null)
			inputListener.onPickImageListener(image);
		else
			inputListener.onEditImageListener(image);
	}
	
	// actual data
	private Page page;
	
	// preferences
	private int pen_thickness = -1;
	private int pen_color = -1;
	private Tool tool_type = null;
	private Filter penSmoothFilter = Filter.KERNEL_SAVITZKY_GOLAY_11;
	protected boolean onlyPenInput = true;
	protected boolean moveGestureWhileWriting = true;
	protected boolean moveGestureFixZoom = true;
	protected int moveGestureMinDistance = 400; // pixels
	protected boolean doubleTapWhileWriting = true;
	
	private boolean acceptInput = false;
	
	/**
	 * Stop input event processing (to be called from onPause/onResume if you want to make sure 
	 * that no events are processed
	 */
	public void stopInput() {
		acceptInput = false;
	}
	
	/**
	 * Start input event processing. Needs to be called from onResume() when the 
	 * activity is ready to receive input.
	 */
	public void startInput() {
		acceptInput = true;
	}
	
	public void setOnGraphicsModifiedListener(GraphicsModifiedListener newListener) {
		graphicsListener = newListener;
	}
	
	public void add(Graphics graphics) {
		if (graphics instanceof Stroke) { // most likely first
			Stroke s = (Stroke)graphics;
			page.addStroke(s);
		} else if (graphics instanceof GraphicsLine ) {
			GraphicsLine l = (GraphicsLine)graphics;
			page.addLine(l);
		} else if (graphics instanceof GraphicsImage ) {
			GraphicsImage img = (GraphicsImage)graphics;
			page.addImage(img);
		} else
			Assert.fail("Unknown graphics object");
		page.draw(canvas, graphics.getBoundingBox());
		invalidate(graphics.getBoundingBoxRoundOut());
	}
	
	public void remove(Graphics graphics) {
		if (graphics instanceof Stroke) { 
			Stroke s = (Stroke)graphics;
			page.removeStroke(s);
		} else if (graphics instanceof GraphicsLine ) {
			GraphicsLine l = (GraphicsLine)graphics;
			page.removeLine(l);
		} else if (graphics instanceof GraphicsImage ) {
			GraphicsImage img = (GraphicsImage)graphics;
			page.removeImage(img);
		} else
			Assert.fail("Unknown graphics object");
		page.draw(canvas, graphics.getBoundingBox());
		invalidate(graphics.getBoundingBoxRoundOut());
	}
	
    public void add(LinkedList<Stroke> penStrokes) {
    	getPage().strokes.addAll(penStrokes);
		page.draw(canvas);
    	invalidate();
    }
    
    public void remove(LinkedList<Stroke> penStrokes) {
    	getPage().strokes.removeAll(penStrokes);
		page.draw(canvas);
    	invalidate();
   }
    
    /**
     * Set the image
     * @param uuid The UUID
     * @param name The image file name (path+uuid+extension)
     */
    public void setImage(UUID uuid, String name, boolean constrainAspect) {
    	for (GraphicsImage image : getPage().images)
    		if (image.getUuid().equals(uuid)) {
    			if (name==null)
    				getPage().images.remove(image);
    			else { 
    				if (image.checkFileName(name)) {
        				image.setFile(name, constrainAspect);
    				} else {
    					Log.e(TAG, "incorrect image file name");
        				getPage().images.remove(image);
    				}
    			}
    			page.draw(canvas);
    			invalidate();
    			return;
    		}
    	Log.e(TAG, "setImage(): Image does not exist");
    }
	
    public GraphicsImage getImage(UUID uuid) {
    	for (GraphicsImage image : getPage().images)
    		if (image.getUuid().equals(uuid))
    			return image;
    	Log.e(TAG, "getImage(): Image does not exists");
    	return null;
    }
    
	public void interrupt() {
		if (page==null || canvas==null)
			return;
		Log.d(TAG, "Interrupting current interaction");
		if (touchHandler != null) 
			touchHandler.interrupt();
		page.draw(canvas);
		invalidate();
	}
	
	public void setToolType(Tool tool) {
		if (tool.equals(tool_type)) return;
		if (touchHandler != null) {
			touchHandler.destroy();
			touchHandler = null;
		}
		switch (tool) {
		case FOUNTAINPEN:
		case PENCIL:
			if (onlyPenInput)
				touchHandler = new TouchHandlerActivePen(this);
			else
				touchHandler = new TouchHandlerPassivePen(this);
			toolHistory.setTool(tool);
			break;
		case ARROW:
			break;
		case LINE:
			touchHandler = new TouchHandlerLine(this);
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
		case IMAGE:
			touchHandler = new TouchHandlerImage(this);
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

	public Filter getPenSmoothFilter() {
		return penSmoothFilter;
	}
	
	public void setPenSmootFilter(Filter filter) {
		this.penSmoothFilter = filter;
		// Log.e(TAG, "Pen smoothen filter = "+filter);
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

	public boolean getMoveGestureFixZoom() {
		return moveGestureFixZoom;
	}
	
	public void setMoveGestureFixZoom(boolean moveGestureFixZoom) {
		this.moveGestureFixZoom = moveGestureFixZoom;
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
		setAlwaysDrawnWithCacheEnabled(false);
		setDrawingCacheEnabled(false);
		setWillNotDraw(false);
		setBackgroundDrawable(null);
		
		Display display = ((WindowManager) 
        		context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenDensity = metrics.density;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    	boolean left = settings.getBoolean(KEY_TOOLBOX_IS_ON_LEFT, true);
    	setToolbox(left);
    	
    	Hardware hw = Hardware.getInstance(context);
    	hw.addViewHack(this);
    	hw.setOnHardwareButtonListener(this);
    	// setLayerType(LAYER_TYPE_SOFTWARE, null);
	}	

	/**
	 * To be called from the onResume method of the activity. Update appearance according to preferences etc.
	 */
	public void loadSettings(SharedPreferences settings) {
    	boolean toolbox_left = settings.getBoolean(KEY_TOOLBOX_IS_ON_LEFT, true);
    	setToolbox(toolbox_left);

    	int toolTypeInt = settings.getInt(KEY_PEN_TYPE, Tool.FOUNTAINPEN.ordinal());
    	Stroke.Tool toolType = Stroke.Tool.values()[toolTypeInt];
    	if (toolType == Tool.ERASER)  // don't start with sharp whirling blades 
    		toolType = Tool.MOVE;
    	setToolType(toolType);

    	boolean toolbox_is_visible = settings.getBoolean(KEY_TOOLBOX_IS_VISIBLE, false);
        getToolBox().setToolboxVisible(toolbox_is_visible);
    	setMoveGestureMinDistance(settings.getInt("move_gesture_min_distance", 400));
       	
    	int penColor = settings.getInt(KEY_PEN_COLOR, Color.BLACK);
    	int penThickness = settings.getInt(KEY_PEN_THICKNESS, 2);
    	setPenColor(penColor);
    	setPenThickness(penThickness);
    	
    	ToolHistory history = ToolHistory.getToolHistory();
    	history.restoreFromSettings(settings);
    	getToolBox().onToolHistoryChanged(false);
    	
    	final boolean hwPen = Hardware.hasPenDigitizer();
        String pen_input_mode;
		if (hwPen)
			pen_input_mode = settings.getString(KEY_LIST_PEN_INPUT_MODE, STYLUS_WITH_GESTURES);
		else
			pen_input_mode = STYLUS_AND_TOUCH;
		Log.d(TAG, "pen input mode "+pen_input_mode);
		if (pen_input_mode.equals(STYLUS_ONLY)) {
			setOnlyPenInput(true);
			setDoubleTapWhileWriting(false);
			setMoveGestureWhileWriting(false);
			setMoveGestureFixZoom(false);
			setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(STYLUS_WITH_GESTURES)) {
			setOnlyPenInput(true);
			setDoubleTapWhileWriting(settings.getBoolean(
					KEY_DOUBLE_TAP_WHILE_WRITE, hwPen));
    		setMoveGestureWhileWriting(settings.getBoolean(
    				KEY_MOVE_GESTURE_WHILE_WRITING, hwPen));
			setMoveGestureFixZoom(settings.getBoolean(KEY_MOVE_GESTURE_FIX_ZOOM, false));
    		setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(STYLUS_AND_TOUCH)) {
			setOnlyPenInput(false);
			setDoubleTapWhileWriting(false);
			setMoveGestureWhileWriting(false);
			setMoveGestureFixZoom(false);
			setPalmShieldEnabled(settings.getBoolean(KEY_PALM_SHIELD, false));
		}
		else Assert.fail();
	
//		final String pen_smooth_filter = settings.getString
//				(KEY_PEN_SMOOTH_FILTER, Filter.KERNEL_SAVITZKY_GOLAY_11.toString());
		final String pen_smooth_filter = settings.getString(KEY_PEN_SMOOTH_FILTER, 
				getContext().getString(R.string.preferences_pen_smooth_default));
		setPenSmootFilter(Filter.valueOf(pen_smooth_filter));

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
		for (int i=0; i<getChildCount(); i++)
			getChildAt(i).layout(l, t, r, b);
//		toolbox.layout(l, t, r, b);
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
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			zoomFitWidth();
		else
			zoomOutOverview();
		page.draw(canvas);
		invalidate();
	}
	
	private void zoomOutOverview() {
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
	}
	
	private void zoomFitWidth() {
		float H = canvas.getHeight();
		float W = canvas.getWidth();
		float dimension = W/page.aspect_ratio;
		float w = dimension*page.aspect_ratio;
		float offset_y;
		RectF r = page.getLastStrokeRect();
		if (r == null)
			offset_y = 0;
		else {
			float y_center = r.centerY() * dimension;
			float screen_h = w/W*H;
			offset_y = screen_h/2 - y_center;  // put y_center at screen center
			if (offset_y > 0) offset_y = 0;
			if (offset_y - screen_h < -dimension) offset_y = -dimension + screen_h;
		}
		page.setTransform(0, offset_y, dimension);
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
		graphicsListener.onPageClearListener(page);
		page.draw(canvas);
		invalidate();
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

	@Override 
	protected void onDraw(Canvas canvas) {
		if (bitmap == null) return;
		if (touchHandler != null) 
			touchHandler.draw(canvas, bitmap);
		if (overlay != null) 
			overlay.draw(canvas);
		if (palmShield) {
			canvas.drawRect(palmShieldRect, palmShieldPaint);
		}
	}

	@Override 
	public boolean onTouchEvent(MotionEvent event) {
		if (!acceptInput) return false;
		if (touchHandler == null) return false;
		
		// Log.e(TAG, "onTouch "+ Hardware.isPenButtonPressed(event));
		// switch to eraser if button is pressed
		if (getToolType() != Tool.ERASER && Hardware.isPenButtonPressed(event)) {
			interrupt();
			setToolType(Tool.ERASER);
		}
		
		// return touchHandler.onTouchEvent(event);
		touchHandler.onTouchEvent(event);
		return true;
	}
	
	@Override
	public void onHardwareButtonListener(Type button) {
		interrupt();
		setToolType(Tool.ERASER);
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
	    for (Stroke s: page.strokes) {	
			if (!RectF.intersects(r, s.getBoundingBox())) continue;
			if (s.intersects(r)) {
				toRemove.add(s);
			}
		}
	    for (Stroke s : toRemove)
	    	graphicsListener.onGraphicsEraseListener(page, s);
		if (toRemove.isEmpty())
			return false;
		else {
			invalidate();
			return true;
		}
	}
	
	public boolean eraseLineArtIn(RectF r) {
		LinkedList<GraphicsControlpoint> toRemove = new LinkedList<GraphicsControlpoint>();
	    for (GraphicsControlpoint graphics: page.lineArt) {	
			if (!RectF.intersects(r, graphics.getBoundingBox())) continue;
			if (graphics.intersects(r)) {
				toRemove.add(graphics);
			}
		}
	    for (GraphicsControlpoint graphics : toRemove)
	    	graphicsListener.onGraphicsEraseListener(page, graphics);
		if (toRemove.isEmpty())
			return false;
		else {
			invalidate();
			return true;
		}
	}
	

	protected void saveStroke(Stroke s) {
		if (page.is_readonly) {
			toastIsReadonly();
			return;
		}
		toolHistory.commit();
		if (page != null && graphicsListener != null) {
			graphicsListener.onGraphicsCreateListener(page, s);
		}
	}
	
	protected void saveGraphics(GraphicsControlpoint graphics) {
		if (page.is_readonly) {
			toastIsReadonly();
			return;
		}
		if (page != null && graphicsListener != null) {
			graphicsListener.onGraphicsCreateListener(page, graphics);
		}
	}
	
	protected void removeGraphics(GraphicsControlpoint graphics) {
		if (page.is_readonly) {
			toastIsReadonly();
			return;
		}
		if (page != null && graphicsListener != null) {
			graphicsListener.onGraphicsEraseListener(page, graphics);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (Hardware.onKeyDown(keyCode, event))	
			return true;
		else
			return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (Hardware.onKeyUp(keyCode, event))	
			return true;
		else
			return super.onKeyUp(keyCode, event);
	}

	
	
	@Override
	public boolean onDragEvent(DragEvent event) {
		Log.e(TAG, "onDragEv");
		return super.onDragEvent(event);
	}
	
	
}
