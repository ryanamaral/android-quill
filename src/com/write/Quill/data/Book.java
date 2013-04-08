package com.write.Quill.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import com.write.Quill.BookModifiedListener;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.data.TagManager.TagSet;

import name.vbraun.view.write.Page;

import junit.framework.Assert;

import android.text.format.Time;
import android.util.Log;

/**
 * Data model for "Book"
 * 
 * A book is a collection of Pages and the tag manager together with some 
 * metadata like its title. The data is stored in a fixed
 * {@link BookDirectory}.
 * 
 * @author vbraun
 *
 */
public class Book {
	private static final String TAG = "Book";
	private static final String QUILL_DATA_FILE_SUFFIX = ".quill_data";
	protected static final String INDEX_FILE = "index"+QUILL_DATA_FILE_SUFFIX;
	protected static final String PAGE_FILE_PREFIX = "page_";

	// You must set this to true if you change metadata (e.g. title)
	private boolean modified = false;
	
	private boolean isModified() {
		if (modified) return true;
		for (Page page : pages)
			if (page.isModified())
				return true;
		return false;
	}
	
	protected final TagManager tagManager = new TagManager();
	
	public TagManager getTagManager() {
		return tagManager;
	}

	public TagSet getFilter() {
		return filter;
	}

	public void setFilter(TagSet newFilter) {
		filter = newFilter;
	}

	/**
	 * dummy ctor for the {@link BookOldFormat} derived class only
	 */
	protected Book() {
		allowSave = true;
	} 
	
