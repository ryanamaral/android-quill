package name.vbraun.view.write;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Assert;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class GraphicsImage extends GraphicsControlpoint {
	private static final String TAG = "GraphicsImage";
	
	private Controlpoint bottom_left, bottom_right, top_left, top_right, center;
	private final Paint paint = new Paint();
	private final Rect  rect  = new Rect();
	private final RectF rectF = new RectF();
	
	/**
	 * @param transform The current transformation
	 * @param x Screen x coordinate 
	 * @param y Screen y coordinate 
	 * @param penThickness
	 * @param penColor
	 */
	protected GraphicsImage(Transformation transform, float x, float y) {
		super(Tool.IMAGE);
		setTransform(transform);
		bottom_left  = new Controlpoint(transform, x, y);
		bottom_right = new Controlpoint(transform, x, y);
		top_left     = new Controlpoint(transform, x, y);
		top_right    = new Controlpoint(transform, x, y);
		center       = new Controlpoint(transform, x, y);
		controlpoints.add(bottom_left);
		controlpoints.add(bottom_right);
		controlpoints.add(top_left);
		controlpoints.add(top_right);
		controlpoints.add(center);
		init();
	}

	private void init() {
		paint.setARGB(0x60, 0x0, 0xff, 0x0);
		paint.setStyle(Style.FILL);
	}
	
	@Override
	protected Controlpoint initialControlpoint() {
		return bottom_right;
	}
	
	@Override
	public boolean intersects(RectF screenRect) {
		return false;
	}
	
	@Override
	public void draw(Canvas c, RectF bounding_box) {
		computeScreenRect();
		c.drawRect(rect, paint);
		c.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
	}

	private Controlpoint oppositeControlpoint(Controlpoint point) {
		if (point == bottom_right) return top_left;
		if (point == bottom_left)  return top_right;
		if (point == top_right)    return bottom_left;
		if (point == top_left)     return bottom_right;
		if (point == center)       return center;
		Assert.fail("Unreachable"); return null;
	}
	
	private final static float minDistancePixel = 30;
	
	@Override
	void controlpointMoved(Controlpoint point) {
		super.controlpointMoved(point);
		if (point == center) {
			float width2  = (bottom_right.x - bottom_left.x)/2;
			float height2 = (top_right.y - bottom_right.y)/2;
			bottom_right.y = bottom_left.y = center.y - height2;
			top_right.y    = top_left.y    = center.y + height2;
			bottom_right.x = top_right.x   = center.x + width2;
			bottom_left.x  = top_left.x    = center.x - width2;		
		} else {
			Controlpoint opposite = oppositeControlpoint(point);
			float dx = opposite.x - point.x;
			float dy = opposite.y - point.y;
			float minDistance = minDistancePixel / scale;
			if (0<= dx && dx <= minDistance) dx = minDistance;
			if (-minDistance <= dx && dx <= 0) dx = -minDistance;
			if (0<= dy && dy <= minDistance) dy = minDistance;
			if (-minDistance <= dy && dy <= 0) dy = -minDistance;
			rectF.bottom = point.y;
			rectF.top    = point.y + dy;
			rectF.left   = point.x;
			rectF.right  = point.x + dx;
			rectF.sort();
			bottom_right.y = bottom_left.y = rectF.bottom;
			top_right.y    = top_left.y    = rectF.top;
			bottom_right.x = top_right.x   = rectF.right;
			bottom_left.x  = top_left.x    = rectF.left;
			center.x = rectF.left   + (rectF.right - rectF.left)/2;
			center.y = rectF.bottom + (rectF.top   - rectF.bottom)/2;
		}
	}

	
	private void computeScreenRect() {
		rectF.bottom = bottom_left.screenY();
		rectF.top    = top_left.screenY();
		rectF.left   = bottom_left.screenX();
		rectF.right  = bottom_right.screenX();
		rectF.round(rect);
	}
	
	public void writeToStream(DataOutputStream out) throws IOException {
	}
	
	public GraphicsImage(DataInputStream in) throws IOException {
		super(Tool.IMAGE);
		int version = in.readInt();
		if (version > 1)
			throw new IOException("Unknown version!");
		tool = Tool.values()[in.readInt()];
		if (tool != Tool.LINE)
			throw new IOException("Unknown tool type!");
//		
//		p0 = new Controlpoint(in.readFloat(), in.readFloat());
//		p1 = new Controlpoint(in.readFloat(), in.readFloat());
//		controlpoints.add(p0);
//		controlpoints.add(p1);
		init();
	}

}
