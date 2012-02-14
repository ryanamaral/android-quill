package com.write.Quill.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.UUID;

import junit.framework.Assert;

/** Abstract base class for storage
 * @author vbraun
 *
 */
public abstract class Storage {
	public final static String TAG = "Storage";

	protected static Storage instance;
	
	protected Storage getInstance() {
		Assert.assertNotNull(instance);
		return instance;
	}
	
	protected void postInitializaton() {
		update();
		Bookshelf.initialize(this);
	}
	
	abstract public File getFilesDir();
	
	abstract protected UUID loadCurrentBookUUID();	
	abstract protected void saveCurrentBookUUID(UUID uuid);
	
	abstract public String formatDateTime(long millis);
	
	abstract public void LogError(String TAG, String message);

	
	
	////////////////////////////////////////////////////
	/// Obsoleted (update from old data file versions)
	
	protected void update() {
		File dir = getFilesDir();
    	// TODO

		File index = new File(dir, "quill.index");
		
		
		ArrayList<String> files = listBookFiles(dir);
	}
	
	private ArrayList<String> listBookFiles(File dir) {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".quill");
		    }};
		File[] entries = dir.listFiles(filter);
		ArrayList<String> files = new ArrayList<String>();
		if (entries == null) return files;
		for (int i=0; i<entries.length; i++) {
			files.add(entries[i].getAbsolutePath());
			LogError(TAG, "Found notebook: "+files.get(i));
		}
		return files;
	}


	
}
