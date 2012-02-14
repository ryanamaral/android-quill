package com.write.Quill.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;


import name.vbraun.view.write.Page;

import android.util.Log;


/**
 * Load book from the data/files/ directory (old format, obsoleted in Quill v9)
 */
public class BookOldFormat extends Book {
	private final static String TAG = "BookOldFormat";
	private static final String FILENAME_STEM = "quill";

	// Loads the book. This is the complement to the save() method
	public BookOldFormat(Storage storage) {
		int n_pages;
		try {
			n_pages = loadIndex(storage);
		} catch (IOException e) {
			storage.LogError(TAG, "Page index missing, recovering...");
			recoverFromMissingIndex(storage);
			return;
		}
		pages.clear();
		for (int i = 0; i < n_pages; i++) {
			try {
				pages.add(loadPage(i, storage));
			} catch (IOException e) {
				storage.LogError(TAG, "Error loading book page " + i + " of "+ n_pages);
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
		Log.d(TAG, "Loading book page " + i);
		return new Page(dataIn, getTagManager());
	}
	
}
