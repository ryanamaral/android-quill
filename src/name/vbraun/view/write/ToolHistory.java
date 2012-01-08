package name.vbraun.view.write;

import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.R;

import name.vbraun.view.write.Graphics.Tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;


public class ToolHistory {
	private static final String TAG = "ToolHistory";
	
	private static final ToolHistory instance = new ToolHistory();
	protected static final int MAX_SIZE = 10;
	private final LinkedList<HistoryItem> history = new LinkedList<HistoryItem>(); 
	private HistoryItem current = new HistoryItem(Tool.FOUNTAINPEN, 1, Color.BLACK);
	
	private Drawable iconFountainpen;
	private Drawable iconPencil;

    Paint paintMask = new Paint();
	private Bitmap thicknessUltraFine, thicknessThin, thicknessMedium, 
		thicknessThick, thicknessGiant;
	private float density;
	
	public interface OnToolHistoryChangedListener {
		public void onToolHistoryChanged(boolean onlyCurrent);
	}
	
	private OnToolHistoryChangedListener listener;
	
	public void setOnToolHistoryChangedListener(OnToolHistoryChangedListener listener) {
		this.listener = listener;
	}

	private void callListener(boolean onlyCurrent) {
		if (listener != null)
			listener.onToolHistoryChanged(onlyCurrent);
	}
	
	private ToolHistory() {
        paintMask.setColor(0xff000000);
        paintMask.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
	}
	
	public void setColor(int color) {
		if (current.color == color) return;
		current.color = color;
		current.icon = null;
		callListener(true);
	}
	
	public void setTool(Tool tool) {
		if (current.tool.equals(tool)) return;
		current.tool = tool;
		current.icon = null;
		callListener(true);
	}
	
	public void setThickness(int thickness) {
		if (current.thickness == thickness) return;
		current.thickness = thickness;
		current.icon = null;
		callListener(true);
	}
	
