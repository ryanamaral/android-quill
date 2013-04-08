package com.write.Quill.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import org.json.JSONException;
import org.json.JSONObject;

import com.write.Quill.UndoManager;
import com.write.Quill.data.Book.BookIOException;
import com.write.Quill.data.Book.BookLoadException;
import com.write.Quill.data.Book.BookSaveException;
import com.write.Quill.data.Storage.StorageIOException;

import junit.framework.Assert;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcelable.Creator;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * The Bookshelf is a singleton holding the current Book 
 * (fully loaded data) and light-weight BookPreviews for 
 * all books.
 * 
 * At any time there is at most one loaded book, which is the current book.
 * 
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
		@SuppressWarnings("unused")
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
		public Time getLastModifiedTime() {
			return preview.mtime;
		}
		public Bitmap getThumbnail(int width, int height) {
    		return preview.currentPage().renderBitmap(width, height, true);
		}
		public void reload() {
			preview = new Book(storage, uuid, 1);
		}
		public void deleteFromStorage() { storage.getBookDirectory(uuid).deleteAll(); }
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
	private Storage storage;
	
	private Bookshelf(Storage storage) {
		this.storage = storage;
		LinkedList<UUID> bookUUIDs = storage.listBookUUIDs();
		for (UUID uuid : bookUUIDs) {
			BookPreview notebook = new BookPreview(uuid);
			data.add(notebook);
		}
		if (!data.isEmpty()) {
			UUID uuid = storage.loadCurrentBookUUID();
			if (uuid == null)
				uuid = data.getFirst().getUUID();
			currentBook = new Book(storage, uuid);
		} else 
			currentBook = null;
	}

	private void createFirstNotebook() {
		Assert.assertNull(currentBook);
		currentBook = new Book("Example Notebook");
		currentBook.save();		
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
	
	/**
	 * The counterpart to initialize: forget any saved data. 
	 * Note: We might get re-initialized later.
	 * @param storage
	 */
	protected static void finalize(Storage storage) {
		instance = null;
	}
	
	public static Bookshelf getBookshelf() { 
		Assert.assertNotNull(instance);
		return instance;
	}
	
	public static Book getCurrentBook() {
		if (currentBook == null)
			Bookshelf.getBookshelf().createFirstNotebook();
		Assert.assertNotNull(currentBook);
		return currentBook;
	}
	
	protected static void assertNoCurrentBook() {
		Assert.assertNull(currentBook);
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
		currentBook.save();
		Collections.sort(data, new BookPreviewComparator());
	}
	
	public static int getCount() {
		return data.size();
	}
	
	protected void reloadPreview(Book book) {
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
	
	/**
	 * Import a notebook archive and make it the current book
	 * @param file A backup.quill notebook archive
	 * @throws BookIOException
	 */
	public void importBook(File file) throws BookIOException {
		BookPreview nb = getCurrentBookPreview();
		currentBook.save();
		currentBook = null;
		UUID uuid;
		try {
			uuid = storage.importArchive(file);
		} catch (StorageIOException e) {
			Log.e(TAG, "importArchive failed ("+e.getMessage()+"), trying old format.");
			try {
				uuid = storage.importOldArchive(file);
			} catch (StorageIOException dummy) {
				setCurrentBook(nb);
				throw new BookLoadException(e.getMessage());				
			}
		}
		nb = getPreview(uuid);
		if (nb != null)
			nb.reload();
		else {
			nb = new BookPreview(uuid);
			data.add(nb);
		}
		setCurrentBook(nb, false);
		currentBook.save();
		Assert.assertTrue(data.contains(nb));
	}
	
	/**
	 * Import a notebook directory.
	 * The directory is deleted after importing it.
	 * @param dir A quill notebook directory
	 * @param uuid The uuid of the notebook
	 * @throws BookIOException
	 */
	public void importBookDirectory(File dir, UUID uuid) {
		BookPreview nb = getCurrentBookPreview();
		final boolean isCurrentBook = currentBook.getUUID().equals(uuid);
		if (isCurrentBook)
			currentBook = null;
	
		File bookDir = storage.getBookDirectory(uuid);
		bookDir.mkdir();
		for (File src : dir.listFiles()) {
			File dst = new File(bookDir, src.getName());
			dst.delete();
			src.renameTo(dst);
		}
		dir.delete();
		
		nb = getPreview(uuid);
		if (nb != null)
			nb.reload();
		else {
			nb = new BookPreview(uuid);
			data.add(nb);
		}
		if (isCurrentBook)
			setCurrentBook(nb, false);
		Assert.assertTrue(data.contains(nb));
	}

	public void exportCurrentBook(File file) throws BookSaveException {
		exportBook(currentBook.getUUID(), file);
	}
	
	public void exportBook(UUID uuid, File file) throws BookSaveException {
		if (currentBook.getUUID().equals(uuid))
			currentBook.save();
		try {
			storage.exportArchive(uuid, file);
		} catch (StorageIOException e) {
			throw new BookSaveException(e.getMessage());
		}
	}
	
	public void newBook(String title) {
		getCurrentBook().save();
		currentBook = new Book(title);
		currentBook.save();
		Assert.assertTrue(data.contains(getCurrentBookPreview()));
	}
	
	public void setCurrentBook(BookPreview nb) {
		setCurrentBook(nb, true);
	}

	public void setCurrentBook(BookPreview nb, boolean saveCurrent) {
		if (currentBook != null) {
			if (saveCurrent) getCurrentBook().save();
			if (nb.getUUID().equals(currentBook.getUUID())) return;
		}
		currentBook = new Book(storage, nb.uuid);
		UndoManager.getUndoManager().clearHistory();
		currentBook.setOnBookModifiedListener(UndoManager.getUndoManager());
		storage.saveCurrentBookUUID(currentBook.getUUID());
	}
	
	
	/**
	 * Backup all notebooks
	 * @param dir
	 */
	public void backup() {
		File dir = storage.getBackupDir();
		if (dir == null) return;  // backups are disabled by user request
		backup(dir);
	}
		
	/**
	 * Backup all notebooks. Does not overwrite backup files that have the same or newer modification time.
	 * @param dir The directory to save the backups in
	 */
	public void backup(File dir) {
		for (BookPreview nb : getBookPreviewList()) {
			UUID uuid = nb.getUUID();
			File file = new File(dir, uuid.toString() + QUILL_EXTENSION);
			File index = new File(storage.getBookDirectory(uuid), Book.INDEX_FILE);
			if (file.exists() && file.lastModified() >= index.lastModified()) continue;
			try {
				exportBook(uuid, file);
			} catch (BookSaveException e) {
				storage.LogError(TAG, e.getLocalizedMessage());
			}
			backupDescription(dir);
		}
	}
	
	/**
	 * Save the backup file names and corresponding notebook titles in a JSON file
	 * @param dir
	 */
	public void backupDescription(File dir) {
		JSONObject json = new JSONObject();
		for (BookPreview nb : getBookPreviewList())
			try {
				json.put(nb.getUUID().toString(), nb.getTitle());
			} catch (JSONException e) {
				storage.LogError(TAG, "cannot create json file");
			}		
		File indexFile = new File(dir, "description.json");
		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter(indexFile));
			output.write(json.toString());
			output.close();
		} catch (IOException e) {
			storage.LogError(TAG, "cannot save json file");
		}
	}
}
