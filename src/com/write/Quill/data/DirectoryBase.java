package com.write.Quill.data;

import java.io.File;
import java.util.UUID;

/**
 * Base class for directories to be handed out by the Storage class
 * @author vbraun
 *
 */
public class DirectoryBase extends File {
	private static final long serialVersionUID = 5746307953245396546L;
	private final static String TAG = "DirectoryBase";
	
	protected final String prefix;
	protected final UUID uuid;
	protected final Storage storage;
	
	public DirectoryBase(Storage storage, String prefix, UUID uuid) {
		super(storage.getFilesDir(), prefix + uuid.toString());
		this.prefix = prefix;
		this.uuid = uuid;
		this.storage = storage;
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


}
