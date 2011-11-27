package com.write.Quill;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import junit.framework.Assert;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class Bookshelf {
	private static final String TAG = "Bookshelf";
	private static final String QUILL_EXTENSION = ".quill"; 
	
	public class Notebook {
		private static final String TAG = "Notebook";
		private Book preview;
		private File file;
		private Notebook(File f) throws IOException {
			file = f;
			preview = new Book(file, 1);
		}
		public UUID getUUID() { return preview.uuid; }
		public String getTitle() { return preview.title; }
		public String getSummary() {
			int fmt = DateUtils.FORMAT_SHOW_DATE + DateUtils.FORMAT_SHOW_TIME + 
					DateUtils.FORMAT_SHOW_YEAR + DateUtils.FORMAT_SHOW_WEEKDAY;
			String s = "Created on ";
			s += DateUtils.formatDateTime(context, preview.ctime.toMillis(false), fmt) + "\n";
			s += "Last modified on ";
			s += DateUtils.formatDateTime(context, preview.mtime.toMillis(false), fmt) + "\n";
			return s;
		}
		public Bitmap getThumbnail(int width, int height) {
    		return preview.currentPage().renderBitmap(width, height);
		}
	}	
	
	private static LinkedList<Notebook> data = new LinkedList<Notebook>();
	private static Book currentBook;
	private static Bookshelf instance;
	private static File homeDirectory;
	private Context context;
	
	private Bookshelf(Context c) {
		context = c.getApplicationContext();
		homeDirectory = context.getFilesDir();
		ArrayList<String> files = listNotebookFiles(homeDirectory);
		ListIterator<String> iter = files.listIterator();
		while (iter.hasNext()) {
			String filename = iter.next();
			Log.d(TAG, filename);
			try {
				File file = new File(filename);
				Notebook notebook = new Notebook(file);
				data.add(notebook);
			} catch (IOException e) {
				Log.e(TAG, "Error opening notebook "+filename);
				Toast.makeText(context, 
						"Error loading notebook "+filename, 
						Toast.LENGTH_LONG);
			}	
		}
	}

	// Call this in the activity's onCreate method before accessing anything!
	public static void onCreate(Context context) {
      	if (instance == null) {
        	Log.v(TAG, "Reading notebook list from storage.");
    		instance = new Bookshelf(context);
        	currentBook = new Book(context);
    		instance.addCurrentBookToNotebooks();
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
	
	public static LinkedList<Notebook> getNotebookList() {
		Assert.assertNotNull(data);
		return data;
	}
	
	private File fileFromUUID(UUID uuid) {
		 String filename = "nb_"+uuid.toString()+QUILL_EXTENSION;
		 return new File(homeDirectory.getPath() + File.separator + filename);
	}
	
	private void saveBook(Book b) throws IOException {
		File file = fileFromUUID(b.uuid);
		b.saveArchive(file);
	}
	
	public void deleteBook(UUID uuid) {
		if (data.size() <= 1) {
			Toast.makeText(context, 
					"Cannot delete last notebook", Toast.LENGTH_LONG);
			return;
		}
		if (uuid.equals(currentBook.uuid)) {
			ListIterator<Notebook> iter = data.listIterator();
			while (iter.hasNext()) {
				Notebook nb = iter.next();
				if (nb.getUUID().equals(uuid)) 
					continue;
				setCurrentBook(nb);
				break;
			}
		}
		ListIterator<Notebook> iter = data.listIterator();
		while (iter.hasNext()) {
			Notebook nb = iter.next();
			if (nb.getUUID().equals(uuid)) { 
				boolean rc = nb.file.delete();
				if (rc == false) {
					Log.e(TAG, "Delete failed");
					Toast.makeText(context, 
							"Delete failed", Toast.LENGTH_LONG);
					return;		
				}
				data.remove(nb);
				return;
			}
		}
		Assert.fail("Notebook to delete does not exist");
	}
	
	public void save() {
		getCurrentBook().save(context);
		try {
			saveBook(getCurrentBook());
		} catch (IOException ex) {
			Log.e(TAG, "Error saving notebook");
			Toast.makeText(context, "Error saving notebook", 
					Toast.LENGTH_LONG);	
		}
	}
	
	public void importBook(File file) throws IOException {
		saveBook(getCurrentBook());
		currentBook = new Book(file);
		saveBook(getCurrentBook());
		addCurrentBookToNotebooks();
	}
	
	public void newBook(String title) {
		try {
			saveBook(getCurrentBook());
			currentBook = new Book(title);
			saveBook(getCurrentBook());
			addCurrentBookToNotebooks();	
		} catch (IOException ex) {
			Log.e(TAG, "Error saving notebook");
			Toast.makeText(context, "Error saving current notebook", 
					Toast.LENGTH_LONG);	
		}
	}

	public void setCurrentBook(Notebook nb) {
		try {
			saveBook(getCurrentBook());
			currentBook = new Book(nb.file);
		} catch (IOException ex) {
			Log.e(TAG, "Error loading book");
			Toast.makeText(context, "Error saving notebook", 
					Toast.LENGTH_LONG);	
		} 
	}
	
	
	private void addCurrentBookToNotebooks() {
		UUID uuid = getCurrentBook().uuid;
		ListIterator<Notebook> iter = data.listIterator();
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
			Notebook nb = new Notebook(file);
			data.add(nb);
		} catch (IOException ex) {
			Log.e(TAG, "Error loading notebook");
			Toast.makeText(context, "Error loading notebook", 
					Toast.LENGTH_LONG);	
		}
	}
	
	private ArrayList<String> listNotebookFiles(File dir) {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".quill");
		    }};
		File[] entries = dir.listFiles(filter);
		ArrayList<String> files = new ArrayList<String>();
		if (entries == null) return files;
		for (int i=0; i<entries.length; i++) {
			files.add(entries[i].getAbsolutePath());
			Log.d(TAG, "Found notebook: "+files.get(i));
		}
		return files;
	}

		
}
