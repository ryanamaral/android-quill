package com.write.Quill;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Bookshelf {
	private static final String TAG = "Bookshelf";
	
	public class Notebook {
		private static final String TAG = "Notebook";
		
		
	}
		
	private LinkedList<Notebook> data = new LinkedList<Notebook>();
	
	private static Bookshelf instance;
	private Bookshelf() {};

	// Call this in the activity's onCreate method before accessing anything!
	public static void onCreate(Context context) {
      	if (instance == null) {
        	Log.v(TAG, "Reading notebook list from storage.");
    		instance = new Bookshelf();
        	instance.load(context);
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
		}
		return files;
	}

	
	private void load(Context context) {
		ArrayList<String> files = listNotebookFiles(context.getFilesDir());
		ListIterator<String> iter = files.listIterator();
		while (iter.hasNext()) {
			String filename = iter.next();
			Log.d(TAG, filename);
			try {
				File file = new File(filename);
				Book book = new Book();
				book.peekArchive(file);
			} catch (IOException e) {
				Log.e(TAG, "Error opening notebook "+filename);
			}	
		}
	}
	
	
}
