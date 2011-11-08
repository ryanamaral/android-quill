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


import junit.framework.Assert;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;


public class Book {
	private static final String TAG = "Book";
	private static final String FILENAME_STEM = "quill";
	
	// the singleton instance
	private static Book book = null;
	private Book() {}
	
	// Returns always the same instance 
	public static Book getBook() {
		return book;
	}
	
	// Call this in the activity's onCreate method before accessing getBook()
	public static void onCreate(Context context) {
      	if (book == null) {
        	Log.v(TAG, "Reading book from storage.");
        	Book.load(context);
        }
	}
	
	// persistent data
	// pages is never empty
	protected final LinkedList<Page> pages = new LinkedList<Page>();
	protected TagSet filter = TagManager.newTagSet();
	protected int currentPage = 0;
	
	// filteredPages is never empty
	protected final LinkedList<Page> filteredPages = new LinkedList<Page>();

	// mark all subsequent pages as changed so that they are saved again
	private void touchAllSubsequentPages(int fromPage) {
		for (int i=fromPage; i<pages.size(); i++)
			getPage(i).touch();
	}
	
	private void touchAllSubsequentPages(Page fromPage) {
		touchAllSubsequentPages(pages.indexOf(fromPage));
	}

	private void touchAllSubsequentPages() {
		touchAllSubsequentPages(currentPage);
	}

