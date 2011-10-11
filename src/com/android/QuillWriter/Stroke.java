package com.android.QuillWriter;

import java.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Math;

import android.util.Log;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class Stroke {
	private static final String TAG = "Stroke";
	
	public enum PenType {
		FOUNTAINPEN, PENCIL, MOVE, ERASER
	}
	
	public final int N;
	public final float[] position_x;
	public final float[] position_y;
	public final float[] pressure;
	protected float x_min = 0f;
	protected float x_max = 0f;
	protected float y_min = 0f;
	protected float y_max = 0f;
	protected RectF bBox;
	private boolean recompute_bounding_box = true;
	
	private float offset_x = 0f;
	private float offset_y = 0f;
	private float scale = 1.0f;
	
	private final Paint mPen = new Paint();
	private int pen_thickness = 0;
	private PenType pen_type = PenType.FOUNTAINPEN;
	private int pen_color = Color.BLACK;
	
	public Stroke(float[] x, float[] y, float[] p, int from, int to) {
		N = to-from;
		assert N>0: "Stroke must consist of at least one point";
		position_x = Arrays.copyOfRange(x, from, to);
		position_y = Arrays.copyOfRange(y, from, to);
		pressure = Arrays.copyOfRange(p, from, to);
	}

	public RectF get_bounding_box() {
		if (recompute_bounding_box) compute_bounding_box();
		return bBox;
	}
	
	void set_pen(PenType new_pen_type, int new_pen_thickness, int new_pen_color) {
		assert new_pen_type == PenType.FOUNTAINPEN || new_pen_type == PenType.PENCIL:
			"Pen type is not actual pen.";
		pen_thickness = new_pen_thickness;
		pen_type = new_pen_type;
		pen_color = new_pen_color;
		mPen.setStrokeWidth(pen_thickness);
		mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color), 
					 Color.green(pen_color), Color.blue(pen_color));
		mPen.setAntiAlias(true);
		mPen.setStrokeCap(Paint.Cap.ROUND);
		recompute_bounding_box = true;
	}
	
	private void compute_bounding_box() {
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
		bBox = new RectF(x0, y0, x1, y1);
		bBox.inset(-pen_thickness/2-1, -pen_thickness/2-1);		
		recompute_bounding_box = false;
	}
	
	protected void set_transform(float dx, float dy, float s) {
		offset_x = dx;
		offset_y = dy;
		scale = s;
		recompute_bounding_box = true;
	}
	
	protected void apply_inverse_transform() {
		float x, y;
		for (int i=0; i<N; i++) {
			x = position_x[i];
			y = position_y[i];
			position_x[i] = (x-offset_x)/scale;
			position_y[i] = (y-offset_y)/scale;
		}
		recompute_bounding_box = true;
	}
	
	public void render(Canvas c) {
		if (recompute_bounding_box) compute_bounding_box();
		float x0, x1, y0, y1, p0, p1=0;
		//c.drawRect(left, top, right, bottom, paint)
		x0 = position_x[0] * scale + offset_x;
		y0 = position_y[0] * scale + offset_y;
		p0 = pressure[0];
		for (int i=1; i<N; i++) {			
			x1 = position_x[i] * scale + offset_x;
			y1 = position_y[i] * scale + offset_y;
			if (pen_type == PenType.FOUNTAINPEN) {
				p1 = pressure[i];
				mPen.setStrokeWidth((p0+p1)/2 * pen_thickness);
			}
			c.drawLine(x0, y0, x1, y1, mPen);
			x0 = x1;  y0 = y1;  p0 = p1;
		}

//		mPath = new Path();
//		mPath.incReserve(N);
//		mPath.moveTo(position_x[0], position_y[0]);
//		for (int i=1; i<N; i++)
//			mPath.lineTo(position_x[i], position_y[i]);
//		// mPath.close();
//
//		RectF bBoxF = new RectF();
//		mPath.computeBounds(bBoxF, false);
//		bBox = new Rect((int)bBoxF.left, (int)bBoxF.bottom, (int)bBoxF.right, (int)bBoxF.top);
//		bBox.sort();
//		bBox.inset(-1,-1);
//		Log.v(TAG, mPath.toString());
//		mPath.close();
//		return mPath;
	}

	public void write_to_stream(DataOutputStream out) throws IOException {
		out.writeInt(1);  // protocol #1
		out.writeInt(pen_color);
		out.writeInt(pen_thickness);
		out.writeInt(pen_type.ordinal());
		out.writeInt(N);
		for (int i=0; i<N; i++) {
			out.writeFloat(position_x[i]);
			out.writeFloat(position_y[i]);
			out.writeFloat(pressure[i]);
		}
	}
	
//	public void read_from_stream(DataInputStream in) throws IOException {
	public Stroke(DataInputStream in) throws IOException {
	int version = in.readInt();
		if (version != 1)
			throw new IOException("Unknown version!");
		pen_color = in.readInt();
		pen_thickness = in.readInt();
		pen_type = PenType.values()[in.readInt()];
		set_pen(pen_type, pen_thickness, pen_color);
		N = in.readInt();
		position_x = new float[N];
		position_y = new float[N];
		pressure = new float[N];
		for (int i=0; i<N; i++) {
			position_x[i] = in.readFloat();
			position_y[i] = in.readFloat();
			pressure[i] = in.readFloat();
		}
	}
	
}

