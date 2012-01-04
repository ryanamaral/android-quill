package name.vbraun.view.write;

import android.graphics.Canvas;
import android.graphics.RectF;

public abstract class Graphics {
	private static final String TAG = "Graphics";

	public enum Tool {
		FOUNTAINPEN, PENCIL, MOVE, ERASER,
		TEXT, LINE, ARROW
	}
	
	protected Tool tool;
	
	protected Graphics(Tool mTool) {
		tool = mTool;
	}
	
	public Tool getTool() {
		return tool;
	}
	
	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;
	
	protected RectF bBox = new RectF();
	protected boolean recompute_bounding_box = true;
	
	public RectF getBoundingBox() {
		if (recompute_bounding_box) computeBoundingBox();
		return bBox;
	}
	
	abstract protected void computeBoundingBox();

	protected void setTransform(float dx, float dy, float s) {
		offset_x = dx;
		offset_y = dy;
		scale = s;
		recompute_bounding_box = true;
	}
	
	protected void setTransform(Transformation transform) {
		offset_x = transform.offset_x;
		offset_y = transform.offset_y;
		scale = transform.scale;	
		recompute_bounding_box = true;
	}
	
	abstract public float distance(float x_screen, float y_screen);
	abstract public boolean intersects(RectF r_screen);
	abstract public void draw(Canvas c, RectF bounding_box);
}
