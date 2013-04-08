package name.vbraun.view.write;


import java.util.Calendar;
import java.util.Locale;

import com.write.Quill.artist.Artist;
import com.write.Quill.artist.LineStyle;

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
		aspectRatio = AspectRatio.closestMatch(aspect);
		heightMm = aspectRatio.guessHeightMm();
		widthMm = aspectRatio.guessWidthMm();
	}
	
	private int paperColour = Color.WHITE;
	
	public int getPaperColour() {	
			return paperColour;
	}
	
	public void setPaperColour(int paperColour) {
		this.paperColour = paperColour;
	}

	private void drawGreyFrame(Canvas canvas, RectF bBox, Transformation t) {
		paper.set(t.offset_x, t.offset_y, 
				  t.offset_x+aspectRatio.ratio*t.scale, t.offset_y+t.scale);
		if (!paper.contains(bBox))
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);		
	}
	
	/**
	 * This is where we clear the (possibly uninitialized) backing bitmap in the canvas.
	 * The background is filled with white, which is most suitable for printing.
	 * @param canvas The canvas to draw on 
	 * @param bBox   The damage area
	 * @param t      The linear transformation from paper to screen
	 */
	public void drawWhiteBackground(Canvas canvas, RectF bBox, Transformation t) {
		drawGreyFrame(canvas, bBox, t);
		paint.setARGB(0xff, 0xff, 0xff, 0xff);
		canvas.drawRect(paper, paint);		
	}

	/**
	 * This is where we clear the (possibly uninitialized) backing bitmap in the canvas.
	 * @param canvas The canvas to draw on 
	 * @param bBox   The damage area
	 * @param t      The linear transformation from paper to screen
	 */
	public void drawEmptyBackground(Canvas canvas, RectF bBox, Transformation t) {
		drawGreyFrame(canvas, bBox, t);
		paint.setColor(paperColour);
		canvas.drawRect(paper, paint);		
	}
	
	public void draw(Canvas canvas, RectF bBox, Transformation t) {
		//Log.v(TAG, "draw_paper at scale "+scale);
		// the paper is 1 high and aspect_ratio wide
		drawEmptyBackground(canvas, bBox, t);
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
        case MUSIC:
            draw_music_manuscript(canvas, t);
            return;
		case HEX:
			// TODO
			return;
		}
	}
	
	private void draw_dayplanner(Canvas c, Transformation t, Calendar calendar) {
		float x0, x1, y, y0, y1;
		float textHeight;
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

		// Header
		float headerHeightMm = 30f;
		x0 = t.applyX(marginMm/heightMm);
		x1 = t.applyX((widthMm-marginMm)/heightMm);
		y = t.applyY(headerHeightMm/heightMm);
		c.drawLine(x0, y, x1, y, paint);
		
		textHeight = t.scaleText(24f);
		paint.setTextSize(textHeight);
		y = t.applyY(marginMm/heightMm) + textHeight;
		c.drawText(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), x0, y, paint);
				
