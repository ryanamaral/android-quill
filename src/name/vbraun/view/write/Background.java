package name.vbraun.view.write;


import java.util.Calendar;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.graphics.Typeface;


public class Background {
	public static final String TAG = "Background"; 
	private static final float INCH_in_CM = 2.54f;
	private static final float INCH_in_MM = INCH_in_CM * 10;
	
	private static final float marginMm = 5;
	
	private static final float LEGALRULED_SPACING = 8.7f;
	private static final float COLLEGERULED_SPACING = 7.1f;
	private static final float NARROWRULED_SPACING = 6.35f;

	private Paper.Type paperType= Paper.Type.EMPTY;
	private AspectRatio aspectRatio = AspectRatio.Table[0];
	private float heightMm, widthMm;	
	
	private final RectF paper = new RectF();
	private final Paint paint = new Paint();
	

	public void setPaperType(Paper.Type paper) {
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
			draw_ruled(canvas, t, LEGALRULED_SPACING, 31.75f);
			return;
		case COLLEGERULED:
			draw_ruled(canvas, t, COLLEGERULED_SPACING, 31.75f);
			return;			
		case NARROWRULED:
			draw_ruled(canvas, t, NARROWRULED_SPACING, 0.0f);
			return;						
		case QUAD:
			draw_quad(canvas, t);
			return;
		case CORNELLNOTES:
			draw_cornellnotes(canvas, t);
			return;
		case DAYPLANNER:
			draw_dayplanner(canvas, t, Calendar.getInstance());
			return;			
		case HEX:
			// TODO
			return;
		}
	}
	
	private void draw_dayplanner(Canvas c, Transformation t, Calendar calendar) {

		float x0, x1, y0, y1;
		int shade = 0xaa;
		float threshold = 1500;
		if (t.scale < threshold)
			shade += (int)((threshold-t.scale)/threshold*(0xff-shade));
		//paint.setARGB(0xff, shade, shade, shade);
		paint.setStrokeWidth(0);
		paint.setColor(Color.DKGRAY);
		
		Typeface font = Typeface.create(Typeface.SERIF, Typeface.BOLD);
		paint.setTypeface(font);
		paint.setAntiAlias(true);

		paint.setTextSize(t.scaleText(20f));
		
		// Header
		x0 = t.applyX(marginMm/heightMm);
		x1 = t.applyX((widthMm-marginMm)/heightMm);
		y0 = t.applyY(2 * marginMm/widthMm);
		y1 = y0;
		
		c.drawLine(x0, y0, x1, y1, paint);
		
		
		c.drawText(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), x0, t.applyY(marginMm/widthMm), paint);
		
		c.drawText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), x0, y0 + t.applyY(marginMm/heightMm), paint);
		
		paint.setTextSize(t.scaleText(12f));
		
		c.drawText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()), x0 + t.applyX(2*marginMm/heightMm), y0 + t.applyY(marginMm/heightMm), paint);
		
		paint.setTextSize(t.scaleText(10f));
		font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
		paint.setTextAlign(Align.RIGHT);
		c.drawText("Week " + calendar.get(Calendar.WEEK_OF_YEAR),t.applyX((widthMm-marginMm)/heightMm), t.applyY((float) (marginMm*1.75/widthMm)), paint);
		
		// Details
		paint.setTextAlign(Align.LEFT);
		paint.setARGB(0xff, shade, shade, shade);
		float spacingMm = COLLEGERULED_SPACING;
		int n = (int) ((int)Math.floor((heightMm-t.applyY(marginMm/widthMm)) - marginMm) / spacingMm);
				
		x0 = t.applyX(marginMm/heightMm);
		x1 = t.applyX((widthMm-marginMm)/heightMm);
		
		int hourMarker = 7;
		paint.setTextSize(t.scaleText(10f));
		
		for (int i=1; i<=n; i++) {
			float y = t.applyY(((heightMm-n*spacingMm - marginMm)/2 + i*spacingMm)/heightMm);
			c.drawLine(x0, y, x1, y, paint);
			
			if (i % 2 == 0){
				float z = t.applyY(((heightMm-n*spacingMm - marginMm/2)/2 + i*spacingMm + marginMm/2)/heightMm);
				c.drawText(hourMarker + ":", x0, z, paint);
				
				hourMarker++;
				if (hourMarker == 13)
					hourMarker = 1;
			}
		
		}
	}

	private void draw_cornellnotes(Canvas c, Transformation t) {
		
		float x0, x1, y0, y1;
		final float MARGIN = 1.25f;
				
		int shade = 0xaa;
		float threshold = 1500;
		if (t.scale < threshold)
			shade += (int)((threshold-t.scale)/threshold*(0xff-shade));
		paint.setARGB(0xff, shade, shade, shade);
		paint.setStrokeWidth(0);
		
		// Cue Column
		x0 = t.applyX((MARGIN*INCH_in_MM)/widthMm);
		x1 = x0;
		y0 = 0;
		y1 = t.applyY((heightMm-(MARGIN*INCH_in_MM))/heightMm);
		
		c.drawLine(x0, y0, x1, y1, paint);
		
		// Summary area at base of page
		x0 = t.applyX(0);
		x1 = t.applyX(widthMm/heightMm);
		y0 = t.applyY((heightMm-(MARGIN*INCH_in_MM))/heightMm);
		y1 = y0;
		
		c.drawLine(x0, y0, x1, y1, paint);
		
		// Details
		float spacingMm = COLLEGERULED_SPACING;
		int n = (int)Math.floor((heightMm-(MARGIN*INCH_in_MM) - 2 * marginMm) / spacingMm);
		
		x0 = t.applyX((MARGIN*INCH_in_MM)/widthMm + marginMm/heightMm);
		x1 = t.applyX((widthMm-marginMm)/heightMm);
		
		for (int i=1; i<=n; i++) {
			float y = t.applyY(((heightMm-n*spacingMm - MARGIN*INCH_in_MM)/2 + i*spacingMm)/heightMm);
			c.drawLine(x0, y, x1, y, paint);
		}
		
	}

	
	
	private void draw_ruled(Canvas c, Transformation t, float lineSpacing, float margin){
		
		float spacingMm = lineSpacing;
		float vertLineMm = margin;
		
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

		// Paint margin
		if (margin > 0.0f){
			paint.setARGB(0xff, 0xff, shade, shade);
			paint.setStrokeWidth(0);
			float y0 = t.applyY(marginMm/heightMm);
			float y1 = t.applyY((heightMm-marginMm)/heightMm);
			float x = t.applyX(vertLineMm/widthMm);
			c.drawLine(x, y0, x, y1, paint);
		}
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


