package com.write.Quill;

import com.write.Quill.Page.PaperType;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;


public class Background {
	public static final String TAG = "Background"; 
	private static final float INCH_in_CM = 2.54f;

	private PaperType paperType= PaperType.EMPTY;
	private AspectRatio aspectRatio = AspectRatio.Table[0];
	private float heightMm, widthMm;
	
	private final RectF paper = new RectF();
	private final Paint paint = new Paint();
	

	public void setPaperType(PaperType paper) {
		paperType = paper;
		paint.setStrokeCap(Cap.BUTT);
	}
	
	public void setAspectRatio(float aspect) {
		aspectRatio = new AspectRatio(aspect);
		heightMm = aspectRatio.guessHeightMm();
		widthMm = aspectRatio.guessWidthMm();
	}
	
	public void draw(Canvas canvas, RectF bBox, Transformation t) {
		//Log.v(TAG, "draw_paper at scale "+scale);
		// the paper is 1 high and aspect_ratio wide
		paper.set(t.offset_x, t.offset_y, 
				  t.offset_x+aspectRatio.ratio*t.scale, t.offset_y+t.scale);
		if (!paper.contains(bBox))
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
		paint.setARGB(0xff, 0xff, 0xff, 0xff);
		canvas.drawRect(paper, paint);
		switch (paperType) {
		case EMPTY:
			return;
		case RULED:
			draw_ruled(canvas, t);
			return;
		case QUAD:
			draw_quad(canvas, t);
			return;
		case HEX:
			// TODO
			return;
		}
	}
	
	private static final float marginMm = 5;
	
	private void draw_ruled(Canvas c, Transformation t) {
		float spacingMm = 8.7f;
		float vertLineMm = 31.75f;
		
		int shade = 0xaa;
		float threshold = 1500;
		if (t.scale < threshold)
			shade += (int)((threshold-t.scale)/threshold*(0xff-shade));
		paint.setARGB(0xff, shade, shade, shade);
		
		paint.setStrokeWidth(0);
		int n = (int)Math.floor((heightMm-2*marginMm) / spacingMm) - 2;
		float x0 = t.applyX(marginMm/heightMm);
		float x1 = t.applyX((widthMm-marginMm)/heightMm);
		for (int i=1; i<=n; i++) {
			float y = t.applyY(((heightMm-n*spacingMm)/2 + i*spacingMm)/heightMm);
			c.drawLine(x0, y, x1, y, paint);
		}

		paint.setARGB(0xff, 0xff, shade, shade);
		paint.setStrokeWidth(0);
		float y0 = t.applyY(marginMm/heightMm);
		float y1 = t.applyY((heightMm-marginMm)/heightMm);
		float x = t.applyX(vertLineMm/widthMm);
		c.drawLine(x, y0, x, y1, paint);
	}
	
	private void draw_quad(Canvas c, Transformation t) {
		float spacingMm = 5f;
		int nx, ny;
		float x, x0, x1, y, y0, y1;
		
		int shade = 0xaa;
		float threshold = 1500;
		if (t.scale < threshold)
			shade += (int)((threshold-t.scale)/threshold*(0xff-shade));
		paint.setARGB(0xff, shade, shade, shade);
		
		paint.setStrokeWidth(0);
		ny = (int)Math.floor((heightMm-2*marginMm) / spacingMm);
		nx = (int)Math.floor((widthMm-2*marginMm) / spacingMm);
		float marginXMm = (widthMm-nx*spacingMm)/2;
		float marginYMm = (heightMm-ny*spacingMm)/2;
		x0 = t.applyX(marginXMm/heightMm);
		x1 = t.applyX((widthMm-marginXMm)/heightMm);
		y0 = t.applyY(marginYMm/heightMm);
		y1 = t.applyY((heightMm-marginYMm)/heightMm);
		for (int i=0; i<=ny; i++) {
			y = t.applyY((marginYMm + i*spacingMm)/heightMm);
			c.drawLine(x0, y, x1, y, paint);
		}
		for (int i=0; i<=nx; i++) {
			x = t.applyX((marginXMm + i*spacingMm)/heightMm);
			c.drawLine(x, y0, x, y1, paint);
		}
	}
}