	public Book(String description) {
		allowSave = true;
		pages.add(new Page(tagManager));
		ctime.setToNow();
		mtime.setToNow();
		uuid = UUID.randomUUID();
		title = description;
		modified = true;
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

	// unset this to ensure that the book is never saved (truncated previews, for example)
	protected boolean allowSave = false;

	// filteredPages is never empty
	protected final LinkedList<Page> filteredPages = new LinkedList<Page>();

	public LinkedList<Page> getFilteredPages() {
		return filteredPages;
	}

	public LinkedList<Page> getPages() {
		return pages;
	}

	// mark all subsequent pages as changed so that they are saved again
	private void touchAllSubsequentPages(int fromPage) {
		for (int i = fromPage; i < pages.size(); i++)
			getPage(i).touch();
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
			if (p.isEmpty()) {
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

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
		modified = true;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public BookDirectory getDirectory() {
		Storage storage = Storage.getInstance();
		return storage.getBookDirectory(uuid);
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
		modified = true;
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
		modified = true;
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
			requestAddPage(Page.emptyWithStyleOf(page), 1);
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
			new_page = Page.emptyWithStyleOf(template);
		else
			new_page = new Page(tagManager);
		new_page.tags.add(getFilter());
		requestAddPage(new_page, position); // pages.add(position, new_page);
		removeEmptyPages();
		Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
		Assert.assertTrue("wrong page", new_page == currentPage());
		return new_page;
	}

	public Page duplicatePage() {
		Page new_page;
		Storage storage = Storage.getInstance();
		BookDirectory dir = storage.getBookDirectory(uuid);
		new_page = new Page(currentPage(), dir);
		new_page.tags.add(getFilter());
		requestAddPage(new_page, currentPage + 1); 
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
	
	// Base exception
	public static class BookIOException extends Exception {
		public BookIOException(String string) {
			super(string);
		}
		private static final long serialVersionUID = 4923229504804959444L;		
	}
	
	public static class BookLoadException extends BookIOException {
		public BookLoadException(String string) {
			super(string);
		}
		private static final long serialVersionUID = -4727997764997002754L;		
	}
	
	public static class BookSaveException extends BookIOException {
		public BookSaveException(String string) {
			super(string);
		}
		private static final long serialVersionUID = -7622965955861362254L;
	}

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

	/**
	 * Called at the end of every constructor
	 */
	protected void loadingFinishedHook() {
		makeCurrentPageConsistent();
		filterChanged();
	}
	
	////////////////////////////////////////
	/// Load and save app private data 
	/// handle failure gracefully without throwing errors

	// Loads the book. This is the complement to the save() method
	public Book(Storage storage, UUID uuid) {
		allowSave = true;
		this.uuid = uuid;
		BookDirectory dir = storage.getBookDirectory(uuid);
		try {
			doLoadBookFromDirectory(dir, -1);
		} catch (BookLoadException e) {
			storage.LogError(TAG, e.getLocalizedMessage());
		} catch (EOFException e) {
			storage.LogError(TAG, "Truncated data file");
		} catch (IOException e) {
			storage.LogError(TAG, e.getLocalizedMessage());
		}
		loadingFinishedHook();
	}
	
	// Load a truncated preview of the book
	public Book(Storage storage, UUID uuid, int pageLimit) {
		allowSave = false;
		this.uuid = uuid;
		BookDirectory dir = storage.getBookDirectory(uuid);
		try {
			doLoadBookFromDirectory(dir, pageLimit);
		} catch (BookLoadException e) {
			storage.LogError(TAG, e.getLocalizedMessage());
		} catch (EOFException e) {
			storage.LogError(TAG, "Truncated data file");
		} catch (IOException e) {
			storage.LogError(TAG, e.getLocalizedMessage());
		}
		loadingFinishedHook();
	}

	// save data internally. To load, use the constructor.
	public void save() {
		Storage storage = Storage.getInstance();
		save(storage);
	}

	// save data internally. To load, use the constructor.
	protected void save(Storage storage) {
		Assert.assertTrue(allowSave);
		BookDirectory dir = storage.getBookDirectory(getUUID());
		try {
			doSaveBookInDirectory(dir);
		} catch (BookSaveException e ) {
			storage.LogError(TAG, e.getLocalizedMessage());
		} catch (IOException e ) {
			storage.LogError(TAG, e.getLocalizedMessage());
		}
		markAsSaved();
		Bookshelf.getBookshelf().reloadPreview(this);
	}
	
	/**
	 * To be called after the book has been saved to the internal storage (but NOT: anywhere else like backups)
	 */
	private void markAsSaved() {
		modified = false;
		for (Page page : pages)
			page.markAsSaved();
	}
	
	private void doLoadBookFromDirectory(BookDirectory dir, int pageLimit) throws BookLoadException, IOException {
		if (!dir.isDirectory())
			throw new BookLoadException("No such directory: "+dir.toString());
		LinkedList<UUID> pageUUIDs = loadIndex(dir);

		// add  remaining page uuids from files in dir 
		LinkedList<UUID> pageUUIDsInDir = dir.listPages();
		pageUUIDsInDir.removeAll(pageUUIDs);
		if (!pageUUIDsInDir.isEmpty()) {
			pageUUIDs.addAll(pageUUIDsInDir);
			Storage.getInstance().LogError(TAG, "I recovered pages missing in notebook index");
		}
		
		pages.clear();
		for (UUID uuid : pageUUIDs) {
			if (pageLimit >=0 && pages.size() >= pageLimit) return;
			loadPage(uuid, dir);
		}
	}
	
	private void doSaveBookInDirectory(BookDirectory dir) throws BookSaveException, IOException {
		if (!dir.isDirectory() && !dir.mkdir())
			throw new BookSaveException("Error creating directory "+dir.toString());
		saveIndex(dir);
		LinkedList<UUID> pageUUIDsInDir = dir.listPages();
		LinkedList<UUID> blobUUIDsInDir = dir.listBlobs();
		for (Page page : getPages()) {
			pageUUIDsInDir.remove(page.getUUID());
			blobUUIDsInDir.removeAll(page.getBlobUUIDs());
			if (!page.isModified())	continue;
			savePage(page, dir);
		}

		for (UUID unused: pageUUIDsInDir) {
			File file = dir.getFile(unused);
			Log.d(TAG, "Deleteing unusued page file: "+file.toString());
			file.delete();
		}
		for (UUID unused: blobUUIDsInDir) {
			File file = dir.getFile(unused);
			Log.d(TAG, "Deleteing unusued blob file: "+file.toString());
			file.delete();
		}
	}

	
	
	/////////////////////////////
	/// implementation of load/save
	
	private LinkedList<UUID> loadIndex(File dir) throws IOException, BookLoadException {
		File indexFile = new File(dir, INDEX_FILE);
		FileInputStream fis = null;
		BufferedInputStream buffer = null;
		DataInputStream dataIn = null;
		try {
			fis = new FileInputStream(indexFile);
			buffer = new BufferedInputStream(fis);
			dataIn = new DataInputStream(buffer);
			return loadIndex(dataIn);
		} finally {
			if (dataIn != null) dataIn.close();
			else if (buffer != null) buffer.close();
			else if (fis != null) fis.close();
		}
	}

	private void saveIndex(File dir) throws IOException, BookSaveException {
		File indexFile = new File(dir, INDEX_FILE);
		FileOutputStream fos = null;
		BufferedOutputStream buffer = null;
		DataOutputStream dataOut = null;
		try {
			fos = new FileOutputStream(indexFile);
			buffer = new BufferedOutputStream(fos);
			dataOut = new DataOutputStream(buffer);
			saveIndex(dataOut);
		} finally {
			if (dataOut != null) dataOut.close();
			else if (buffer != null) buffer.close();
			else if (fos != null) fos.close();
		}			
	}

	private LinkedList<UUID> loadIndex(DataInputStream dataIn) throws IOException, BookLoadException {
		Log.d(TAG, "Loading book index");
		int n_pages;
		LinkedList<UUID> pageUuidList = null;
		int version = dataIn.readInt();
		if (version == 4) {
			n_pages = dataIn.readInt();
			pageUuidList = new LinkedList<UUID>();
			for (int i=0; i<n_pages; i++)
				pageUuidList.add(UUID.fromString(dataIn.readUTF()));
			currentPage = dataIn.readInt();
			title = dataIn.readUTF();
			ctime.set(dataIn.readLong());
			mtime.set(dataIn.readLong());
			uuid = UUID.fromString(dataIn.readUTF());
			setFilter(tagManager.loadTagSet(dataIn));
		} else 
			throw new BookLoadException("Unknown version in load_index()");
		return pageUuidList;
	}

	protected void saveIndex(DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book index");
		dataOut.writeInt(4);
		dataOut.writeInt(pages.size());
		for (int i=0; i<pages.size(); i++)
			dataOut.writeUTF(getPage(i).getUUID().toString());
		dataOut.writeInt(currentPage);
		dataOut.writeUTF(title);
		dataOut.writeLong(ctime.toMillis(false));
		if (isModified())
			mtime.setToNow();
		dataOut.writeLong(mtime.toMillis(false));
		dataOut.writeUTF(uuid.toString());
		getFilter().write_to_stream(dataOut);
	}

	private File getPageFile(File dir, UUID uuid) {
		return new File(dir, PAGE_FILE_PREFIX + uuid.toString() + QUILL_DATA_FILE_SUFFIX);
	}
	
	private void loadPage(UUID uuid, File dir) throws IOException {
		Log.d(TAG, "Loading page "+uuid);
		File file = getPageFile(dir, uuid);
		FileInputStream fis = null;
		BufferedInputStream buffer = null;
		DataInputStream dataIn = null;
		try {
			fis = new FileInputStream(file);
			buffer = new BufferedInputStream(fis);
			dataIn = new DataInputStream(buffer);
			Page page = new Page(dataIn, tagManager, dir);
			if (!page.getUUID().equals(uuid)) {
				Storage.getInstance().LogError(TAG, "Page UUID mismatch.");
				page.touch();
			}
			pages.add(page);
		} finally {
			if (dataIn != null) dataIn.close();
			else if (buffer != null) buffer.close();
			else if (fis != null) fis.close();
		}
	}

	private void savePage(Page page, File dir) throws IOException {
		File file = getPageFile(dir, page.getUUID());
		FileOutputStream fos = null;
		BufferedOutputStream buffer = null;
		DataOutputStream dataOut = null;
		try {
			fos = new FileOutputStream(file);
			buffer = new BufferedOutputStream(fos);
			dataOut = new DataOutputStream(buffer);
			savePage(page, dataOut);
		} finally {
			if (dataOut != null) dataOut.close();
			else if (buffer != null) buffer.close();
			else if (fos != null) fos.close();
		}
	}

	protected void savePage(Page page, DataOutputStream dataOut) throws IOException {
		Log.d(TAG, "Saving book page "+page.getUUID());
		page.writeToStream(dataOut);
	}

}