	// Call this whenever the filter changed
	// will ensure that there is at least one page matching the filter
	// but will not change the current page (which need not match).
	public void filterChanged() {
		Page curr = currentPage();
		filteredPages.clear();
		removeEmptyPages(true);
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			if (pageMatchesFilter(p))
				filteredPages.add(p);
		}
		ensureNonEmpty(curr);
		Assert.assertTrue("current page must not change", curr == currentPage());
	}
	
	// remove empty pages
	// If keepCurrent is false and current page is empty, then 
	// currentPage is invalidated! 
	private void removeEmptyPages(boolean keepCurrent) {
		Page curr = currentPage();
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			if (keepCurrent && p == curr) continue;
			if (p.is_empty()) {
				touchAllSubsequentPages(p);
				iter.remove();
				filteredPages.remove(p);
			}
		}
		currentPage = pages.indexOf(curr);
		Assert.assertTrue("Current page removed?", !keepCurrent || currentPage>=0);
	}
		
	
	// make sure the book and filteredPages is non-empty
	// call after every operation that potentially removed pages
	// the current page is not changed
	private void ensureNonEmpty(Page template) {
		if (!filteredPages.isEmpty()) return;
		Page curr = currentPage();
		Page new_page;
		if (template != null)
			new_page = new Page(template);
		else
			new_page = new Page();
		new_page.tags.add(filter);
		pages.add(pages.size(), new_page);
		filteredPages.add(new_page);
		Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
		Assert.assertTrue("Current page removed?", curr == currentPage());
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
			if (!page.tags.contains(t)) {
				// Log.d(TAG, "does not match: "+t.name+" "+page.tags.size());
				return false;
			}
		}
		return true;
	}
	
	public Page currentPage() {
		// Log.v(TAG, "current_page() "+currentPage+"/"+pages.size());
		Assert.assertTrue(currentPage >= 0 && currentPage < pages.size());
		return pages.get(currentPage);
	}
	
	public void setCurrentPage(Page page) {
		currentPage = pages.indexOf(page);
		Assert.assertTrue(currentPage >= 0);
	}
	
	public int filteredPagesSize() {
		return filteredPages.size();
	}
	
	public Page getFilteredPage(int position) {
		return filteredPages.get(position);
	}
	
	// deletes page but makes sure that there is at least one page
	// the book always has at least one page. 
	// deleting the last page is only clearing it etc.
	public Page deletePage() {		
		Log.d(TAG, "delete_page() "+currentPage+"/"+pages.size());
		Page curr = currentPage();
		touchAllSubsequentPages();
		int initialIndex = currentPage;
		int lastIndex  = pages.indexOf(filteredPages.getLast());
		int firstIndex = pages.indexOf(filteredPages.getFirst());
		if (currentPage < firstIndex) {
			pages.remove(curr);
			currentPage = firstIndex-1;
		} else if (currentPage==firstIndex && currentPage == lastIndex) {
			pages.remove(curr);
			filteredPages.clear();
			return insertPage(curr, currentPage);
		} else if (currentPage>=firstIndex && currentPage < lastIndex) {
			nextPage();
			Assert.assertTrue(initialIndex < currentPage);
			pages.remove(curr);
			currentPage --;
		} else if (currentPage >= lastIndex) {
			previousPage();
			pages.remove(curr);
		}
		filterChanged();
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
		Assert.assertTrue(currentPage >= 0);
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
		Assert.assertTrue(currentPage >= 0);
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
	
	// inserts a page at position and makes it the current page
	// empty pages are removed
	public Page insertPage(Page template, int position) {
		Page new_page;
		if (template != null)
			new_page = new Page(template);
		else
			new_page = new Page();
		new_page.tags.add(filter);
		pages.add(position, new_page);
		currentPage = position;
		touchAllSubsequentPages();
		removeEmptyPages(true);
		filterChanged(); 
		Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
		Assert.assertTrue("wrong page", new_page == currentPage());
		return new_page;
	}
	
	public Page insertPage() {
		return insertPage(currentPage(), currentPage+1);
	}
	
	public Page insertPageAtEnd() {
		return insertPage(currentPage(), pages.size());
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

	
	/////////////////////////////////////////////////////
	// Input/Output
	
	// Pick a current page if it is out of bounds
	private void makeCurrentPageConsistent() {
		if (currentPage <0) currentPage = 0;
		if (currentPage >= pages.size()) currentPage = pages.size() - 1;
		if (pages.isEmpty()) {
			Page p = new Page();
			p.tags.add(filter);
			pages.add(p);
			currentPage = 0;
		}
	}
	
	// this is always called after the book was loaded
	private void loadingFinishedHook() {
		makeCurrentPageConsistent();
		filterChanged();
	}
	
	public void writeToStream(DataOutputStream out) throws IOException {
		out.writeInt(1);  // protocol #1
		out.writeInt(currentPage);
		out.writeInt(pages.size());
		ListIterator<Page> piter = pages.listIterator(); 
		while (piter.hasNext())
			piter.next().writeToStream(out);
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
		loadingFinishedHook();
		return book;
	}
	
	private static Book makeEmptyBook() {
		Book book = new Book();
		book.pages.add(new Page());
		Book.book = book;
		book.loadingFinishedHook();
		return getBook();
	}
	
	// Loads the book. This is the complement to the save() method
	public static Book load(Context context) {
		Book book = new Book();
		int n_pages;
		try {
			n_pages = book.loadIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error opening book index page ");
			Toast.makeText(context, 
					"Page index missing, recovering...", Toast.LENGTH_LONG);
			return recoverFromMissingIndex(context);
		}
		for (int i=0; i<n_pages; i++) {
			try {
				book.pages.add(book.loadPage(i, context));
			} catch (IOException e) {
				Log.e(TAG, "Error loading book page "+i+" of "+n_pages);
				Toast.makeText(context, 
						"Error loading book page "+i+" of "+n_pages, 
						Toast.LENGTH_LONG);
			}
		}
		// recover from errors
		book.loadingFinishedHook();
		Book.book = book;
		return getBook();
	}
			
	// Loads the book. This is the complement to the save() method
	public static Book recoverFromMissingIndex(Context context) {
		Book book = new Book();
		int pos = 0;
		while (true) {
			Page page;
			Log.d(TAG, "Trying to recover page "+pos);
			try {
				page = book.loadPage(pos++, context);
			} catch (IOException e) {
				break;
			}
			page.touch();
			book.pages.add(page);
		}
		// recover from errors
		book.loadingFinishedHook();
		Book.book = book;
		return getBook();
	}
			
	// save data internally. Will be used automatically
	// the next time we start. To load, use the constructor.
	public void save(Context context) {
		try {
			saveIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error saving book index page ");
			Toast.makeText(context, 
					"Error saving book index page", 
					Toast.LENGTH_LONG);
		}
		for (int i=0; i<pages.size(); i++) {
			if (!pages.get(i).is_modified) continue;
			try {
				savePage(i, context);
			} catch (IOException e) {
				Log.e(TAG, "Error saving book page "+i+" of "+pages.size());
				Toast.makeText(context, 
						"Error saving book page "+i+" of "+pages.size(), 
						Toast.LENGTH_LONG);
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
    public Book loadArchive(File file) throws IOException {
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
		loadingFinishedHook();
		return getBook();
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
		pages.get(i).writeToStream(dataOut);
	}
	

}
