package com.write.Quill.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.UUID;

public class TemporaryDirectory extends DirectoryBase {
	private static final long serialVersionUID = 4388067618150641872L;
	private final static String TAG = "TempDir";
	
	public TemporaryDirectory(Storage storage, UUID uuid) {
		super(storage, Storage.TEMPORARY_DIRECTORY_PREFIX, uuid);
		mkdir();
	}

	public static LinkedList<TemporaryDirectory> allTemporaryDirectories() {
		Storage storage = Storage.getInstance();
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.startsWith(Storage.TEMPORARY_DIRECTORY_PREFIX);
		    }};
		File[] entries = storage.getFilesDir().listFiles(filter);
		LinkedList<TemporaryDirectory> dirs = new LinkedList<TemporaryDirectory>();
		if (entries == null) return dirs;
		for (File tempdir : entries) {
			String path = tempdir.getAbsolutePath();
			int pos = path.lastIndexOf(Storage.TEMPORARY_DIRECTORY_PREFIX);
			pos += Storage.TEMPORARY_DIRECTORY_PREFIX.length();
			UUID uuid = UUID.fromString(path.substring(pos));
			dirs.add(new TemporaryDirectory(storage, uuid));
		}
		return dirs;
	}
	
}
