package com.write.Quill;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.content.Context;
import android.util.Log;


public class Book {
	private static final String TAG = "Book";
	private static final String FILENAME_STEM = "quill";
	
	// the singleton instance
	private static Book book = makeEmptyBook(FILENAME_STEM);
	private Book() {}
	
	// Returns always the same instance 
	public static Book getBook() {
		return book;
	}
	
	// persistent data
	protected final LinkedList<Page> pages = new LinkedList<Page>();
	protected int currentPage = 0;
	protected TagSet filter = TagManager.newTagSet();
	
	private void touch_all_subsequent_pages() {
		for (int i=currentPage; i<pages.size(); i++)
			get_page(i).touch();
	}
	
	public Page get_page(int n) {
		return pages.get(n);
	}
	
	public TagSet getFilter() {
		return filter;
	}
	
	public boolean pageMatchesFilter(Page page) {
		ListIterator<Tag> iter = page.tags.tagIterator();
		while (iter.hasNext()) {
			Tag t = iter.next();
			if (!filter.contains(t))
				return false;
		}
		return true;
	}
	
	public Page current_page() {
		Log.v(TAG, "current_page() "+currentPage+"/"+pages.size());
		return pages.get(currentPage);
	}
	
	// deletes page but makes sure that there is at least one page
	// the book always has at least one page. 
	// deleting the last page is only clearing it etc.
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
	
	public Book loadFromStream(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version != 1)	
			throw new IOException("Unknown version!");
		currentPage = in.readInt();
		int N = in.readInt();
		pages.clear();
		for (int i=0; i<N; i++) {
			book.pages.add(new Page(in));
		}
		return book;
	}
	
	private static Book makeEmptyBook(String filename_stem) {
		Book book = new Book();
		book.pages.add(new Page());
		return book;
	}
	
	// Loads the book. This is the complement to the save() method
	public void load(Context context) {
		int n_pages;
		try {
			n_pages = load_index(context);
		} catch (IOException e) {
			Log.e(TAG, "Error opening book index page ");
			n_pages = 1;
		}
		pages.clear();
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
			
	// save data internally. Will be used automatically
	// the next time we start. To load, use the constructor.
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
	
	// Save an archive 
	public void saveArchive(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
	    BufferedOutputStream buffer;
	    DataOutputStream dataOut = null;
    	try {
    		buffer = new BufferedOutputStream(fos);
    		dataOut = new DataOutputStream(buffer);
    		save_index(dataOut);
    		for (int i=0; i<pages.size(); i++)
    			save_page(i, dataOut);
    	} finally {
    		if (dataOut != null) dataOut.close();
		}

	}
	
    // Save an archive 
    public void loadArchive(File file) throws IOException {
    	FileInputStream fis;
	    BufferedInputStream buffer;
	    DataInputStream dataIn = null;
 	  	try {
    		fis = new FileInputStream(file);
    		buffer = new BufferedInputStream(fis);
    		dataIn = new DataInputStream(buffer);
    		int n_pages = load_index(dataIn);
    		pages.clear();
    		for (int i=0; i<n_pages; i++)
    			pages.add(load_page(i, dataIn));
        } finally {
        	if (dataIn != null) dataIn.close();
    	}
        // recover from errors
        if (pages.isEmpty()) pages.add(new Page());
        if (currentPage <0) currentPage = 0;
        if (currentPage >= pages.size()) currentPage = pages.size() - 1;
		ListIterator<Page> piter = pages.listIterator(); 
		while (piter.hasNext())
			piter.next().is_modified = true;
   }
    	
    	

	
	private int load_index(Context context) throws IOException {
	    FileInputStream fis = context.openFileInput(FILENAME_STEM + ".index");
	    BufferedInputStream buffer;
	    DataInputStream dataIn = null;
    	try {
    		buffer = new BufferedInputStream(fis);
    		dataIn = new DataInputStream(buffer);
    		return load_index(dataIn);
    	} finally {
    		if (dataIn != null)	dataIn.close();
		}
	}
	
	private void save_index(Context context) throws IOException {
	    FileOutputStream fos;
    	fos = context.openFileOutput(FILENAME_STEM + ".index", Context.MODE_PRIVATE);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
    	try {
    		buffer = new BufferedOutputStream(fos);
    		dataOut = new DataOutputStream(buffer);
    		save_index(dataOut);
    	} finally {
    		if (dataOut != null) dataOut.close();
		}
	}
	
	private int load_index(DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book index");
		int n_pages;
		int version = dataIn.readInt();
		if (version == 1) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
		} else
			throw new IOException("Unknown version in load_index()");				
		return n_pages;
	}
	
	private void save_index(DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book index");
		dataOut.writeInt(1);
		dataOut.writeInt(pages.size());
		dataOut.writeInt(currentPage);				
	}

	private Page load_page(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileInputStream fis;
	    fis = context.openFileInput(filename);
	    BufferedInputStream buffer;
	    DataInputStream dataIn = null;
    	try {
    		buffer = new BufferedInputStream(fis);
    		dataIn = new DataInputStream(buffer);
    		return load_page(i, dataIn);
    	} finally {
    		if (dataIn != null) dataIn.close();
		}
	}
	
	private void save_page(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileOutputStream fos;
	    fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
    	try {
    		buffer = new BufferedOutputStream(fos);
    		dataOut = new DataOutputStream(buffer);
    		save_page(i, dataOut);
    	} finally {
    		if (dataOut != null) dataOut.close();
    	}
	}
 	
	private Page load_page(int i, DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book page "+i);
		return new Page(dataIn);
	}
	
	private void save_page(int i, DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book page "+i);
		pages.get(i).write_to_stream(dataOut);
	}
	

}
