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
import com.write.Quill.data.Book.BookLoadException;

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
			int fmt = DateUtils.FORMAT_SHOW_DATE + DateUtils.FORMAT_SHOW_TIME + 
					DateUtils.FORMAT_SHOW_YEAR + DateUtils.FORMAT_SHOW_WEEKDAY;
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
	}
	
	public static class BookPreviewComparator implements Comparator<BookPreview> {
		@Override
		public int compare(BookPreview lhs, BookPreview rhs) {
			return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
		}
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
	}

	/**
	 * Call this in the activity's onCreate method before accessing anything!
	 *
	 * @param context
	 */
	protected static void initialize(Storage storage) {
      	if (instance == null) {
        	Log.v(TAG, "Reading notebook list from storage.");
    		instance = new Bookshelf(storage);
        	currentBook = new Book(storage, storage.loadCurrentBookUUID());
    		instance.addCurrentBookToPreviews();
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
		BookPreview nb = getBookPreview(currentBook);
		Assert.assertNotNull("Book not in the preview list", nb);
		return nb;
	}
	
	/**
	 * Find the stored preview for the given book  
	 * 
	 * @param book
	 * @return The associated BookPreview or null
	 */
	public static BookPreview getBookPreview(Book book) {
		ListIterator<BookPreview> iter = data.listIterator();
		while (iter.hasNext()) {
			BookPreview nb = iter.next();
			if (nb.getUUID().equals(book.getUUID()))
				return nb;
		}
		return null;
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
	
	private File fileFromUUID(UUID uuid) {
		 String filename = "nb_"+uuid.toString()+QUILL_EXTENSION;
		 return new File(homeDirectory.getPath() + File.separator + filename);
	}
	
	private void saveBook(Book book) throws IOException {
		File file = fileFromUUID(book.uuid);
		book.saveBookArchive(file);
		BookPreview preview = getBookPreview(book);
		if (preview != null) preview.reload();
	}
	
	public void deleteBook(UUID uuid) {
		if (data.size() <= 1) {
			storage.LogError(TAG, "Cannot delete last notebook");
			return;
		}
		if (uuid.equals(currentBook.uuid)) {
			ListIterator<BookPreview> iter = data.listIterator();
			while (iter.hasNext()) {
				BookPreview nb = iter.next();
				if (nb.getUUID().equals(uuid)) 
					continue;
				setCurrentBook(nb, false);
				break;
			}
		}
		ListIterator<BookPreview> iter = data.listIterator();
		while (iter.hasNext()) {
			BookPreview nb = iter.next();
			if (nb.getUUID().equals(uuid)) { 
				boolean rc = nb.file.delete();
				if (rc == false) {
					storage.LogError(TAG, "Delete failed");
					return;		
				}
				data.remove(nb);
				return;
			}
		}
		Assert.fail("BookPreview to delete does not exist");
	}
	
//	public void save() {
//		getCurrentBook().save(context);
//		try {
//			saveBook(getCurrentBook());
//		} catch (IOException ex) {
//			Log.e(TAG, "Error saving notebook");
//			Toast.makeText(context, "Error saving notebook", 
//					Toast.LENGTH_LONG);	
//		}
//	}
	
	public void importBook(File file) throws IOException {
		saveBook(getCurrentBook());
		currentBook = new Book(file);
		saveBook(currentBook);
		addCurrentBookToPreviews();
	}
	
	public void newBook(String title) {
		try {
			saveBook(getCurrentBook());
			currentBook = new Book(title);
			saveBook(currentBook);
			addCurrentBookToPreviews();	
		} catch (IOException ex) {
			storage.LogError(TAG, "Error saving new notebook");
		}
		Assert.assertTrue(data.contains(getCurrentBookPreview()));
	}
	
	public void setCurrentBook(BookPreview nb) {
		setCurrentBook(nb, true);
	}

	public void setCurrentBook(BookPreview nb, boolean saveCurrent) {
		if (nb.getUUID().equals(currentBook.getUUID()));
		try {
			if (saveCurrent) saveBook(getCurrentBook());
			currentBook = new Book(storage, nb.uuid);
		} catch (IOException ex) {
			storage.LogError(TAG, "Error saving current notebook");
		} 
		UndoManager.getUndoManager().clearHistory();
		currentBook.setOnBookModifiedListener(UndoManager.getUndoManager());
	}
	
	
	private void addCurrentBookToPreviews() {
		UUID uuid = getCurrentBook().uuid;
		ListIterator<BookPreview> iter = data.listIterator();
		while (iter.hasNext())
			if (iter.next().preview.uuid.equals(uuid))  
				return;
		File file = fileFromUUID(uuid);
		if (!file.exists())
			try {
				saveBook(getCurrentBook());
			} catch (IOException ex) {
				Log.e(TAG, "Error saving current book");
			}
		try {
			BookPreview nb = new BookPreview(uuid);
			data.add(nb);
		} catch (BookLoadException ex) {
			storage.LogError(TAG, "Error loading notebook");
		}
	}
	
	
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
