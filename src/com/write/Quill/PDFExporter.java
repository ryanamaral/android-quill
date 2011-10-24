package com.write.Quill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import crl.android.pdfwriter.*;

public class PDFExporter extends PDFWriter {
	private static final String TAG = "PDFExporter";

	protected int height = PaperSize.A4_HEIGHT;
	protected int width = PaperSize.A4_WIDTH;
	protected int page_number_size = 14;
	protected int page_number = 1;
	private boolean first_page = true;
	private float scale;
	private boolean rotate;
	
	private volatile float progress = 0;
	private volatile boolean interrupt = false;
	
	public PDFExporter(int w, int h) {
		super(w, h);
		width = w;
		height = h;
	}
	
	public PDFExporter() {
		super(PaperSize.A4_WIDTH, PaperSize.A4_HEIGHT);
		width = PaperSize.A4_WIDTH;
		height = PaperSize.A4_HEIGHT;
	}
	
	public void add(Book book) {
		ListIterator<Page> piter = book.pages.listIterator(); 
		while (piter.hasNext()) {
			if (interrupt) return;
			add(piter.next());
		}
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

	public void add(Page page) {
		boolean page_is_portrait = (page.aspect_ratio < 1);
		boolean paper_is_portrait = (width < height);
		if (page_is_portrait ^ paper_is_portrait) {
			rotate = true;
			scale = Math.min(height/page.aspect_ratio, width);
		} else {
			rotate = false;
			scale = Math.min(height, width/page.aspect_ratio);
		}
		if (!first_page)
			newPage();
		first_page = false;
		ListIterator<Stroke> siter = page.strokes.listIterator();
//		if (siter.hasNext())
//		 	addStroke(siter.next());
		float len = page.strokes.size();
		while (siter.hasNext()) {
			if (interrupt) return;
			progress = siter.nextIndex()/len;
			add(siter.next());
		}
		addPageNumber();
	}

	public void add(Stroke stroke) {
        float red  = Color.red(stroke.pen_color)/(float)0xff;
        float green = Color.green(stroke.pen_color)/(float)0xff;
        float blue = Color.blue(stroke.pen_color)/(float)0xff;
        addRawContent(""+red+" "+green+" "+blue+" RG\n"); // color
		switch (stroke.pen_type) {
		case FOUNTAINPEN:
			addStrokeFountainpen(stroke);
			return;
		case PENCIL:
			addStrokePencil(stroke);
			return;
		}
		Log.e(TAG, "Unknown stroke type.");
	}
	
	private void addStrokeFountainpen(Stroke stroke) {
		float scaled_pen_thickness = stroke.getScaledPenThickness(scale);
        addRawContent("1 J\n");   // round cap
        addRawContent("1 j\n");   // round join
        float x0 = scaled_x(stroke.position_x[0], stroke.position_y[0]);
        float y0 = scaled_y(stroke.position_x[0], stroke.position_y[0]);        
        float p0 = stroke.pressure[0];
        for (int i=1; i<stroke.N; i++) {
			if (interrupt) return;
        	float x1 = scaled_x(stroke.position_x[i], stroke.position_y[i]);
            float y1 = scaled_y(stroke.position_x[i], stroke.position_y[i]);
            float p1 = stroke.pressure[i];
            addRawContent(""+(scaled_pen_thickness*(p0+p1)/2)+" w\n");
            addRawContent(""+x0+" "+y0+" m\n");
            addRawContent(""+x1+" "+y1+" l\n");
            addRawContent("S\n");
            x0 = x1;
            y0 = y1;
            p0 = p1;
        }
	}
	
	private void addStrokePencil(Stroke stroke) {
		float scaled_pen_thickness = stroke.getScaledPenThickness(scale);
        addRawContent(""+scaled_pen_thickness+" w\n");  // line width
        addRawContent("1 J\n");   // round cap
        addRawContent("1 j\n");   // round join
        float x = scaled_x(stroke.position_x[0], stroke.position_y[0]);
        float y = scaled_y(stroke.position_x[0], stroke.position_y[0]);        
        addRawContent(""+x+" "+y+" m\n");
        for (int i=1; i<stroke.N; i++) {
			if (interrupt) return;
         	x = scaled_x(stroke.position_x[i], stroke.position_y[i]);
            y = scaled_y(stroke.position_x[i], stroke.position_y[i]);
            addRawContent(""+x+" "+y+" l\n");
        }
        addRawContent("S\n");
	}

	public void addPageNumber() {
        setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER_BOLD);
        addRawContent("0 0 0 rg\n");
        int margin = 20;
        addText(width-page_number_size-margin, height-page_number_size-margin, 
                page_number_size, ""+page_number);
        page_number++;
	}
	
	public String generateHelloWorldPDF() {
		PDFWriter mPDFWriter = new PDFWriter(PaperSize.FOLIO_WIDTH, PaperSize.FOLIO_HEIGHT);
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);
        mPDFWriter.addRawContent("1 0 0 rg\n");
        mPDFWriter.addText(70, 50, 12, "hello world");
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER, StandardFonts.WIN_ANSI_ENCODING);
        mPDFWriter.addRawContent("0 0 0 rg\n");
        mPDFWriter.addText(30, 90, 10, "� CRL", StandardFonts.DEGREES_270_ROTATION);
        mPDFWriter.newPage();
        mPDFWriter.addRawContent("[] 0 d\n");
        mPDFWriter.addRawContent("1 w\n");
        mPDFWriter.addRawContent("0 0 1 RG\n");
        mPDFWriter.addRawContent("0 1 0 rg\n");
        mPDFWriter.addRectangle(40, 50, 280, 50);
        mPDFWriter.addText(85, 75, 18, "Code Research Laboratories");
        mPDFWriter.newPage();
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER_BOLD);
        mPDFWriter.addText(150, 150, 14, "http://coderesearchlabs.com");
        mPDFWriter.addLine(150, 140, 270, 140);
        String s = mPDFWriter.asString();
        return s;
	}
	
	public void export(File file) {
		String out = asString();
        outputToFile(file, out, "ISO-8859-1");	
	}
	
	
	private void outputToFile(File file, String pdfContent, String encoding) {
		FileOutputStream pdfFile = null;
		try {
			pdfFile = new FileOutputStream(file);
			pdfFile.write(pdfContent.getBytes(encoding));
		} catch (IOException e) {
			Log.e(TAG, "Error saving PDF file: "+e.toString());
        }
		if (pdfFile == null) return;
        try {
       		pdfFile.close();
       	} catch (IOException e) {
    		Log.e(TAG, "Error closing PDF file: "+e.toString());
       	}
	}

	
	public int get_progress() {
		return (int)(100*progress);
	}
	
	public void interrupt() {
		interrupt = true;
	}	
}
