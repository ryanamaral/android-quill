package com.write.Quill.data;

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
import java.util.UUID;

import com.write.Quill.BookModifiedListener;
import com.write.Quill.data.TagManager.TagSet;

import name.vbraun.view.write.Page;

import junit.framework.Assert;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * A book is a collection of Pages and the tag manager together with some 
 * metadata like its title.
 * 
 * @author vbraun
 *
 */
public class Book {
	private static final String TAG = "Book";
	private static final String FILENAME_STEM = "quill";

	private TagManager tagManager = new TagManager();

	public TagManager getTagManager() {
		return tagManager;
	}

	public TagSet getFilter() {
		return filter;
	}

	public void setFilter(TagSet newFilter) {
		filter = newFilter;
	}

	public Book(String description) {
		pages.add(new Page(tagManager));
		ctime.setToNow();
		mtime.setToNow();
		uuid = UUID.randomUUID();
		title = description;
		loadingFinishedHook();
	}

	// Report page changes to the undo manager
	BookModifiedListener listener;

	public void setOnBookModifiedListener(BookModifiedListener newListener) {
		listener = newListener;
	}

	// /////////////////
	// persistent data

	// the unique identifier
	protected UUID uuid;

	// pages is never empty
	protected final LinkedList<Page> pages = new LinkedList<Page>();
	private TagSet filter = tagManager.newTagSet();
	protected int currentPage = 0;

	// The title
	protected String title = "Default Quill notebook";

	// creation and last modification time
	protected Time ctime = new Time();
	protected Time mtime = new Time();

	// end of persistent data
	// ///////////////////////

	private boolean allowSave = true;

	// filteredPages is never empty
	protected final LinkedList<Page> filteredPages = new LinkedList<Page>();

	// mark all subsequent pages as changed so that they are saved again
	private void touchAllSubsequentPages(int fromPage) {
		for (int i = fromPage; i < pages.size(); i++)
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
		updateFilteredPages();
		Assert.assertTrue("current page must not change", curr == currentPage());
	}

