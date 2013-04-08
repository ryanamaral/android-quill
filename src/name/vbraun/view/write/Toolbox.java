package name.vbraun.view.write;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics.Tool;
import junit.framework.Assert;

import com.write.Quill.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;

/**
 * The toolbox is a view with a collapsed and expanded view. 
 * The expanded view is a grid of icons.
 * 
 * @author vbraun
 *
 */
public class Toolbox 
	extends 
		RelativeLayout 
	implements 
		View.OnClickListener,
		AdapterView.OnItemSelectedListener,
		ToolHistory.OnToolHistoryChangedListener {
	private static final String TAG = "Toolbox";
	
	public interface OnToolboxListener {
		public void onToolboxListener(View view);
		public void onToolboxColorListener(int color);
		public void onToolboxLineThicknessListener(int thickness);
	}
	
	private OnToolboxListener listener;
	public void setOnToolboxListener(OnToolboxListener listener) {
		this.listener = listener;
	}
	
	private boolean toolboxIsVisible = true;
	private boolean actionBarReplacementIsVisible = false;
	private boolean debugOptions;
	
	protected ImageButton redButton, redButtonRight, redButtonLeft;
	protected ImageButton undoButton, redoButton;
	protected ImageButton fountainpenButton, pencilButton, lineButton;
	protected ImageButton resizeButton;
	protected ImageButton eraserButton;	
	protected ImageButton textButton, photoButton;
	protected ImageButton history1, history2, history3, history4;
	protected Spinner thicknessSpinner;
	protected ImageButton colorWhite, colorSilver, colorGray, colorBlack;
	protected ImageButton colorRed, colorMaroon, colorYellow, colorOlive;
	protected ImageButton colorLime, colorGreen, colorAqua, colorTeal;
	protected ImageButton colorBlue, colorNavy, colorFuchsia, colorPurple;
	protected ImageButton nextButton, prevButton, nextActionButton, prevActionButton;
	
	protected ImageButton quillButton;
	protected Button tagButton;
	protected ImageButton menuButton;

	protected ImageButton controlpointGearsButton, controlpointTrashButton;
	
	private boolean height_small, height_tiny;
	
	private final Hardware hardware;
	
	protected Toolbox(Context context, boolean left) {
		super(context);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        debugOptions = settings.getBoolean(HandwriterView.KEY_DEBUG_OPTIONS, false);

		hardware = Hardware.getInstance(context);
		
		Display display = ((WindowManager) 
        		context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        float height = display.getHeight() / metrics.density;
        height_tiny = (height < 500);
        height_small = !height_tiny && (height < 750);
        
        if (left) 
        	View.inflate(context, R.layout.toolbox, this);
        else      
        	View.inflate(context, R.layout.toolbox_right, this);
		redButton    = (ImageButton) findViewById(R.id.toolbox_redbutton);
		undoButton   = (ImageButton) findViewById(R.id.toolbox_undo);
		redoButton   = (ImageButton) findViewById(R.id.toolbox_redo);
		fountainpenButton = (ImageButton) findViewById(R.id.toolbox_fountainpen);
		pencilButton = (ImageButton) findViewById(R.id.toolbox_pencil);
		lineButton   = (ImageButton) findViewById(R.id.toolbox_line);
		resizeButton = (ImageButton) findViewById(R.id.toolbox_resize);
		eraserButton = (ImageButton) findViewById(R.id.toolbox_eraser);
		textButton   = (ImageButton) findViewById(R.id.toolbox_text);
		photoButton  = (ImageButton) findViewById(R.id.toolbox_photo);
		nextButton   = (ImageButton) findViewById(R.id.toolbox_next);
		prevButton   = (ImageButton) findViewById(R.id.toolbox_prev);	
		nextActionButton = (ImageButton) findViewById(R.id.toolbox_action_next);
		prevActionButton = (ImageButton) findViewById(R.id.toolbox_action_prev);	
		history1     = (ImageButton) findViewById(R.id.toolbox_history_1);
		history2     = (ImageButton) findViewById(R.id.toolbox_history_2);
		history3     = (ImageButton) findViewById(R.id.toolbox_history_3);
		history4     = (ImageButton) findViewById(R.id.toolbox_history_4);
		colorWhite   = (ImageButton) findViewById(R.id.toolbox_color_white);
		colorSilver  = (ImageButton) findViewById(R.id.toolbox_color_silver);
		colorGray    = (ImageButton) findViewById(R.id.toolbox_color_gray);
		colorBlack   = (ImageButton) findViewById(R.id.toolbox_color_black);
		colorRed     = (ImageButton) findViewById(R.id.toolbox_color_red);
		colorMaroon  = (ImageButton) findViewById(R.id.toolbox_color_maroon);
		colorYellow  = (ImageButton) findViewById(R.id.toolbox_color_yellow);
		colorOlive   = (ImageButton) findViewById(R.id.toolbox_color_olive);
		colorLime    = (ImageButton) findViewById(R.id.toolbox_color_lime);
		colorGreen   = (ImageButton) findViewById(R.id.toolbox_color_green);
		colorAqua    = (ImageButton) findViewById(R.id.toolbox_color_aqua);
		colorTeal    = (ImageButton) findViewById(R.id.toolbox_color_teal);
		colorBlue    = (ImageButton) findViewById(R.id.toolbox_color_blue);
		colorNavy    = (ImageButton) findViewById(R.id.toolbox_color_navy);
		colorFuchsia = (ImageButton) findViewById(R.id.toolbox_color_fuchsia);
		colorPurple  = (ImageButton) findViewById(R.id.toolbox_color_purple);
		
		quillButton  = (ImageButton) findViewById(R.id.toolbox_quill_icon);
		tagButton    = (Button)      findViewById(R.id.toolbox_tag);
		menuButton   = (ImageButton) findViewById(R.id.toolbox_menu);
		
		thicknessSpinner = (Spinner) findViewById(R.id.toolbox_thickness_spinner);
		thicknessSpinner.setOnItemSelectedListener(this);
		
		controlpointGearsButton = (ImageButton) findViewById(R.id.toolbox_controlpoint_gears);
		controlpointTrashButton = (ImageButton) findViewById(R.id.toolbox_controlpoint_trash);
		
		if (!Hardware.hasPressureSensor()) {
			fountainpenButton.setVisibility(View.INVISIBLE);
		}
		
		redButton.setOnClickListener(this);
		undoButton.setOnClickListener(this);
		redoButton.setOnClickListener(this);
		fountainpenButton.setOnClickListener(this);
		pencilButton.setOnClickListener(this);
		lineButton.setOnClickListener(this);
		resizeButton.setOnClickListener(this);
		eraserButton.setOnClickListener(this);
		textButton.setOnClickListener(this);
		photoButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		prevButton.setOnClickListener(this);
		nextActionButton.setOnClickListener(this);
		prevActionButton.setOnClickListener(this);
		history1.setOnClickListener(this);
		history2.setOnClickListener(this);
		history3.setOnClickListener(this);
		history4.setOnClickListener(this);
		colorWhite.setOnClickListener(this);
		colorSilver.setOnClickListener(this);
		colorGray.setOnClickListener(this);
		colorBlack.setOnClickListener(this);
		colorRed.setOnClickListener(this);
		colorMaroon.setOnClickListener(this);
		colorYellow.setOnClickListener(this);
		colorOlive.setOnClickListener(this);
		colorLime.setOnClickListener(this);
		colorGreen.setOnClickListener(this);
		colorAqua.setOnClickListener(this);
		colorTeal.setOnClickListener(this);
		colorBlue.setOnClickListener(this);
		colorNavy.setOnClickListener(this);
		colorFuchsia.setOnClickListener(this);
		colorPurple.setOnClickListener(this);
		quillButton.setOnClickListener(this);
		tagButton.setOnClickListener(this);
		menuButton.setOnClickListener(this);
		
        if (!debugOptions) {
        	textButton.setVisibility(View.GONE);  // TODO
        	photoButton.setVisibility(View.GONE);  // TODO
        }
    	   
		if (height_small) {
			prevButton.setVisibility(View.GONE);
			nextButton.setVisibility(View.GONE);
			prevActionButton.setVisibility(View.VISIBLE);
			nextActionButton.setVisibility(View.VISIBLE);
		}
		
		if (height_tiny) {
			prevButton.setVisibility(View.GONE);
			nextButton.setVisibility(View.GONE);
			prevActionButton.setVisibility(View.VISIBLE);
			nextActionButton.setVisibility(View.VISIBLE);
			colorWhite.setVisibility(View.GONE);
			colorSilver.setVisibility(View.GONE); 
			colorGray.setVisibility(View.GONE); 
			colorBlack.setVisibility(View.GONE);
			colorRed.setVisibility(View.GONE); 
			colorMaroon.setVisibility(View.GONE); 
			colorYellow.setVisibility(View.GONE); 
			colorOlive.setVisibility(View.GONE);
			colorLime.setVisibility(View.GONE); 
			colorGreen.setVisibility(View.GONE); 
			colorAqua.setVisibility(View.GONE); 
			colorTeal.setVisibility(View.GONE);
			colorBlue.setVisibility(View.GONE); 
			colorNavy.setVisibility(View.GONE); 
			colorFuchsia.setVisibility(View.GONE); 
			colorPurple.setVisibility(View.GONE);
		}

		ToolHistory.getToolHistory().setOnToolHistoryChangedListener(this);
        onToolHistoryChanged(false);
	}
	
	public ImageButton getToolIcon(Tool tool) {
		switch (tool) {
		case FOUNTAINPEN:
			return fountainpenButton;
		case PENCIL:
			return pencilButton;
		case MOVE:
			return resizeButton;
		case ERASER:
			return eraserButton;
		case LINE:
			return lineButton;
		case TEXT:
			return textButton;
		case IMAGE:
			return photoButton; 
		default:
			Assert.fail();
			return null;
		}		
	}
	
	public void setIconActive(Tool tool, boolean active) {
		if (tool == null) return;
		getToolIcon(tool).setSelected(active);
	}
	
	public void setPrevIconEnabled(boolean active) {
		prevButton.setEnabled(active);
	}

	public void setNextIconEnabled(boolean active) {
		nextButton.setEnabled(active);
	}
	
	public void setUndoIconEnabled(boolean active) {
		undoButton.setEnabled(active);
	}

	public void setRedoIconEnabled(boolean active) {
		redoButton.setEnabled(active);
	}
	
	private Graphics.Tool previousTool;

 	public void setActiveTool(Tool tool) {
 		setIconActive(previousTool, false);
		setIconActive(tool, true);
		previousTool = tool;
	}
	
	public void setActionBarReplacementVisible(boolean visible) {
		actionBarReplacementIsVisible = visible;
		int vis = visible ? View.VISIBLE : View.INVISIBLE;
		quillButton.setVisibility(vis);
		tagButton.setVisibility(vis);		
		menuButton.setVisibility(vis);
		if (prevActionButton.getVisibility() != View.GONE) {
			prevActionButton.setVisibility(vis);
			nextActionButton.setVisibility(vis);
		}
	}
	
	public void setToolboxVisible(boolean visible) {
		Log.d(TAG, "setToolboxVisible "+visible);
		toolboxIsVisible = visible;
		int vis = visible ? View.VISIBLE : View.INVISIBLE;
		if (Hardware.hasPressureSensor())
			fountainpenButton.setVisibility(vis);
		pencilButton.setVisibility(vis);
		history1.setVisibility(vis);
		history2.setVisibility(vis);
		history3.setVisibility(vis);
		history4.setVisibility(vis);
		thicknessSpinner.setVisibility(vis);
		undoButton.setVisibility(vis);
		redoButton.setVisibility(vis);
		resizeButton.setVisibility(vis);
		lineButton.setVisibility(vis);
		eraserButton.setVisibility(vis);
    	photoButton.setVisibility(vis);
		
		if (debugOptions) {
        	textButton.setVisibility(vis);  // TODO
        }

		if (prevButton.getVisibility() != View.GONE) {
			prevButton.setVisibility(vis);
			nextButton.setVisibility(vis);			
		}
		
		if (colorWhite.getVisibility() != View.GONE) {
			colorWhite.setVisibility(vis);
			colorSilver.setVisibility(vis); 
			colorGray.setVisibility(vis); 
			colorBlack.setVisibility(vis);
			colorRed.setVisibility(vis); 
			colorMaroon.setVisibility(vis); 
			colorYellow.setVisibility(vis); 
			colorOlive.setVisibility(vis);
			colorLime.setVisibility(vis); 
			colorGreen.setVisibility(vis); 
			colorAqua.setVisibility(vis); 
			colorTeal.setVisibility(vis);
			colorBlue.setVisibility(vis); 
			colorNavy.setVisibility(vis); 
			colorFuchsia.setVisibility(vis); 
			colorPurple.setVisibility(vis);
		}
	}
	
	public boolean isToolboxVisible() {
		return toolboxIsVisible;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Log.d(TAG, "onDraw "+redButton.getLeft()+" "+getTop());
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		// Log.d(TAG, "dispatchDraw");
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Log.d(TAG, "onLayout "+l+" "+t+" "+r+" "+b);
		super.onLayout(changed, l, t, r, b);
	}	

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		Log.d(TAG, "onMeasure "+View.MeasureSpec.getSize(widthMeasureSpec)+
//				" "+View.MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.toolbox_redbutton:
			toolboxIsVisible = !toolboxIsVisible;
			setToolboxVisible(toolboxIsVisible);
			if (listener != null) listener.onToolboxListener(v);
			break;
		case R.id.toolbox_color_white:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0xff, 0xff, 0xff)); 
			break;
		case R.id.toolbox_color_silver:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0xc0, 0xc0, 0xc0)); 
			break;
		case R.id.toolbox_color_gray:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x80, 0x80, 0x80)); 
			break;
		case R.id.toolbox_color_black:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0x00, 0x00)); 
			break;
		case R.id.toolbox_color_red:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0xff, 0x00, 0x00)); 
			break;
		case R.id.toolbox_color_maroon:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x80, 0x00, 0x00)); 
			break;
		case R.id.toolbox_color_yellow:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0xff, 0xff, 0x00)); 
			break;
		case R.id.toolbox_color_olive:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x80, 0x80, 0x00)); 
			break;
		case R.id.toolbox_color_lime:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0xff, 0x00)); 
			break;
		case R.id.toolbox_color_green:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0x80, 0x00)); 
			break;
		case R.id.toolbox_color_aqua:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0xff, 0xff)); 
			break;
		case R.id.toolbox_color_teal:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0x80, 0x80)); 
			break;
		case R.id.toolbox_color_blue:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0x00, 0xff)); 
			break;
		case R.id.toolbox_color_navy:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0x00, 0x80)); 
			break;
		case R.id.toolbox_color_fuchsia:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0xff, 0x00, 0xff)); 
			break;
		case R.id.toolbox_color_purple:
			if (listener != null) listener.onToolboxColorListener(Color.argb(0xff, 0x80, 0x00, 0x80)); 
			break;
		default:
			if (listener != null) listener.onToolboxListener(v);
			break;
		}
	}

	private int[] thicknessChoices = {1, 2, 5, 12, 40};

	
	@Override
	public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
		int thickness = thicknessChoices[position];
		if (listener != null) listener.onToolboxLineThicknessListener(thickness);
	}
	
	public void setThickness(int thickness) {
		for (int i=0; i<thicknessChoices.length; i++)
			if (thickness == thicknessChoices[i])
				thicknessSpinner.setSelection(i);
	}
	 
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
	
	@Override
	public void onToolHistoryChanged(boolean onlyCurrent) {
		ToolHistory h = ToolHistory.getToolHistory();
		// Log.d(TAG, "onToolHistoryChanged "+h.size());
		history1.setImageDrawable(h.getIcon());
		if (onlyCurrent) return;
		if (h.size() > 0) history2.setImageDrawable(h.getIcon(0));
		if (h.size() > 1) history3.setImageDrawable(h.getIcon(1));
		if (h.size() > 2) history4.setImageDrawable(h.getIcon(2));
	}

	
	private boolean inControlpointMoveMode = false;
	private boolean toolboxVisibleBeforeMove;
	private Rect rectGears = new Rect();
	private Rect rectTrash = new Rect();
	
	public void startControlpointMove(boolean showGearsButton, boolean showTrashButton) {
		inControlpointMoveMode = true;
		toolboxVisibleBeforeMove = toolboxIsVisible;
		setToolboxVisible(false);
		redButton.setVisibility(INVISIBLE);
		if (showGearsButton)
			controlpointGearsButton.setVisibility(VISIBLE);
		if (showTrashButton)
			controlpointTrashButton.setVisibility(VISIBLE);
		controlpointGearsButton.setPressed(false);
		controlpointTrashButton.setPressed(false);
	}
	
	/**
	 * Stop the {@link Controlpoint} move mode.
	 * 
	 * It is safe to call this method even if you are not in the move mode.
	 */
	public void stopControlpointMove() {
		redButton.setVisibility(VISIBLE);
		if (inControlpointMoveMode)
			setToolboxVisible(toolboxVisibleBeforeMove);
		controlpointGearsButton.setVisibility(GONE);
		controlpointTrashButton.setVisibility(GONE);
		inControlpointMoveMode = false;
	}

	/**
	 * While moving the control point, the event must be passed to this method.
	 * @param event
	 * @return true if the point hovers over the "gears" button that is visible only while moving the control point.
	 */
	public boolean onControlpointMotion(MotionEvent event) {
		Assert.assertTrue(inControlpointMoveMode);
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			boolean pressGears = false;
			boolean pressTrash = false;
			float x=0, y=0;
			controlpointGearsButton.getHitRect(rectGears);
			controlpointTrashButton.getHitRect(rectTrash);
			for (int idx = 0; idx < event.getPointerCount(); idx++) {
				x = event.getX(idx);
				y = event.getY(idx);
				pressGears = pressGears || rectGears.contains((int)x, (int)y);
				pressTrash = pressTrash || rectTrash.contains((int)x, (int)y);
			}			
			controlpointGearsButton.setPressed(pressGears);
			controlpointTrashButton.setPressed(pressTrash);
			return pressGears;
		}
		return false;
	}
	
	public boolean isGearsSelectedControlpointMove() {
		return controlpointGearsButton.getVisibility() == VISIBLE && 
				controlpointGearsButton.isPressed();
	}
	
	public boolean isTrashSelectedControlpointMove() {
		return controlpointTrashButton.getVisibility() == VISIBLE && 
				controlpointTrashButton.isPressed();
	}
	
}
