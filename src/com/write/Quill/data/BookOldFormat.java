package com.write.Quill.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;


import name.vbraun.view.write.Page;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;


/**
 * Load book from the data/files/ directory (old format, obsoleted in Quill v9)
 */
public class BookOldFormat extends Book {
	private final static String TAG = "BookOldFormat";
	private static final String FILENAME_STEM = "quill";

	// Loads the book. This is the complement to the save() method
	public BookOldFormat(Context context) {
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
		
		updateFileFormat(context);
	}

	private void updateFileFormat(Context context) {
		for (int i = 0; i < pages.size(); i++)
			getPage(i).touch();  // make sure all pages request being saved
		save(context);
		// now erase old files
		File dir = context.getFilesDir();
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


	private Page loadPage(int i, DataInputStream dataIn) throws IOException {
		Log.d(TAG, "Loading book page " + i);
		return new Page(dataIn, getTagManager());
	}
	
}
