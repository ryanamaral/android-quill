package name.vbraun.view.write;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Assert;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
		Log.e(TAG, "draw() "+ rect);
		c.drawRect(rect, paint);
	}

	@Override
	void controlpointMoved(Controlpoint point) {
		super.controlpointMoved(point);
		// make rectangular again
	}

	
	private void computeScreenRect() {
		rectF.bottom = bottom_left.screenY();
		rectF.top    = top_left.screenY();
		rectF.left   = bottom_left.screenX();
		rectF.right  = bottom_right.screenX();
		rectF.sort();
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
