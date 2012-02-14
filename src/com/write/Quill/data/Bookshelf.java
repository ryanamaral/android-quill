package com.write.Quill.data;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import com.write.Quill.UndoManager;
import com.write.Quill.data.Book.BookIOException;
import com.write.Quill.data.Book.BookLoadException;
import com.write.Quill.data.Book.BookSaveException;

import junit.framework.Assert;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

/**
 * The Bookshelf is a singleton holding the current Book 
 * (fully loaded data) and light-weight BookPreviews for 
 * all books.
 * 
 * @author vbraun
 *
 */
/**
 * @author vbraun
 *
 */
/**
 * @author vbraun
 *
 */
public class Bookshelf {
	private static final String TAG = "Bookshelf";
	private static final String QUILL_EXTENSION = ".quill"; 
	
	
	/**
	 * The book preview is a truncated version of the book where only the first page is loaded.
	 * 
	 * @author vbraun
	 *
	 */
	public class BookPreview {
		private static final String TAG = "BookPreview";
		private Book preview;
		private UUID uuid;
		private BookPreview(UUID uuid) {
			this.uuid = uuid;
			preview = new Book(storage, uuid, 1);
		}
		public UUID getUUID() { return preview.uuid; }
		public String getTitle() { return preview.title; }
		public String getSummary() {
			String s = "Created on ";
			s += storage.formatDateTime(preview.ctime.toMillis(false)) + "\n";
			s += "Last modified on ";
			s += storage.formatDateTime(preview.mtime.toMillis(false)) + "\n";
			return s;
		}
		public Bitmap getThumbnail(int width, int height) {
    		return preview.currentPage().renderBitmap(width, height, true);
		}
		public void reload() {
			if (uuid.equals(currentBook.uuid)) {
				preview.title = currentBook.title;
				return;
			}
			preview = new Book(storage, uuid, 1);
		}
		public void deleteFromStorage() { preview.delete(storage); }
	}
	
	public static class BookPreviewComparator implements Comparator<BookPreview> {
		@Override
		public int compare(BookPreview lhs, BookPreview rhs) {
			return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
		}
	}
	
	/** Return the preview associated with the given UUID
	 * @param uuid
	 * @return The BookPreview with matching UUID or null.
	 */
	public BookPreview getPreview(UUID uuid) {
		for (BookPreview nb : data) {
			if (nb.getUUID().equals(uuid))
				return nb;
		}
		return null;
	}

	/**
	 * Find the stored preview for the given book  
	 * 
	 * @param book
	 * @return The associated BookPreview or null
	 */
	public BookPreview getPreview(Book book) {
		return getPreview(book.getUUID());
	}

	private static LinkedList<BookPreview> data = new LinkedList<BookPreview>();
	private static Book currentBook;
	private static Bookshelf instance;
	private static File homeDirectory;
	private Storage storage;
	
	private Bookshelf(Storage storage) {
		this.storage = storage;
		homeDirectory = storage.getFilesDir();
		LinkedList<UUID> bookUUIDs = listBookUUIDs(homeDirectory);
		for (UUID uuid : bookUUIDs) {
			BookPreview notebook = new BookPreview(uuid);
			data.add(notebook);
		}
		if (data.isEmpty()) {
			currentBook = new Book("Example Notebook");
			saveBook(currentBook);
		} else {
			UUID uuid = storage.loadCurrentBookUUID();
			if (uuid == null)
				uuid = data.getFirst().getUUID();
			currentBook = new Book(storage, uuid);
		}
	}

	/** This is called automatically from the Storage initializer
	 * @param storage
	 */
	protected static void initialize(Storage storage) {
      	if (instance == null) {
        	Log.v(TAG, "Reading notebook list from storage.");
    		instance = new Bookshelf(storage);
      	}
	}
	
	public static Bookshelf getBookshelf() { 
		Assert.assertNotNull(instance);
		return instance;
	}
	
	public static Book getCurrentBook() {
		Assert.assertNotNull(currentBook);
		return currentBook;
	}
	
	public static BookPreview getCurrentBookPreview() {
		Assert.assertNotNull(currentBook);
		BookPreview nb = getBookshelf().getPreview(currentBook);
		Assert.assertNotNull("Book not in the preview list", nb);
		return nb;
	}
	

	
	public static LinkedList<BookPreview> getBookPreviewList() {
		Assert.assertNotNull(data);
		return data;
	}
	
	public static void sortBookPreviewList() {
		Assert.assertNotNull(data);
		Collections.sort(data, new BookPreviewComparator());
	}
	
	public static int getCount() {
		return data.size();
	}
	
	private void saveBook(Book book) {
		book.save(storage);
		BookPreview preview = getPreview(book);
		if (preview != null) 
			preview.reload();
		else {
			BookPreview nb = new BookPreview(book.getUUID());
			data.add(nb);		
		}
	}
	
	public void deleteBook(UUID uuid) {
		if (data.size() <= 1) {
			storage.LogMessage(TAG, "Cannot delete last notebook");
			return;
		}
		if (uuid.equals(currentBook.uuid)) {
			// switch away from the current book first
			for (BookPreview nb : data) {
				if (nb.getUUID().equals(uuid)) continue;
				setCurrentBook(nb, false);
				break;
			}
		}
		BookPreview nb = getPreview(uuid);
		if (nb == null) return;
		nb.deleteFromStorage();
		data.remove(nb);
	}
	
	public void importBook(File file) throws BookIOException {
		saveBook(getCurrentBook());
		currentBook = new Book(file);
		BookPreview nb = getPreview(currentBook.getUUID());
		if (nb != null) { // delete existing book if necessary
			nb.deleteFromStorage();
			data.remove(nb);
		}
		saveBook(currentBook);
	}
	
	public void newBook(String title) {
		saveBook(getCurrentBook());
		currentBook = new Book(title);
		saveBook(currentBook);
		Assert.assertTrue(data.contains(getCurrentBookPreview()));
	}
	
	public void setCurrentBook(BookPreview nb) {
		setCurrentBook(nb, true);
	}

	public void setCurrentBook(BookPreview nb, boolean saveCurrent) {
		if (nb.getUUID().equals(currentBook.getUUID())) return;
		if (saveCurrent) saveBook(getCurrentBook());
		currentBook = new Book(storage, nb.uuid);
		UndoManager.getUndoManager().clearHistory();
		currentBook.setOnBookModifiedListener(UndoManager.getUndoManager());
	}
	
//	private void addCurrentBookToPreviews() {
//		UUID uuid = getCurrentBook().uuid;
//		BookPreview existing = getPreview(uuid);
//		if (existing != null) return;
//		saveBook(getCurrentBook());
//		BookPreview nb = new BookPreview(uuid);
//		data.add(nb);
//	}
//	
//	
	private LinkedList<UUID> listBookUUIDs(File dir) {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.startsWith(Book.NOTEBOOK_DIRECTORY_PREFIX);
		    }};
		File[] entries = dir.listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File bookdir : entries) {
			String path = bookdir.getAbsolutePath();
			Log.d(TAG, "Found notebook: "+path);
			int pos = path.lastIndexOf(Book.NOTEBOOK_DIRECTORY_PREFIX);
			pos += Book.NOTEBOOK_DIRECTORY_PREFIX.length();
			UUID uuid = UUID.fromString(path.substring(pos));
			Log.d(TAG, "Found notebook: "+uuid);
			uuids.add(uuid);
		}
		return uuids;
	}

		
}