	private void updateFilteredPages() {
		filteredPages.clear();
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			if (pageMatchesFilter(p))
				filteredPages.add(p);
		}
	}

	// remove empty pages as far as possible
	private void removeEmptyPages() {
		Page curr = currentPage();
		LinkedList<Page> empty = new LinkedList<Page>();
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			if (p == curr)
				continue;
			if (filteredPages.size() <= 1 && filteredPages.contains(p))
				continue;
			if (p.is_empty()) {
				empty.add(p);
				filteredPages.remove(p);
			}
		}
		iter = empty.listIterator();
		while (iter.hasNext()) {
			Page p = iter.next();
			requestRemovePage(p);
		}
		currentPage = pages.indexOf(curr);
		Assert.assertTrue("Current page removed?", currentPage >= 0);
	}

	// make sure the book and filteredPages is non-empty
	// call after every operation that potentially removed pages
	// the current page is not changed
	private void ensureNonEmpty(Page template) {
		Page curr = currentPage();
		Page new_page;
		if (template != null)
			new_page = new Page(template);
		else
			new_page = new Page(tagManager);
		new_page.tags.add(getFilter());
		requestAddPage(new_page, pages.size()); // pages.add(pages.size(),
												// new_page);
		setCurrentPage(curr);
		Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public Page getPage(int n) {
		return pages.get(n);
	}

	public int getPageNumber(Page page) {
		return pages.indexOf(page);
	}

	// to be called from the undo manager
	public void addPage(Page page, int position) {
		Assert.assertFalse("page already in book", pages.contains(page));
		touchAllSubsequentPages(position);
		pages.add(position, page);
		updateFilteredPages();
		currentPage = position;
	}

	// to be called from the undo manager
	public void removePage(Page page, int position) {
		Assert.assertTrue("page not in book", getPage(position) == page);
		int pos = filteredPages.indexOf(page);
		if (pos >= 0) {
			if (pos+1 < filteredPages.size()) { 
				pos = pages.indexOf(filteredPages.get(pos+1)) - 1;
			}
			else if (pos-1 >= 0)
				pos = pages.indexOf(filteredPages.get(pos-1));
			else
				pos = -1;
		}
		if (pos == -1) {
			if (position+1 < pages.size())
				pos = position + 1 - 1;
			else if (position-1 >= 0)
				pos = position - 1;
			else
				Assert.fail("Cannot create empty book");
		}
		pages.remove(position);
		updateFilteredPages();
		touchAllSubsequentPages(position);
		currentPage = pos;
		Log.d(TAG, "Removed page " + position + ", current = " + currentPage);
	}

	private void requestAddPage(Page page, int position) {
		if (listener == null)
			addPage(page, position);
		else
			listener.onPageInsertListener(page, position);
	}

	private void requestRemovePage(Page page) {
		int position = pages.indexOf(page);
		if (listener == null)
			removePage(page, position);
		else
			listener.onPageDeleteListener(page, position);
	}

	public boolean pageMatchesFilter(Page page) {
		ListIterator<TagManager.Tag> iter = getFilter().tagIterator();
		while (iter.hasNext()) {
			TagManager.Tag t = iter.next();
			if (!page.tags.contains(t)) {
				// Log.d(TAG, "does not match: "+t.name+" "+page.tags.size());
				return false;
			}
		}
		return true;
	}

	public int currentPageNumber() {
		return currentPage;
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

	public int pagesSize() {
		return pages.size();
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
	public void deletePage() {
		Log.d(TAG, "delete_page() " + currentPage + "/" + pages.size());
		Page page = currentPage();
		if (pages.size() == 1) {
			requestAddPage(new Page(page), 1);
		}
		requestRemovePage(page);
	}

	public Page lastPage() {
		Page last = filteredPages.getLast();
		currentPage = pages.indexOf(last);
		return last;
	}

	public Page lastPageUnfiltered() {
		currentPage = pages.size() - 1;
		return pages.get(currentPage);
	}

	public Page nextPage() {
		int pos = filteredPages.indexOf(currentPage());
		Page next = null;
		if (pos >= 0) {
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
		if (pos >= 0) {
			ListIterator<Page> iter = filteredPages.listIterator(pos);
			if (iter.hasPrevious())
				prev = iter.previous();
			if (prev != null)
				Log.d(TAG, "Prev " + pos + " " + pageMatchesFilter(prev));
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
		if (currentPage + 1 < pages.size())
			currentPage += 1;
		return pages.get(currentPage);
	}

	public Page previousPageUnfiltered() {
		if (currentPage > 0)
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
			new_page = new Page(tagManager);
		new_page.tags.add(getFilter());
		requestAddPage(new_page, position); // pages.add(position, new_page);
		removeEmptyPages();
		Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
		Assert.assertTrue("wrong page", new_page == currentPage());
		return new_page;
	}

	public Page insertPage() {
		return insertPage(currentPage(), currentPage + 1);
	}

	public Page insertPageAtEnd() {
		return insertPage(currentPage(), pages.size());
	}

	public boolean isFirstPage() {
		if (filteredPages.isEmpty()) return false;
		return currentPage() == filteredPages.getFirst();
	}

	public boolean isLastPage() {
		if (filteredPages.isEmpty()) return false;
		return currentPage() == filteredPages.getLast();
	}

	public boolean isFirstPageUnfiltered() {
		return currentPage == 0;
	}

	public boolean isLastPageUnfiltered() {
		return currentPage + 1 == pages.size();
	}

	// ///////////////////////////////////////////////////
	// Input/Output

	// Pick a current page if it is out of bounds
	private void makeCurrentPageConsistent() {
		if (currentPage < 0)
			currentPage = 0;
		if (currentPage >= pages.size())
			currentPage = pages.size() - 1;
		if (pages.isEmpty()) {
			Page p = new Page(tagManager);
			p.tags.add(getFilter());
			pages.add(p);
			currentPage = 0;
		}
	}

	// this is always called after the book was loaded
	private void loadingFinishedHook() {
		makeCurrentPageConsistent();
		filterChanged();
	}

	public void writePagesToStream(DataOutputStream out) throws IOException {
		out.writeInt(1); // protocol #1
		out.writeInt(currentPage);
		out.writeInt(pages.size());
		ListIterator<Page> piter = pages.listIterator();
		while (piter.hasNext())
			piter.next().writeToStream(out);
	}

	public void loadPagesFromStream(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version != 1)
			throw new IOException("Unknown version!");
		currentPage = in.readInt();
		int N = in.readInt();
		pages.clear();
		for (int i = 0; i < N; i++) {
			pages.add(new Page(in, tagManager));
		}
		loadingFinishedHook();
	}

	// Loads the book. This is the complement to the save() method
	public Book(Context context) {
		int n_pages;
		try {
			n_pages = loadIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error opening book index page ");
			Toast.makeText(context, "Page index missing, recovering...",
					Toast.LENGTH_LONG);
			recoverFromMissingIndex(context);
			return;
		}
		pages.clear();
		for (int i = 0; i < n_pages; i++) {
			try {
				pages.add(loadPage(i, context));
			} catch (IOException e) {
				Log.e(TAG, "Error loading book page " + i + " of " + n_pages);
				Toast.makeText(context, "Error loading book page " + i + " of "
						+ n_pages, Toast.LENGTH_LONG);
			}
		}
		// recover from errors
		loadingFinishedHook();
	}

	/**
	 * If the page index is missing, simply read ahead and fill in missing metadata.
	 * 
	 * @param context
	 */
	public void recoverFromMissingIndex(Context context) {
		title = "Recovered notebook";
		ctime.setToNow();
		mtime.setToNow();
		uuid = UUID.randomUUID();
		int pos = 0;
		pages.clear();
		while (true) {
			Page page;
			Log.d(TAG, "Trying to recover page " + pos);
			try {
				page = loadPage(pos++, context);
			} catch (IOException e) {
				break;
			}
			page.touch();
			pages.add(page);
		}
		// recover from errors
		loadingFinishedHook();
	}

	// save data internally. Will be used automatically
	// the next time we start. To load, use the constructor.
	public void save(Context context) {
		Assert.assertTrue(allowSave);
		try {
			saveIndex(context);
		} catch (IOException e) {
			Log.e(TAG, "Error saving book index page ");
			Toast.makeText(context, "Error saving book index page",
					Toast.LENGTH_LONG);
		}
		for (int i = 0; i < pages.size(); i++) {
			if (!pages.get(i).isModified())
				continue;
			try {
				savePage(i, context);
			} catch (IOException e) {
				Log.e(TAG,
						"Error saving book page " + i + " of " + pages.size());
				Toast.makeText(context, "Error saving book page " + i + " of "
						+ pages.size(), Toast.LENGTH_LONG);
			}
		}

	}

	
	public void saveBookArchive(File file) throws IOException {
		Assert.assertTrue(allowSave);
		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
		try {
			buffer = new BufferedOutputStream(fos);
			dataOut = new DataOutputStream(buffer);
			saveIndex(dataOut);
			for (int i = 0; i < pages.size(); i++)
				savePage(i, dataOut);
		} finally {
			if (dataOut != null)
				dataOut.close();
		}

	}

	// Load an archive; the complement to saveArchive
	public Book(File file) throws IOException {
		FileInputStream fis;
		BufferedInputStream buffer;
		DataInputStream dataIn = null;
		try {
			fis = new FileInputStream(file);
			buffer = new BufferedInputStream(fis);
			dataIn = new DataInputStream(buffer);
			int n_pages = loadIndex(dataIn);
			pages.clear();
			for (int i = 0; i < n_pages; i++)
				pages.add(loadPage(i, dataIn));
		} finally {
			if (dataIn != null)
				dataIn.close();
		}
		// recover from errors
		if (pages.isEmpty())
			pages.add(new Page(tagManager));
		if (currentPage < 0)
			currentPage = 0;
		if (currentPage >= pages.size())
			currentPage = pages.size() - 1;
		ListIterator<Page> piter = pages.listIterator();
		touchAllSubsequentPages(0);
		loadingFinishedHook();
	}

	// Peek an archive: load index data but skip pages except for the first one
	public Book(File file, int pageLimit) throws IOException {
		allowSave = false; // just to be sure that we don't save truncated data
							// back
		FileInputStream fis;
		BufferedInputStream buffer;
		DataInputStream dataIn = null;
		try {
			fis = new FileInputStream(file);
			buffer = new BufferedInputStream(fis);
			dataIn = new DataInputStream(buffer);
			int n_pages = loadIndex(dataIn);
			n_pages = Math.min(n_pages, pageLimit);
			pages.clear();
			for (int i = 0; i < n_pages; i++)
				pages.add(loadPage(i, dataIn));
		} finally {
			if (dataIn != null)
				dataIn.close();
		}
		// recover from errors
		if (pages.isEmpty())
			pages.add(new Page(tagManager));
		currentPage = 0;
		loadingFinishedHook();
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
			if (dataIn != null)
				dataIn.close();
		}
	}

	private void saveIndex(Context context) throws IOException {
		FileOutputStream fos;
		fos = context.openFileOutput(FILENAME_STEM + ".index",
				Context.MODE_PRIVATE);
		BufferedOutputStream buffer;
		DataOutputStream dataOut = null;
		try {
			buffer = new BufferedOutputStream(fos);
			dataOut = new DataOutputStream(buffer);
			saveIndex(dataOut);
		} finally {
			if (dataOut != null)
				dataOut.close();
		}
	}

	private int loadIndex(DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book index");
		int n_pages;
		int version = dataIn.readInt();
		if (version == 3) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = dataIn.readUTF();
			ctime.set(dataIn.readLong());
			mtime.set(dataIn.readLong());
			uuid = UUID.fromString(dataIn.readUTF());
			setFilter(tagManager.loadTagSet(dataIn));
		} else if (version == 2) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = "Imported Quill notebook v2";
			ctime.setToNow();
			mtime.setToNow();
			uuid = UUID.randomUUID();
			setFilter(tagManager.loadTagSet(dataIn));
		} else if (version == 1) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = "Imported Quill notebook v1";
			ctime.setToNow();
			mtime.setToNow();
			uuid = UUID.randomUUID();
			setFilter(tagManager.newTagSet());
		} else
			throw new IOException("Unknown version in load_index()");
		return n_pages;
	}

	private void saveIndex(DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book index");
		dataOut.writeInt(3);
		dataOut.writeInt(pages.size());
		dataOut.writeInt(currentPage);
		dataOut.writeUTF(title);
		dataOut.writeLong(ctime.toMillis(false));
		mtime.setToNow();
		dataOut.writeLong(mtime.toMillis(false));
		dataOut.writeUTF(uuid.toString());
		getFilter().write_to_stream(dataOut);
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
			if (dataIn != null)
				dataIn.close();
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
			if (dataOut != null)
				dataOut.close();
		}
	}

	private Page loadPage(int i, DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book page " + i);
		return new Page(dataIn, tagManager);
	}

	private void savePage(int i, DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book page " + i);
		pages.get(i).writeToStream(dataOut);
	}

	public LinkedList<Page> getFilteredPages() {
		return filteredPages;
	}

	public LinkedList<Page> getPages() {
		return pages;
	}

}
