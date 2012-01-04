package com.write.Quill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import name.vbraun.view.write.Page;
import name.vbraun.view.write.Stroke;
import name.vbraun.view.write.ScalePDF;

import org.libharu.Font;
import org.libharu.Font.BuiltinFont;
import org.libharu.Page.LineCap;
import org.libharu.Page.LineJoin;
import org.libharu.Page.PageDirection;
import org.libharu.Page.PageSize;

import com.write.Quill.data.Book;

import android.graphics.Color;
import android.util.Log;


public class PDFExporter 
	extends org.libharu.Document 
	implements ScalePDF {

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
		pages.addAll(book.getPages());
	}

	public void add(LinkedList<Page> page_list) {
		pages.addAll(page_list);
	}


	public void setPageSize(PageSize newPageSize) {
		pageSize = newPageSize;
	}
	
	public float scaledX(float x, float y) {
		if (rotate)
			return y * scale;
		else
			return x * scale;
	}
	
	public float scaledY(float x, float y) {
		if (rotate)
			return x * scale;
		else
			return (1-y) * scale;
	}

	public float getScale() {
		return scale;
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
		boolean page_is_portrait = (page.getAspectRatio() < 1);
		pdf = addPage();
		if (page_is_portrait)	{
			pdf.setSize(pageSize, PageDirection.PORTRAIT);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.getAspectRatio());
		} else {
			pdf.setSize(pageSize, PageDirection.LANDSCAPE);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.getAspectRatio());
		}
		rotate = false;
		ListIterator<Stroke> siter = page.strokes.listIterator();
		while (siter.hasNext()) {
			if (interrupt) return;
			siter.next().render(pdf, this);
		}
		addPageNumber();
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
