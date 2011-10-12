package com.write.Quill;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.util.Log;


public class Book {
	private static final String TAG = "Book";
	private static final String FILENAME_STEM = "quill";
	// the book always has at least one page. 
	// deleting the last page is only clearing it etc.
	
	protected final LinkedList<Page> pages = new LinkedList();
	protected int currentPage = 0;
	
	private void touch_all_subsequent_pages() {
		for (int i=currentPage; i<pages.size(); i++)
			get_page(i).touch();
	}
	
	public Page get_page(int n) {
		return pages.get(n);
	}
	
	public Page current_page() {
		Log.v(TAG, "current_page() "+currentPage+"/"+pages.size());
		return pages.get(currentPage);
	}
	
	// deletes page but makes sure that there is at least one page
	public Page delete_page() {		
		Log.d(TAG, "delete_page() "+currentPage+"/"+pages.size());
		if (pages.size() == 1) {
			insert_page();
			if (pages.size() == 1) // the current page is empty
				return current_page();
			previous_page();
			pages.remove(currentPage);
		} else {
			pages.remove(currentPage);
			if (currentPage == pages.size())
				currentPage -= 1;
		}
		touch_all_subsequent_pages();
		return current_page();
	}

	public Page last_page() {
		currentPage = pages.size()-1;
		return pages.get(currentPage);
	}
	
	public Page next_page() {
		if (currentPage+1 < pages.size())
			currentPage += 1;
		return pages.get(currentPage);
	}
	
	public Page previous_page() {
		if (currentPage>0)
			currentPage -= 1;
		return pages.get(currentPage);
	}
	
	// inserts a page _if_ it makes sense
	public Page insert_page() {
		if (current_page().is_empty())
			return current_page();
		Page new_page = new Page(current_page());
		pages.add(++currentPage, new_page);
		touch_all_subsequent_pages();
		return new_page;
	}
	
	public boolean is_first_page() {
		return currentPage == 0;
	}
	
	public boolean is_last_page() {
		return currentPage+1 == pages.size();
	}
		
	public void write_to_stream(DataOutputStream out) throws IOException {
		out.writeInt(1);  // protocol #1
		out.writeInt(currentPage);
		out.writeInt(pages.size());
		ListIterator<Page> piter = pages.listIterator(); 
		while (piter.hasNext())
			piter.next().write_to_stream(out);
	}
	
	public Book(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version != 1)	
			throw new IOException("Unknown version!");
		currentPage = in.readInt();
		int N = in.readInt();
		for (int i=0; i<N; i++) {
			pages.add(new Page(in));
		}
	}
	
	private Book() {}
	
	public static Book make_empty_Book(String filename_stem) {
		Book book = new Book();
		book.pages.add(new Page());
		return book;
	}
	
	public Book(Context context) {
		int n_pages;
		try {
			n_pages = load_index(context);
		} catch (IOException e) {
			Log.e(TAG, "Error opening book index page ");
			n_pages = 1;
		}
		for (int i=0; i<n_pages; i++) {
			try {
				pages.add(load_page(i, context));
			} catch (IOException e) {
				Log.e(TAG, "Error opening book page "+i+" of "+n_pages);
			}
		}
		// recover from errors
		if (pages.isEmpty()) pages.add(new Page());
		if (currentPage <0) currentPage = 0;
		if (currentPage >= pages.size()) currentPage = pages.size() - 1;
	}
			
	public void save(Context context) {
		try {
			save_index(context);
		} catch (IOException e) {
			Log.e(TAG, "Error saving book index page ");
		}
		for (int i=0; i<pages.size(); i++) {
			if (!pages.get(i).is_modified) continue;
			try {
				save_page(i, context);
			} catch (IOException e) {
				Log.e(TAG, "Error saving book page "+i+" of "+pages.size());
			}
		}
		
	}
	
	private int load_index(Context context) throws IOException {
	    FileInputStream fis;
    	fis = context.openFileInput(FILENAME_STEM + ".index");
    	try {
    		return load_index(fis);
    	} finally {
			fis.close();
		}
	}
	
	private void save_index(Context context) throws IOException {
	    FileOutputStream fos;
    	fos = context.openFileOutput(FILENAME_STEM + ".index", Context.MODE_PRIVATE);
    	try {
    		save_index(fos);
    	} finally {
			fos.close();
		}
	}
	

	private int load_index(FileInputStream fis) throws IOException {
		Log.d(TAG, "Loading book index");
		BufferedInputStream buffer = new BufferedInputStream(fis);
		DataInputStream dataIn = new DataInputStream(buffer);
		int n_pages;
		try {
			int version = dataIn.readInt();
			if (version == 1) {
				n_pages = dataIn.readInt();
				currentPage = dataIn.readInt();
			} else
				throw new IOException("Unknown version in load_index()");				
		} finally {
			dataIn.close();
			buffer.close();
		}
		return n_pages;
	}
	
	private void save_index(FileOutputStream fos) throws IOException {
		Log.d(TAG, "Saving book index");
		BufferedOutputStream buffer = new BufferedOutputStream(fos);
		DataOutputStream dataOut = new DataOutputStream(buffer);
		try {
			dataOut.writeInt(1);
			dataOut.writeInt(pages.size());
			dataOut.writeInt(currentPage);				
		} finally {
			dataOut.close();
			buffer.close();
		}
	}

	private Page load_page(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileInputStream fis;
	    fis = context.openFileInput(filename);
    	try {
    		return load_page(i, fis);
    	} finally {
			fis.close();
		}
	}
	
	private void save_page(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileOutputStream fos;
	    fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
    	try {
    		save_page(i, fos);
    	} finally {
			fos.close();
		}
	}
 	
	private Page load_page(int i, FileInputStream fis) throws IOException {
		Log.d(TAG, "Loading book page "+i);
		BufferedInputStream buffer = new BufferedInputStream(fis);
		DataInputStream dataIn = new DataInputStream(buffer);
		try {
			return new Page(dataIn);
		} finally {
			dataIn.close();
			buffer.close();
		}
	}
	
	private void save_page(int i, FileOutputStream fos) throws IOException {
		Log.d(TAG, "Saving book page "+i);
		BufferedOutputStream buffer = new BufferedOutputStream(fos);
		DataOutputStream dataOut = new DataOutputStream(buffer);
		try {
			pages.get(i).write_to_stream(dataOut);
		} finally {
			dataOut.close();
			buffer.close();
		}
	}
	

}
