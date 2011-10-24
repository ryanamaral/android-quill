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
	// pages is never empty
	protected final LinkedList<Page> pages = new LinkedList<Page>();
	protected TagSet filter = TagManager.newTagSet();
	protected int currentPage = 0;
	
	// filteredPages is never empty
	protected final LinkedList<Page> filteredPages = new LinkedList<Page>();

	private void touchAllSubsequentPages() {
		for (int i=currentPage; i<pages.size(); i++)
			getPage(i).touch();
	}
	
	public void filterChanged() {
		filteredPages.clear();
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			if (pageMatchesFilter(p))
				filteredPages.add(p);
		}
		if (filteredPages.isEmpty()) {
			Page curr = currentPage();
			lastPageUnfiltered();
			Page new_page = insertPage(curr);
			new_page.tags.set(filter);
			filteredPages.add(new_page);
		}
	}
	
	public Page getPage(int n) {
		return pages.get(n);
	}
	
	public TagSet getFilter() {
		return filter;
	}
	
	public boolean pageMatchesFilter(Page page) {
		ListIterator<Tag> iter = filter.tagIterator();
		while (iter.hasNext()) {
			Tag t = iter.next();
			if (!page.tags.contains(t))
				return false;
		}
		return true;
	}
	
	public Page currentPage() {
		// Log.v(TAG, "current_page() "+currentPage+"/"+pages.size());
		return pages.get(currentPage);
	}
	
	// deletes page but makes sure that there is at least one page
	// the book always has at least one page. 
	// deleting the last page is only clearing it etc.
	public Page deletePage() {		
		Log.d(TAG, "delete_page() "+currentPage+"/"+pages.size());
		if (pages.size() == 1) {
			insertPage();
			if (pages.size() == 1) // the current page is empty
				return currentPage();
			previousPage();
			pages.remove(currentPage);
		} else {
			pages.remove(currentPage);
			if (currentPage == pages.size())
				currentPage -= 1;
		}
		filterChanged();
		touchAllSubsequentPages();
		return currentPage();
	}

	public Page lastPage() {
		Page last = filteredPages.getLast();
		currentPage = pages.indexOf(last);
		return last;
	}
	
	public Page lastPageUnfiltered() {
		currentPage = pages.size()-1;
		return pages.get(currentPage);
	}
	
	public Page nextPage() {
		int pos = filteredPages.indexOf(currentPage());
		Page next = null;
		if (pos>=0) {
			ListIterator<Page> iter = filteredPages.listIterator(pos);
			iter.next(); // == currentPage()
			if (iter.hasNext())
				next = iter.next();
		} else {
			ListIterator<Page> iter = pages.listIterator(currentPage);
			iter.next(); // == currentPage()
			while (iter.hasNext()) {
				Page p = iter.next();
				if (pageMatchesFilter(p)) {
					next = p;
					break;
				}
			}
		}
		if (next == null)
			return currentPage();
		currentPage = pages.indexOf(next);
		assert currentPage >= 0;
		return next;
	}
	
	public Page previousPage() {
		int pos = filteredPages.indexOf(currentPage());
		Page prev = null;
		if (pos>=0) {
			ListIterator<Page> iter = filteredPages.listIterator(pos);
			if (iter.hasPrevious())
				prev = iter.previous();
			if (prev != null) Log.d(TAG, "Prev "+pos+" "+pageMatchesFilter(prev));
		} else {
			ListIterator<Page> iter = pages.listIterator(currentPage);
			while (iter.hasPrevious()) {
				Page p = iter.previous();
				if (pageMatchesFilter(p)) {
					prev = p;
					break;
				}
			}
		}
		if (prev == null)
			return currentPage();
		currentPage = pages.indexOf(prev);
		assert currentPage >= 0;
		return prev;
	}
	
	public Page nextPageUnfiltered() {
		if (currentPage+1 < pages.size())
			currentPage += 1;
		return pages.get(currentPage);
	}
	
	public Page previousPageUnfiltered() {
		if (currentPage>0)
			currentPage -= 1;
		return pages.get(currentPage);
	}
	
	// inserts a page _if_ it makes sense
	public Page insertPage(Page template) {
		if (currentPage().is_empty())
			return currentPage();
		Page new_page;
		new_page = new Page(template);
		pages.add(++currentPage, new_page);
		filterChanged();
		touchAllSubsequentPages();
		return new_page;
	}
	
	public Page insertPage() {
		return insertPage(currentPage());
	}
	
	public boolean isFirstPage() {
		return currentPage() == filteredPages.getFirst();
	}
	
	public boolean isLastPage() {
		return currentPage() == filteredPages.getLast();
	}
		
	public boolean isFirstPageUnfiltered() {
		return currentPage == 0;
	}
	
	public boolean isLastPageUnfiltered() {
		return currentPage+1 == pages.size();
	}

	
	public void writeToStream(DataOutputStream out) throws IOException {
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
		filterChanged();
		return book;
	}
	
	private static Book makeEmptyBook(String filename_stem) {
		Book book = new Book();
		book.pages.add(new Page());
		book.filterChanged();
		return book;
	}
	
	// Loads the book. This is the complement to the save() method
	public void load(Context context) {
		int n_pages;
		try {
			n_pages = loadIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error opening book index page ");
			n_pages = 1;
		}
		pages.clear();
		for (int i=0; i<n_pages; i++) {
			try {
				pages.add(loadPage(i, context));
			} catch (IOException e) {
				Log.e(TAG, "Error opening book page "+i+" of "+n_pages);
			}
		}
		// recover from errors
		if (pages.isEmpty()) pages.add(new Page());
		if (currentPage <0) currentPage = 0;
		if (currentPage >= pages.size()) currentPage = pages.size() - 1;
		filterChanged();
	}
			
	// save data internally. Will be used automatically
	// the next time we start. To load, use the constructor.
	public void save(Context context) {
		try {
			saveIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error saving book index page ");
		}
		for (int i=0; i<pages.size(); i++) {
			if (!pages.get(i).is_modified) continue;
			try {
				savePage(i, context);
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
    		saveIndex(dataOut);
    		for (int i=0; i<pages.size(); i++)
    			savePage(i, dataOut);
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
    		int n_pages = loadIndex(dataIn);
    		pages.clear();
    		for (int i=0; i<n_pages; i++)
    			pages.add(loadPage(i, dataIn));
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
		filterChanged();
   }
    	
    	

	
	private int loadIndex(Context context) throws IOException {
	    FileInputStream fis = context.openFileInput(FILENAME_STEM + ".index");
	    BufferedInputStream buffer;
	    DataInputStream dataIn = null;
    	try {
    		buffer = new BufferedInputStream(fis);
    		dataIn = new DataInputStream(buffer);
    		return loadIndex(dataIn);
    	} finally {
    		if (dataIn != null)	dataIn.close();
		}
	}
	
	private void saveIndex(Context context) throws IOException {
	    FileOutputStream fos;
    	fos = context.openFileOutput(FILENAME_STEM + ".index", Context.MODE_PRIVATE);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
    	try {
    		buffer = new BufferedOutputStream(fos);
    		dataOut = new DataOutputStream(buffer);
    		saveIndex(dataOut);
    	} finally {
    		if (dataOut != null) dataOut.close();
		}
	}
	
	private int loadIndex(DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book index");
		int n_pages;
		int version = dataIn.readInt();
		if (version == 2) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			filter = TagManager.loadTagSet(dataIn);
		} else if (version == 1) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			filter = TagManager.newTagSet();
		} else
			throw new IOException("Unknown version in load_index()");				
		return n_pages;
	}
	
	private void saveIndex(DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book index");
		dataOut.writeInt(2);
		dataOut.writeInt(pages.size());
		dataOut.writeInt(currentPage);
		filter.write_to_stream(dataOut);
	}

	private Page loadPage(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileInputStream fis;
	    fis = context.openFileInput(filename);
	    BufferedInputStream buffer;
	    DataInputStream dataIn = null;
    	try {
    		buffer = new BufferedInputStream(fis);
    		dataIn = new DataInputStream(buffer);
    		return loadPage(i, dataIn);
    	} finally {
    		if (dataIn != null) dataIn.close();
		}
	}
	
	private void savePage(int i, Context context) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
	    FileOutputStream fos;
	    fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
    	try {
    		buffer = new BufferedOutputStream(fos);
    		dataOut = new DataOutputStream(buffer);
    		savePage(i, dataOut);
    	} finally {
    		if (dataOut != null) dataOut.close();
    	}
	}
 	
	private Page loadPage(int i, DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book page "+i);
		return new Page(dataIn);
	}
	
	private void savePage(int i, DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book page "+i);
		pages.get(i).write_to_stream(dataOut);
	}
	

}