// I'm leaving this out for now; Should there be a gui to pick the day of the year? Or just let the user write the date?
//		y0 = t.applyY((widthMm-marginMm)/widthMm);
//		c.drawText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), x0, y0, paint);
//		
//		paint.setTextSize(t.scaleText(12f));
//		
//		c.drawText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()), x0 + t.applyX(2*marginMm/heightMm), y0 + t.applyY(marginMm/heightMm), paint);
//		
//		paint.setTextSize(t.scaleText(10f));
//		font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
//		paint.setTextAlign(Align.RIGHT);
//		c.drawText("Week " + calendar.get(Calendar.WEEK_OF_YEAR),t.applyX((widthMm-marginMm)/heightMm), t.applyY((float) (marginMm*1.75/widthMm)), paint);
		
		// Details
		paint.setTextAlign(Align.LEFT);
		paint.setARGB(0xff, shade, shade, shade);
		float spacingMm = COLLEGERULED_SPACING;
		int n = (int) Math.floor((heightMm-headerHeightMm-marginMm) / spacingMm);
				
		x0 = t.applyX(marginMm/heightMm);
		x1 = t.applyX((widthMm-marginMm)/heightMm);
		
		int hourMarker = 7;
		textHeight = t.scaleText(10f);
		paint.setTextSize(textHeight);
		
		for (int i=1; i<=n; i++) {
			y = t.applyY((headerHeightMm+i*spacingMm)/heightMm);
			c.drawLine(x0, y, x1, y, paint);
			
			if (i % 2 == 1){
				y = t.applyY((headerHeightMm+(i-0.5f)*spacingMm)/heightMm) + textHeight/2;
				c.drawText(hourMarker + ":", x0, y, paint);
				
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
		y0 = t.applyY(0);
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

	
	private void draw_ruled(Canvas c, Transformation t, float lineSpacing, float margin) {
		
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
	
    private void draw_music_manuscript(Canvas c, Transformation t) {
        float lineSpacingMm = 2.5f;
        float staveHeight = 4 * lineSpacingMm;
        float staffTopMarginMm = 25.0f;
        float staffBottomMarginMm = 15.0f;
        float staffSideMarginMm = 15.0f;
        int staveCount;
        if (aspectRatio.isPortrait())
        	staveCount = 12;
        else
        	staveCount = 8;
        float staffTotal = staffTopMarginMm + staffBottomMarginMm + staveCount*staveHeight;
        float staffSpacing = staveHeight + (heightMm-staffTotal)/(staveCount-1);

        float x0, x1, y;
        
        int shade = 0xaa;
        float threshold = 1500;
        if (t.scale < threshold)
                shade += (int)((threshold-t.scale)/threshold*(0xff-shade));
        paint.setARGB(0xff, shade, shade, shade);
        
        paint.setStrokeWidth(0);

        x0 = t.applyX(staffSideMarginMm/heightMm);
        x1 = t.applyX((widthMm-staffSideMarginMm)/heightMm);

        for (int i=0; i<staveCount; i++) {
                for (int j=0; j<5; j++) {
                        y = t.applyY((staffTopMarginMm + i*staffSpacing + j*lineSpacingMm)/heightMm);
                        c.drawLine(x0, y, x1, y, paint);
                }
        }
    }

	
	public void render(Artist artist) {
		if (!artist.getBackgroundVisible()) return;
		switch (paperType) {
		case EMPTY:
			return;
		case RULED:
			render_ruled(artist, LEGALRULED_SPACING, 31.75f);
			return;
		case COLLEGERULED:
			render_ruled(artist, COLLEGERULED_SPACING, 31.75f);
			return;			
		case NARROWRULED:
			render_ruled(artist, NARROWRULED_SPACING, 0.0f);
			return;						
		case QUAD:
			render_quad(artist);
			return;
		case CORNELLNOTES:
			render_cornellnotes(artist);
			return;
		case DAYPLANNER:
			return;			
		case MUSIC:
			render_music_manuscript(artist);
		case HEX:
		return;
		}
	}
		
	
	private void render_ruled(Artist artist, float lineSpacing, float margin) {
		float spacingMm = lineSpacing;
		float vertLineMm = margin;
		LineStyle line = new LineStyle();
		line.setColor(0f, 0f, 0f);
		line.setWidth(0);
		int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
		float x0 = marginMm / heightMm;
		float x1 = (widthMm - marginMm) / heightMm;
		for (int i = 1; i <= n; i++) {
			float y = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
			artist.drawLine(x0, y, x1, y, line);
		}

		// Paint margin
		if (margin > 0.0f) {
			line.setColor(1f, 0f, 0f);
			line.setWidth(0);
			float y0 = marginMm / heightMm;
			float y1 = (heightMm - marginMm) / heightMm;
			float x = vertLineMm / widthMm;
			artist.drawLine(x, y0, x, y1, line);
		}
	}
	
	private void render_quad(Artist artist) {
		float spacingMm = 5f;
		int nx, ny;
		float x, x0, x1, y, y0, y1;
		LineStyle line = new LineStyle();
		line.setColor(0f, 0f, 0f);
		line.setWidth(0);
		ny = (int)Math.floor((heightMm-2*marginMm) / spacingMm);
		nx = (int)Math.floor((widthMm-2*marginMm) / spacingMm);
		float marginXMm = (widthMm-nx*spacingMm)/2;
		float marginYMm = (heightMm-ny*spacingMm)/2;
		x0 = marginXMm/heightMm;
		x1 = (widthMm-marginXMm)/heightMm;
		y0 = marginYMm/heightMm;
		y1 = (heightMm-marginYMm)/heightMm;
		for (int i=0; i<=ny; i++) {
			y = (marginYMm + i*spacingMm)/heightMm;
			artist.drawLine(x0, y, x1, y, line);
		}
		for (int i=0; i<=nx; i++) {
			x = (marginXMm + i*spacingMm)/heightMm;
			artist.drawLine(x, y0, x, y1, line);
		}
	}
	
	private void render_cornellnotes(Artist artist) {	
		float x0, x1, y0, y1;
		final float MARGIN = 1.25f;
		LineStyle line = new LineStyle();
		line.setColor(0f, 0f, 0f);
		line.setWidth(0);

		// Cue Column
		x0 = (MARGIN*INCH_in_MM)/widthMm;
		x1 = x0;
		y0 = 0f;
		y1 = (heightMm-(MARGIN*INCH_in_MM))/heightMm;
		artist.drawLine(x0, y0, x1, y1, line);
		
		// Summary area at base of page
		x0 = 0f;
		x1 = widthMm/heightMm;
		y0 = (heightMm-(MARGIN*INCH_in_MM))/heightMm;
		y1 = y0;
		artist.drawLine(x0, y0, x1, y1, line);
		
		// Details
		float spacingMm = COLLEGERULED_SPACING;
		int n = (int)Math.floor((heightMm-(MARGIN*INCH_in_MM) - 2 * marginMm) / spacingMm);
		
		x0 = (MARGIN*INCH_in_MM)/widthMm + marginMm/heightMm;
		x1 = (widthMm-marginMm)/heightMm;
		
		for (int i=1; i<=n; i++) {
			float y = ((heightMm-n*spacingMm - MARGIN*INCH_in_MM)/2 + i*spacingMm)/heightMm;
			artist.drawLine(x0, y, x1, y, line);
		}
		
	}

    private void render_music_manuscript(Artist artist) {
        float lineSpacingMm = 2.0f;
        float staveHeight = 4 * lineSpacingMm;
        float staffTopMarginMm = 25.0f;
        float staffBottomMarginMm = 15.0f;
        float staffSideMarginMm = 15.0f;                

        int staveCount = 12;
        if (aspectRatio.isPortrait())
        	staveCount = 12;
        else
        	staveCount = 8;
        float staffTotal = staffTopMarginMm + staffBottomMarginMm + staveCount*staveHeight;
        float staffSpacing = staveHeight + (heightMm-staffTotal)/(staveCount-1);

        float x0, x1, y;        
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        
        line.setWidth(0);
        staffSpacing = staveHeight + (heightMm-staffTotal)/(staveCount-1);

        x0 = staffSideMarginMm/heightMm;
        x1 = (widthMm-staffSideMarginMm)/heightMm;

        for (int i=0; i<staveCount; i++) {
                for (int j=0; j<5; j++) {
                        y = (staffTopMarginMm + i*staffSpacing + j*lineSpacingMm)/heightMm;
                        artist.drawLine(x0, y, x1, y, line);
                }
        }
}


}

