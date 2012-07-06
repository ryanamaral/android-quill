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
import java.util.UUID;

import com.write.Quill.data.Storage.StorageIOException;

import junit.framework.Assert;


import name.vbraun.view.write.Page;

import android.util.Log;


/**
 * Load book from the data/files/ directory (old format, obsoleted in Quill v9)
 */
public class BookOldFormat extends Book {
	private final static String TAG = "BookOldFormat";
	private static final String FILENAME_STEM = "quill";

	// Loads the book. This is the complement to the save() method
	public BookOldFormat(Storage storage) throws BookLoadException {
		int n_pages;
		try {
			n_pages = loadIndex(storage);
		} catch (IOException e) {
			storage.LogError(TAG, "Page index missing, aborting.");
			throw new BookLoadException(e.getMessage());
		}
		pages.clear();
		for (int i = 0; i < n_pages; i++) {
			try {
				pages.add(loadPage(i, storage));
			} catch (IOException e) {
				storage.LogError(TAG, "Error loading book page " + i + " of "+ n_pages);
				throw new BookLoadException(e.getMessage());
			}
		}
		// recover from errors
		loadingFinishedHook();
		updateFileFormat(storage);
	}

	private void updateFileFormat(Storage storage) {
		for (int i = 0; i < pages.size(); i++)
			getPage(i).touch();  // make sure all pages request being saved
		save(storage);
		// now erase old files
		File dir = storage.getFilesDir();
		File index = new File(dir, FILENAME_STEM + ".index");
		index.delete();
		for (int i=0; true; i++) {
			File page_i = new File(dir, FILENAME_STEM + ".page." + i);
			boolean rc = page_i.delete();
			if (!rc) // delete failed
				break;
		}
	}
	
	/**
	 * If the page index is missing, simply read ahead and fill in missing metadata.
	 * 
	 * @param context
	 */
	public void recoverFromMissingIndex(Storage storage) {
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
				page = loadPage(pos++, storage);
			} catch (IOException e) {
				break;
			}
			page.touch();
			pages.add(page);
		}
		// recover from errors
		loadingFinishedHook();
	}

	
	
	private int loadIndex(Storage storage) throws IOException {
		FileInputStream fis = storage.openFileInput(FILENAME_STEM + ".index");
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

	private int loadIndex(DataInputStream dataIn) throws IOException {
		// Log.d(TAG, "Loading book index");
		int n_pages;
		int version = dataIn.readInt();
		if (version == 3) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = dataIn.readUTF();
			ctime.set(dataIn.readLong());
			mtime.set(dataIn.readLong());
			uuid = UUID.fromString(dataIn.readUTF());
			setFilter(getTagManager().loadTagSet(dataIn));
		} else if (version == 2) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = "Imported Quill notebook v2";
			ctime.setToNow();
			mtime.setToNow();
			uuid = UUID.randomUUID();
			setFilter(getTagManager().loadTagSet(dataIn));
		} else if (version == 1) {
			n_pages = dataIn.readInt();
			currentPage = dataIn.readInt();
			title = "Imported Quill notebook v1";
			ctime.setToNow();
			mtime.setToNow();
			uuid = UUID.randomUUID();
			setFilter(getTagManager().newTagSet());
		} else
			throw new IOException("Unknown version in load_index()");
		return n_pages;
	}

	private Page loadPage(int i, Storage storage) throws IOException {
		String filename = FILENAME_STEM + ".page." + i;
		FileInputStream fis;
		fis = storage.openFileInput(filename);
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


	private Page loadPage(int i, DataInputStream dataIn) throws IOException {
		// Log.d(TAG, "Loading book page " + i);
		return new Page(dataIn, getTagManager(), null);
	}
	
	////////////////////////////////////////
	/// Load and save archives 
	/// Throw error if necessary
	
//	public void saveBookArchive(File file) throws BookSaveException {
//		Assert.assertTrue(allowSave);
//		try {
//			doSaveBookArchive(file);
//		} catch (IOException e) {
//			throw new BookSaveException(e.getLocalizedMessage());
//		}
//	}

	// Load an archive; the complement to saveArchive
	public BookOldFormat(File file) throws BookLoadException {
		allowSave = true;
		try {
			doLoadBookArchive(file, -1);
		} catch (IOException e) {
			throw new BookLoadException(e.getLocalizedMessage());
		}
		loadingFinishedHook();
	}

	// Peek an archive: load index data but skip pages except for the first couple of pages
	public BookOldFormat(File file, int pageLimit) throws BookLoadException {
		allowSave = false; // just to be sure that we don't save truncated data back
		try {
			doLoadBookArchive(file, pageLimit);
		} catch (IOException e) {
			throw new BookLoadException(e.getLocalizedMessage());
		}
		currentPage = 0;
		loadingFinishedHook();
	}
	
	/** Internal helper to load book from archive file
	 * @param file The archive file to load
	 * @param pageLimit when to stop loading pages. Negative values mean load all pages.
	 * @throws IOException, BookLoadException
	 */
	private void doLoadBookArchive(File file, int pageLimit) throws IOException, BookLoadException {
		FileInputStream fis = null;
		BufferedInputStream buffer = null;
		DataInputStream dataIn = null;
		try {
			fis = new FileInputStream(file);
			buffer = new BufferedInputStream(fis);
			dataIn = new DataInputStream(buffer);
			pages.clear();
			int n_pages = loadIndex(dataIn);
			for (int n=0; n<n_pages; n++) {
				if (pageLimit >=0 && pages.size() >= pageLimit) return;
				Page page = new Page(dataIn, tagManager, null);
				page.touch();
				pages.add(page);
			}
		} finally {
			if (dataIn != null) dataIn.close();
			else if (buffer != null) buffer.close();
			else if (fis != null) fis.close();
		}
	}

//	private void doSaveBookArchive(File file) throws IOException, BookSaveException {
//		FileOutputStream fos = null;
//		BufferedOutputStream buffer = null;
//		DataOutputStream dataOut = null;
//		try {
//			fos = new FileOutputStream(file);
//			buffer = new BufferedOutputStream(fos);
//			dataOut = new DataOutputStream(buffer);
//			saveIndex(dataOut);
//			for (Page page : pages)
//				savePage(page, dataOut);
//		} finally {
//			if (dataOut != null) dataOut.close();
//			else if (buffer != null) buffer.close();
//			else if (fos != null) fos.close();
//		}
//	}
//

	
}
