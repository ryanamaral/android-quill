package com.write.Quill.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.data.Book.BookIOException;

import android.util.Log;

public class BookDirectory extends File {
	private static final long serialVersionUID = -3424988863197267971L;
	private final static String TAG = "BookDir";
	
	private final UUID uuid;
	private final Storage storage;
	
	public BookDirectory(Storage storage, UUID uuid) {
		super(storage.getFilesDir(), getName(uuid));
		this.uuid = uuid;
		this.storage = storage;
	}
	
	private static String getName(UUID uuid) {
		return Storage.NOTEBOOK_DIRECTORY_PREFIX + uuid.toString();
	}	
	
	public UUID getUUID() {
		return uuid;
	}
	
	protected void deleteAll() {
		String[] children = list();
		for (String child : children) {
			File file = new File(this, child);
			file.delete();
		}
		boolean rc = delete();
		if (!rc) 
			storage.LogError(TAG, "Unable to delete directory "+toString());		
	}

	private FilenameFilter getPrefixFilter(final String prefix) {
		return new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return name.startsWith(prefix);
		    }};
	}
	
	protected LinkedList<UUID> listPages() {
		FilenameFilter filter = getPrefixFilter(Book.PAGE_FILE_PREFIX); 
		File[] entries = listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File page : entries) {
			String path = page.getAbsolutePath();
			int pos = path.lastIndexOf(Book.PAGE_FILE_PREFIX);
			pos += Book.PAGE_FILE_PREFIX.length();
			try {
				UUID uuid = UUID.fromString(path.substring(pos, pos+36));
				Log.d(TAG, "Found page: "+uuid);
				uuids.add(uuid);
			} catch (StringIndexOutOfBoundsException e) {
				page.delete();
				Log.e(TAG, "Malformed file name: "+uuid);
			}
		}
		return uuids;
	}
	
	/**
	 * List everything that is not Page and index data
	 */
	protected LinkedList<UUID> listBlobs() {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return !name.startsWith(Book.PAGE_FILE_PREFIX) && 
		        		!name.startsWith(Book.INDEX_FILE);
		    }}; 
		File[] entries = listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File blob : entries) {
			String path = blob.getName();
			try {
				UUID uuid = UUID.fromString(path.substring(0, 36));
				Log.d(TAG, "Found blob: "+uuid);
				uuids.add(uuid);
			} catch (StringIndexOutOfBoundsException e) {
				blob.delete();
				Log.e(TAG, "Malformed file name: "+uuid);
			}
		}
		return uuids;
	}

	/**
	 * Return the file with given UUID
	 * @param uuid
	 * @return A File object or null if it does not exist
	 */
	protected File getFile(UUID uuid) {
		final String uuidStr = uuid.toString();
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return name.contains(uuidStr);
		    }};
		File[] entries = listFiles(filter);
		if (entries.length != 1)
			Log.e(TAG, "getFile() found "+entries.toString());
		if (entries.length < 1) 
			return null;
		return entries[0];
	}
	
}