	public void onCreate(Context context) {
		Display display = ((WindowManager) 
        		context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        density = metrics.density;
        
		Resources res = context.getResources();
        setFountainpenIcon(res.getDrawable(R.drawable.ic_menu_quill));
        setPencilIcon(res.getDrawable(R.drawable.ic_menu_pencil));
    	thicknessUltraFine = BitmapFactory.decodeResource(res, R.drawable.thickness_ultrafine);
    	thicknessThin      = BitmapFactory.decodeResource(res, R.drawable.thickness_thin);
    	thicknessMedium    = BitmapFactory.decodeResource(res, R.drawable.thickness_medium);
    	thicknessThick     = BitmapFactory.decodeResource(res, R.drawable.thickness_thick);
        thicknessGiant     = BitmapFactory.decodeResource(res, R.drawable.thickness_giant);
	}
	
	public void setFountainpenIcon(Drawable icon) {
		iconFountainpen = icon;
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        iconFountainpen.setBounds(0, 0, w, h);
	}
	
	public void setPencilIcon(Drawable icon) {
		iconPencil = icon;
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        iconPencil.setBounds(0, 0, w, h);
	}
	
	public static ToolHistory getToolHistory() {
		return instance;
	}
	
	public int size() {
		return history.size();
	}
	
	public int getColor(int historyIndex) {
		return history.get(historyIndex).color;
	}

	public Tool getTool(int historyIndex) {
		return history.get(historyIndex).tool;
	}

	public int getThickness(int historyIndex) {
		return history.get(historyIndex).thickness;
	}
	
	public Drawable getIcon(int historyIndex) {
		return history.get(historyIndex).getBitmapDrawable();
	}
	
	public int getColor() {
		return current.color;
	}
	
	public Tool getTool() {
		return current.tool;
	}
	
	public int getThickness() {
		return current.thickness;
	}
	
	public Drawable getIcon() {
		return current.getBitmapDrawable();
	}
	
	public class HistoryItem {
	
		protected Tool tool;
		protected int thickness;
		protected int color;
		protected Drawable icon;
		
		public HistoryItem(Tool tool, int thickness, int color) {
			this.tool = tool;
			this.thickness = thickness;
			this.color = color;
		}
		
		public boolean equals(HistoryItem other) {
			return tool.equals(other.tool) && 
				thickness == other.thickness && 
				color == other.color;
		}
		
		public Drawable getBitmapDrawable() {
			if (icon == null) {
				Log.d(TAG, "getBitmapDrawable "+tool+" "+iconFountainpen);
		    	int w = (int)( 48f*density+0.5f );
		    	int h = (int)( 48f*density+0.5f );
		    	Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		    	bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		    	Canvas canvas = new Canvas(bitmap);
		    	canvas.drawARGB(0xa0, Color.red(color), Color.green(color), Color.blue(color));
		    	
		    	canvas.save();
		    	canvas.scale(density, density);
		    	if (thickness > 20)
		    		canvas.drawBitmap(thicknessGiant, 0, 0, paintMask);
		    	else if (thickness > 10)
		    		canvas.drawBitmap(thicknessThick, 0, 0, paintMask);
		    	else if (thickness > 3)
		    		canvas.drawBitmap(thicknessMedium, 0, 0, paintMask);
		    	else if (thickness > 1.5)
		    		canvas.drawBitmap(thicknessThin, 0, 0, paintMask);
		    	else 
		    		canvas.drawBitmap(thicknessUltraFine, 0, 0, paintMask);
		    	canvas.restore();
	
		    	canvas.save();
		    	float scale = 0.7f;
		    	canvas.translate(w*(1-scale), 0);
		    	canvas.scale(scale, scale);
		    	if (tool.equals(Tool.FOUNTAINPEN)) {
		    		iconFountainpen.draw(canvas);
		    	} else if (tool.equals(Tool.PENCIL)) {
		    		iconPencil.draw(canvas);
		    	}
		    	canvas.restore();
	
				icon = new BitmapDrawable(bitmap);
			}
			return icon;
		}
	}
	
	public boolean commit() {
		ListIterator<HistoryItem> iter = history.listIterator();
		while (iter.hasNext()) {
			HistoryItem h = iter.next();
			if (h.equals(current)) {
				history.remove(h);
				history.addFirst(h);
				callListener(false);
				return false;
			}
		}
		history.addFirst(current);
		if (history.size() > ToolHistory.MAX_SIZE)
			history.removeLast();
		current = new HistoryItem(current.tool, current.thickness, current.color); 
		callListener(false);
		return true;
	}

	
	
    public void previous() {
    	if (history.isEmpty()) return;
    	HistoryItem item = history.removeFirst();
    	history.addLast(item);
    	callListener(false);
    }
	
    
    public void saveToSettings(SharedPreferences.Editor editor) {
//    	saveItemToSettings(current, "current", editor);
    	editor.putInt("ToolHistory_size", size());
    	for (int i=0; i<size(); i++)
    		saveItemToSettings(history.get(i), "item"+i, editor);
    }

    private void saveItemToSettings(HistoryItem item, String name, SharedPreferences.Editor editor) {
    	String typeName = "HistoryItem_pen_type_"+name;
    	String colorName = "HistoryItem_pen_color_"+name;
    	String thicknessName = "HistoryItem_pen_thickness_"+name;
        editor.putInt(typeName, item.tool.ordinal());
        editor.putInt(colorName, item.color);
        editor.putInt(thicknessName, item.thickness);
    }

    private HistoryItem restoreItemFromSettings(String name, SharedPreferences settings) {
    	String typeName = "HistoryItem_pen_type_"+name;
    	String colorName = "HistoryItem_pen_color_"+name;
    	String thicknessName = "HistoryItem_pen_thickness_"+name;
    	int penColor = settings.getInt(colorName, Color.BLACK);
    	int penThickness = settings.getInt(thicknessName, 1);
    	int penTypeInt = settings.getInt(typeName, Tool.FOUNTAINPEN.ordinal());
    	Stroke.Tool penType = Stroke.Tool.values()[penTypeInt];
    	return new HistoryItem(penType, penThickness, penColor);
    }
    

    public void restoreFromSettings(SharedPreferences settings) {
    	int n = settings.getInt("ToolHistory_size", 0);
//    	current = restoreItemFromSettings("current", settings);
    	history.clear();
    	for (int i=0; i<n; i++) {
    		HistoryItem item = restoreItemFromSettings("item"+i, settings);
    		history.add(item);
    	}	
    }
    
}

