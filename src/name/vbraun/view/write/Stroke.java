package name.vbraun.view.write;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Math;

import org.libharu.Page.LineCap;
import org.libharu.Page.LineJoin;

import junit.framework.Assert;

import android.util.FloatMath;
import android.util.Log;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class Stroke extends Graphics {
	private static final String TAG = "Stroke";
	
	// line thickness in fraction of the larger dimension of the page
	public static final float LINE_THICKNESS_SCALE = 1/1600f;
		
	// the actual data
	protected int N;
	protected float[] position_x;
	protected float[] position_y;
	protected float[] pressure;
	
	private final Paint mPen = new Paint();
	protected int pen_thickness = 0;
	protected int pen_color = Color.BLACK;
	
	public Stroke(Tool pen_type, float[] x, float[] y, float[] p, int from, int to) {
		super(pen_type);
		Assert.assertTrue("Pen type is not actual pen.",
				pen_type == Tool.FOUNTAINPEN || pen_type == Tool.PENCIL);
		N = to-from;
		Assert.assertTrue("Stroke must consist of at least one point", N>0);
		position_x = Arrays.copyOfRange(x, from, to);
		position_y = Arrays.copyOfRange(y, from, to);
		pressure = Arrays.copyOfRange(p, from, to);
	}

	void setPen(int new_pen_thickness, int new_pen_color) {
		pen_thickness = new_pen_thickness;
		pen_color = new_pen_color;
		mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color), 
					 Color.green(pen_color), Color.blue(pen_color));
		mPen.setAntiAlias(true);
		mPen.setStrokeCap(Paint.Cap.ROUND);
		recompute_bounding_box = true;
	}
	
	// static method that exports the pen scaling algorithm
	public static float getScaledPenThickness(float scale, float pen_thickness) {
		return pen_thickness * scale * LINE_THICKNESS_SCALE;
	}
	
	// static method that exports the pen scaling algorithm
	public static float getScaledPenThickness
			(Transformation transform, float pen_thickness) {
		return pen_thickness * transform.scale * LINE_THICKNESS_SCALE;
	}

	// this computes the argument to Paint.setStrokeWidth()
	public float getScaledPenThickness() {
		return getScaledPenThickness(scale, pen_thickness);
	}

	// Get the scaled thickness for a different scale factor (i.e. printing)
	public float getScaledPenThickness(float scale) {
		return getScaledPenThickness(scale, pen_thickness);
	}

	protected void computeBoundingBox() {
		float x0, x1, y0, y1, x, y;
		x0 = x1 = position_x[0] * scale + offset_x;
		y0 = y1 = position_y[0] * scale + offset_y;
		for (int i=1; i<N; i++) {
			x = position_x[i] *scale + offset_x;
			y = position_y[i] *scale + offset_y;
			x0 = Math.min(x0, x);
			x1 = Math.max(x1, x);
			y0 = Math.min(y0, y);
			y1 = Math.max(y1, y);
		}
		bBox.set(x0, y0, x1, y1);
		float extra = -getScaledPenThickness()/2-1;
		bBox.inset(extra, extra);		
		recompute_bounding_box = false;
	}
	
	protected void applyInverseTransform() {
		float x, y;
		for (int i=0; i<N; i++) {
			x = position_x[i];
			y = position_y[i];
			position_x[i] = (x-offset_x)/scale;
			position_y[i] = (y-offset_y)/scale;
		}
		recompute_bounding_box = true;
	}
	
	public float distance(float x_screen, float y_screen) {
		float x = (x_screen-offset_x) / scale;
		float y = (y_screen-offset_y) / scale;
		float d = Math.abs(x - position_x[0]) + Math.abs(y - position_y[0]);
		for (int i=1; i<N; i++) {
			float d_new = Math.abs(x - position_x[i]) + Math.abs(y - position_y[i]);
			d = Math.min(d, d_new);
		}
		return d * scale;
	}
	
	public boolean intersects(RectF r_screen) {
		// Log.v(TAG, ""+r_screen.left+" "+r_screen.bottom+" "+r_screen.right+" "+r_screen.top);
		RectF r = new RectF((r_screen.left -offset_x)/scale, (r_screen.top   -offset_y)/scale, 
					        (r_screen.right-offset_x)/scale, (r_screen.bottom-offset_y)/scale);
		// Log.v(TAG, ""+r.left+" "+r.bottom+" "+r.right+" "+r.top);
		for (int i=0; i<N; i++)
			if (r.contains(position_x[i], position_y[i]))
				return true;
		return false;
	}
	
	public void draw(Canvas c, RectF bounding_box) {
		// Page.draw already checked the bounding box, we definitely need to draw
		if (recompute_bounding_box) computeBoundingBox();	
		final float scaled_pen_thickness = getScaledPenThickness();
		if (tool == Tool.PENCIL)
			mPen.setStrokeWidth(scaled_pen_thickness);
		float x0, x1, y0, y1, p0, p1=0;
		//c.drawRect(left, top, right, bottom, paint)
		// note: we offset the first point by 1/10 pixel since android does not draw lines with start=end
		x0 = position_x[0] * scale + offset_x + 0.1f;
		y0 = position_y[0] * scale + offset_y;
		p0 = pressure[0];
		for (int i=1; i<N; i++) {			
			x1 = position_x[i] * scale + offset_x;
			y1 = position_y[i] * scale + offset_y;
			if (tool == Tool.FOUNTAINPEN) {
				p1 = pressure[i];
				mPen.setStrokeWidth((p0+p1)/2 * scaled_pen_thickness);
			}
			c.drawLine(x0, y0, x1, y1, mPen);
			x0 = x1;  y0 = y1;  p0 = p1;
		}
	}

	public void writeToStream(DataOutputStream out) throws IOException {
		out.writeInt(2);  // protocol #1
		out.writeInt(pen_color);
		out.writeInt(pen_thickness);
		out.writeInt(tool.ordinal());
		out.writeInt(N);
		for (int i=0; i<N; i++) {
			out.writeFloat(position_x[i]);
			out.writeFloat(position_y[i]);
			out.writeFloat(pressure[i]);
		}
	}
	
	public Stroke(DataInputStream in) throws IOException {
		super(Tool.FOUNTAINPEN);
		int version = in.readInt();
		if (version < 1  ||  version > 2)
			throw new IOException("Unknown version!");
		pen_color = in.readInt();
		pen_thickness = in.readInt();
		tool = Tool.values()[in.readInt()];
		setPen(pen_thickness, pen_color);
		N = in.readInt();
		position_x = new float[N];
		position_y = new float[N];
		pressure = new float[N];
		for (int i=0; i<N; i++) {
			position_x[i] = in.readFloat();
			position_y[i] = in.readFloat();
			pressure[i] = in.readFloat();
		}
		if (version == 1) {
			// I changed the thickness quantization for v2
			pen_thickness *= 2;
			simplify();
		}
	}

	// Reduce the number of points
	// http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
	// Assumes that x,y coordinates and pressure are scaled to be within [0,1]
	// for example, using apply_inverse_transform
	// non-standard metric for "perpendicular distance" for numerical stability
	protected void simplify() {
		//		points.add(0);
		//		points.add(N-1);
		//		ListIterator<Integer> point_iter = points.listIterator(1);
		//		simplifyRecursion(0, N-1, point_iter);
		LinkedList<Integer> points = simplifyWithoutRecursion();
		
		int new_N = points.size();
		float[] new_position_x = new float[new_N];
		float[] new_position_y = new float[new_N];
		float[] new_pressure = new float[new_N];
		int n = 0;
		ListIterator<Integer> point_iter = points.listIterator();
		while (point_iter.hasNext()) {
			int p = point_iter.next();
			new_position_x[n] = position_x[p];
			new_position_y[n] = position_y[p];
			new_pressure[n] = pressure[p];
			n++;
		}
		Assert.assertEquals(n, new_N);
		N = new_N;
		position_x = new_position_x;
		position_y = new_position_y;
		pressure = new_pressure;
	}

	private static float EPSILON = 2e-4f;
	
	// find the mid point with the largest deviation from a straight line
	// return -1 if there is none up to the desired precision EPSILON
	private Integer simplifyFindMidPoint(Integer point0, Integer point1) {
		float x0 = position_x[point0];
		float y0 = position_y[point0];
		float p0 = pressure[point0];
		float x1 = position_x[point1];
		float y1 = position_y[point1];
		float p1 = pressure[point1];

		// the line has the equation ax + by + c = 0 
		float a = y1-y0;
		float b = x0-x1;
		float c = x1*y0-x0*y1;
		float normal_abs = FloatMath.sqrt(a*a+b*b);

		// distance between p0 and p1
		float dx = x1-x0;
		float dy = y1-y0;
		float distance_01 = FloatMath.sqrt(dx*dx+dy*dy);

		// average pressure is the 3rd dimension (line thickness is determined by it)
		float p_avg = (p0+p1)/2;

		int mid = -1;
		float distance_max = 0;
		for (int i=point0+1; i<point1; i++) {
			float x = position_x[i];
			float y = position_y[i];
			float p = pressure[i];
			float distance = 0;
			
			// distance in pressure
			if (tool == Tool.FOUNTAINPEN) {
				float p_0_avg = (p0+p)/2;
				float p_1_avg = (p1+p)/2;
				float pressure_difference = 
					Math.max(Math.abs(p_0_avg-p_avg), Math.abs(p_1_avg-p_avg)) * LINE_THICKNESS_SCALE * 3;
				distance = Math.max(distance, pressure_difference);
			}
			
			// distance for degenerate triangles where midpoint is far away from p0, p1
			float dx0 = x-x0;
			float dy0 = y-y0;
			float distance_p0 = FloatMath.sqrt(dx0*dx0+dy0*dy0);
			distance = Math.max(distance, distance_p0-distance_01);
			float dx1 = x-x1;
			float dy1 = y-y1;
			float distance_p1 = FloatMath.sqrt(dx1*dx1+dy1*dy1);
			distance = Math.max(distance, distance_p1-distance_01);
			
			// perpendicular distance
			if (distance_01>EPSILON) {
				float d = Math.abs(a*x+b*y+c)/normal_abs;
				distance = Math.max(distance, d);
			}
			
			if (distance > distance_max) {
				distance_max = distance;
				mid = i;
			}
		}
		if (distance_max < EPSILON || mid == -1) return null;
		return mid;
	}
	
	private void simplifyRecursion(Integer point0, Integer point1, ListIterator<Integer> iter) {
		Integer mid = simplifyFindMidPoint(point0, point1);
		if (mid==null) return;
		iter.add(mid);
		simplifyRecursion(mid, point1, iter);
		iter.previous();
		simplifyRecursion(point0, mid, iter);		
	}

	// Implement Ramer-Douglas-Peucker without recursion since stack space is very limited
	private LinkedList<Integer> simplifyWithoutRecursion() {
		LinkedList<Integer> simplified_points = new LinkedList<Integer>();
		simplified_points.add(0);
		LinkedList<Integer> endpoint = new LinkedList<Integer>();
		endpoint.addLast(N-1);
		Integer point0 = 0;
		while (!endpoint.isEmpty()) {
			Integer point1 = endpoint.getLast();
			Integer mid = simplifyFindMidPoint(point0, point1);
			//	Log.d(TAG, "Simplify "+point0+" - "+point1+" contains "+mid);
			if (mid == null) { 
				simplified_points.add(point1);
				point0 = point1;
				endpoint.removeLast();
			} else
				endpoint.addLast(mid);
		}
		return simplified_points;
	}
	
	public void render(org.libharu.Page pdf, ScalePDF scale) {
        float red  = Color.red(pen_color)/(float)0xff;
        float green = Color.green(pen_color)/(float)0xff;
        float blue = Color.blue(pen_color)/(float)0xff;
        pdf.setRGBStroke(red, green, blue);
		switch (tool) {
		case FOUNTAINPEN:
			renderFountainpenPDF(pdf, scale);
			return;
		case PENCIL:
			renderPencilPDF(pdf, scale);
			return;
		}
		Log.e(TAG, "Unknown stroke type.");
	}
	
	private void renderFountainpenPDF(org.libharu.Page pdf, ScalePDF scale) {
		float scaled_pen_thickness = getScaledPenThickness(scale.getScale());
		pdf.setLineCap(LineCap.ROUND_END);
		pdf.setLineJoin(LineJoin.ROUND_JOIN);
        float x0 = scale.scaledX(position_x[0], position_y[0]);
        float y0 = scale.scaledY(position_x[0], position_y[0]);        
        float p0 = pressure[0];
        for (int i=1; i<N; i++) {
        	float x1 = scale.scaledX(position_x[i], position_y[i]);
            float y1 = scale.scaledY(position_x[i], position_y[i]);
            float p1 = pressure[i];
            pdf.setLineWidth((scaled_pen_thickness*(p0+p1)/2));
            pdf.moveTo(x0, y0);
            pdf.lineTo(x1, y1);
            pdf.stroke();
            x0 = x1;
            y0 = y1;
            p0 = p1;
        }
	}
	
	private void renderPencilPDF(org.libharu.Page pdf, ScalePDF scale) {
		float scaled_pen_thickness = getScaledPenThickness(scale.getScale());
		pdf.setLineWidth(scaled_pen_thickness);
		pdf.setLineCap(LineCap.ROUND_END);
		pdf.setLineJoin(LineJoin.ROUND_JOIN);
        float x = scale.scaledX(position_x[0], position_y[0]);
        float y = scale.scaledY(position_x[0], position_y[0]);
        pdf.moveTo(x, y);
        for (int i=1; i<N; i++) {
         	x = scale.scaledX(position_x[i], position_y[i]);
            y = scale.scaledY(position_x[i], position_y[i]);
            pdf.lineTo(x, y);
        }
        pdf.stroke();
	}

}

