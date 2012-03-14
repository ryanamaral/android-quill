package name.vbraun.view.write;

import java.util.LinkedList;

import junit.framework.Assert;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class GraphicsLine extends GraphicsControlpoint {
	private static final String TAG = "GraphicsLine";
	
	protected GraphicsLine(Tool mTool) {
		super(mTool);
		Assert.assertTrue(tool == Tool.LINE);
	}

	private Controlpoint p0, p1;
	private final Paint pen = new Paint();
	private int pen_thickness = 0;
	private int pen_color = Color.BLACK;
	
	@Override
	protected LinkedList<Controlpoint> getControlpoints() {
		LinkedList<Controlpoint> points = new LinkedList<Controlpoint>();
		points.add(p0);
		points.add(p1);
		return points;
	}

	void setPen(int new_pen_thickness, int new_pen_color) {
		pen_thickness = new_pen_thickness;
		pen_color = new_pen_color;
		pen.setARGB(Color.alpha(pen_color), Color.red(pen_color), 
					 Color.green(pen_color), Color.blue(pen_color));
		pen.setAntiAlias(true);
		pen.setStrokeCap(Paint.Cap.ROUND);
		recompute_bounding_box = true;
	}
	
	// this computes the argument to Paint.setStrokeWidth()
	public float getScaledPenThickness() {
		return Stroke.getScaledPenThickness(scale, pen_thickness);
	}
	
	@Override
	public boolean intersects(RectF r_screen) {
		return false;
	}

	@Override
	public void draw(Canvas c, RectF bounding_box) {
		final float scaled_pen_thickness = getScaledPenThickness();
		if (tool == Tool.PENCIL)
			pen.setStrokeWidth(scaled_pen_thickness);
		float x0, x1, y0, y1;
		// note: we offset the first point by 1/10 pixel since android does not draw lines with start=end
		x0 = p0.x * scale + offset_x + 0.1f;
		x1 = p1.x * scale + offset_x;
		y0 = p0.y * scale + offset_y;
		y1 = p1.y * scale + offset_y;
		c.drawLine(x0, y0, x1, y1, pen);
	}

}
