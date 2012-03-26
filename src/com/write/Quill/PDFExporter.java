package com.write.Quill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import name.vbraun.view.write.Page;
import name.vbraun.view.write.Stroke;

import com.write.Quill.artist.ArtistPDF;
import com.write.Quill.artist.PaperType;
import com.write.Quill.data.Book;

import android.graphics.Color;
import android.util.Log;


public class PDFExporter {
	private static final String TAG = "PDFExporter";

	private ArtistPDF artist;
	
	private volatile float progress = 0;
	private volatile boolean interrupt = false;
		
	private final LinkedList<Page> pages = new LinkedList<Page>();
	
	public PDFExporter(PaperType paper, File file) {
		artist = new ArtistPDF(file);
		artist.setPaper(paper);
	}
	
	public void setBackgroundVisible(boolean visible) {
		artist.setBackgroundVisible(visible);
	}
	
	public void add(Page page) {
		pages.add(page);
	}
	
	public void add(Book book) {
		pages.addAll(book.getPages());
	}

	public void add(LinkedList<Page> page_list) {
		pages.addAll(page_list);
	}


//	public void setPageSize(PageSize newPageSize) {
//		artist.setPageSize(newPageSize);
//	}
	
	public void draw() {
		ListIterator<Page> iter = pages.listIterator();
		float len = pages.size();
		while (iter.hasNext()) {
			if (interrupt) return;
			progress = iter.nextIndex()/len;
			artist.addPage(iter.next());
		}
	}
		
	public void destroy() {
		artist.destroy();
	}
	
	public int get_progress() {
		return (int)(100*progress);
	}
	
	public void interrupt() {
		interrupt = true;
	}	
}
