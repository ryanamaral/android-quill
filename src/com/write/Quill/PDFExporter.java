package com.write.Quill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import org.libharu.Font;
import org.libharu.Font.BuiltinFont;
import org.libharu.Page.LineCap;
import org.libharu.Page.LineJoin;
import org.libharu.Page.PageDirection;
import org.libharu.Page.PageSize;

import android.graphics.Color;
import android.util.Log;


public class PDFExporter extends org.libharu.Document {
	private static final String TAG = "PDFExporter";

	private org.libharu.Page pdf;
	private Font pageNumberFont;
	private PageSize pageSize;
	
	protected float height;
	protected float width;
	protected int pageNumberSize = 14;
	protected int page_number = 1;
	private float scale;
	private boolean rotate;
	
	private volatile float progress = 0;
	private volatile boolean interrupt = false;
		
	public PDFExporter() {
		super();
		setCompressionMode(CompressionMode.COMP_ALL);
		pageNumberFont = getFont(BuiltinFont.COURIER_BOLD);
	}
	
	private final LinkedList<Page> pages = new LinkedList<Page>();
	
	public void add(Page page) {
		pages.add(page);
	}
	
	public void add(Book book) {
		pages.addAll(book.pages);
	}

	public void add(LinkedList<Page> page_list) {
		pages.addAll(page_list);
	}


	public void setPageSize(PageSize newPageSize) {
		pageSize = newPageSize;
	}
	
	private float scaled_x(float x, float y) {
		if (rotate)
			return y * scale;
		else
			return x * scale;
	}
	
	private float scaled_y(float x, float y) {
		if (rotate)
			return x * scale;
		else
			return (1-y) * scale;
	}

	public void draw() {
		ListIterator<Page> iter = pages.listIterator();
		float len = pages.size();
		while (iter.hasNext()) {
			if (interrupt) return;
			progress = iter.nextIndex()/len;
			draw(iter.next());
		}
	}
	
	private void draw(Page page) {
		boolean page_is_portrait = (page.aspect_ratio < 1);
		pdf = addPage();
		if (page_is_portrait)	{
			pdf.setSize(pageSize, PageDirection.PORTRAIT);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.aspect_ratio);
		} else {
			pdf.setSize(pageSize, PageDirection.LANDSCAPE);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.aspect_ratio);
		}
		rotate = false;
		ListIterator<Stroke> siter = page.strokes.listIterator();
		while (siter.hasNext()) {
			if (interrupt) return;
			draw(siter.next());
		}
		addPageNumber();
	}

	private void draw(Stroke stroke) {
        float red  = Color.red(stroke.pen_color)/(float)0xff;
        float green = Color.green(stroke.pen_color)/(float)0xff;
        float blue = Color.blue(stroke.pen_color)/(float)0xff;
        pdf.setRGBStroke(red, green, blue);
		switch (stroke.pen_type) {
		case FOUNTAINPEN:
			drawStrokeFountainpen(stroke);
			return;
		case PENCIL:
			drawStrokePencil(stroke);
			return;
		}
		Log.e(TAG, "Unknown stroke type.");
	}
	
	private void drawStrokeFountainpen(Stroke stroke) {
		float scaled_pen_thickness = stroke.getScaledPenThickness(scale);
		pdf.setLineCap(LineCap.ROUND_END);
		pdf.setLineJoin(LineJoin.ROUND_JOIN);
        float x0 = scaled_x(stroke.position_x[0], stroke.position_y[0]);
        float y0 = scaled_y(stroke.position_x[0], stroke.position_y[0]);        
        float p0 = stroke.pressure[0];
        for (int i=1; i<stroke.N; i++) {
			if (interrupt) return;
        	float x1 = scaled_x(stroke.position_x[i], stroke.position_y[i]);
            float y1 = scaled_y(stroke.position_x[i], stroke.position_y[i]);
            float p1 = stroke.pressure[i];
            pdf.setLineWidth((scaled_pen_thickness*(p0+p1)/2));
            pdf.moveTo(x0, y0);
            pdf.lineTo(x1, y1);
            pdf.stroke();
            x0 = x1;
            y0 = y1;
            p0 = p1;
        }
	}
	
	private void drawStrokePencil(Stroke stroke) {
		float scaled_pen_thickness = stroke.getScaledPenThickness(scale);
		pdf.setLineWidth(scaled_pen_thickness);
		pdf.setLineCap(LineCap.ROUND_END);
		pdf.setLineJoin(LineJoin.ROUND_JOIN);
        float x = scaled_x(stroke.position_x[0], stroke.position_y[0]);
        float y = scaled_y(stroke.position_x[0], stroke.position_y[0]);
        pdf.moveTo(x, y);
        for (int i=1; i<stroke.N; i++) {
			if (interrupt) return;
         	x = scaled_x(stroke.position_x[i], stroke.position_y[i]);
            y = scaled_y(stroke.position_x[i], stroke.position_y[i]);
            pdf.lineTo(x, y);
        }
        pdf.stroke();
	}

	public void addPageNumber() {
        int margin = 20;
        pdf.setFontAndSize(pageNumberFont, pageNumberSize);
        pdf.beginText();
        pdf.textOut(width-pageNumberSize-margin, height-pageNumberSize-margin,
        			""+page_number);
        pdf.endText();
        page_number++;
	}
		
	public void export(File file) {
		try {
			String path = file.getAbsolutePath();
			saveToFile(path);
		} catch (IOException e) {
			Log.e(TAG, "Error saving PDF file: "+e.toString());
		}	
	}
	
	public int get_progress() {
		return (int)(100*progress);
	}
	
	public void interrupt() {
		interrupt = true;
	}	
}
