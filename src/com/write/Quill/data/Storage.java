package com.write.Quill.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;
import org.xeustechnologies.jtar.TarOutputStream;

import com.write.Quill.data.Book.BookLoadException;

import junit.framework.Assert;

/** Abstract base class for storage
 * @author vbraun
 *
 */
public abstract class Storage {
	public final static String TAG = "Storage";
	protected static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";

	protected static Storage instance;
	
	public static Storage getInstance() {
		Assert.assertNotNull(instance);
		return Storage.instance;
	}
	
	/**
	 * Hook that runs after the instance singleton is initialized
	 */
	protected void postInitializaton() {
		Bookshelf.initialize(this);
	}
	
	/**
	 * Hook that runs right before the instance singleton is destroyed
	 */
	protected void preDestroy() {
		Bookshelf.finalize(this);
	}

	/**
	 * Clear the singleton. You need to initialize storage before you can use it again.
	 */
	public void destroy() {
		preDestroy();
		instance = null;
	}
	
	/**
	 * Get the directory containing the data files. Usually /data/data/com.write.Quill/files
	 * @return a directory
	 */
	abstract public File getFilesDir();
	
	/**
	 * Usually this is /mnt/sdcard
	 * @return
	 */
	abstract public File getExternalStorageDirectory();
	
	public File getDefaultBackupDir() {
		return new File(getExternalStorageDirectory(), "Quill");
	}
	
	abstract public File getBackupDir();


	public static class StorageIOException extends IOException {
		public StorageIOException(String string) {
			super(string);
		}
		private static final long serialVersionUID = 435361903521711631L;
	}
	
	
	public FileInputStream openFileInput(String filename) throws StorageIOException {
		File file = new File(getFilesDir(), filename);
		try {
			return new FileInputStream(file);
		} catch (IOException e) {
			throw new StorageIOException(e.getMessage());
		}
	}
	
	abstract protected UUID loadCurrentBookUUID();	
	abstract protected void saveCurrentBookUUID(UUID uuid);
	
	abstract public String formatDateTime(long millis);
	
	abstract public void LogMessage(String TAG, String message);
	abstract public void LogError(String TAG, String message);
	
	////////////////////////////////////////////////////
	/// Book directory handlings

	public String getBookDirectoryName(UUID uuid) {
		return NOTEBOOK_DIRECTORY_PREFIX + uuid.toString();
	}	
	
	protected UUID getBookUUIDfromDirectoryName(String name) {
		if (!name.startsWith(NOTEBOOK_DIRECTORY_PREFIX)) return null;
		int n = NOTEBOOK_DIRECTORY_PREFIX.length();
		String uuid = name.substring(n, n+36);
		LogError(TAG, "UUID = " + uuid);
		return UUID.fromString(uuid);
	}
	
	public File getBookDirectory(UUID uuid) {
		String dirname = getBookDirectoryName(uuid);
		return new File(getFilesDir(), dirname);

	}
	
	public void deleteBookDirectory(UUID uuid) {
		File dir = getBookDirectory(uuid);
		deleteDirectory(dir);
	}
	
	private void deleteDirectory(File dir) {
        String[] children = dir.list();
        for (String child : children) {
        	File file = new File(dir, child);
        	file.delete();
        }
        boolean rc = dir.delete();
        if (!rc) 
        	LogError(TAG, "Unable to delete directory "+dir.toString());		
	}


	public LinkedList<UUID> listBookUUIDs() {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.startsWith(NOTEBOOK_DIRECTORY_PREFIX);
		    }};
		File[] entries = getFilesDir().listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File bookdir : entries) {
			String path = bookdir.getAbsolutePath();
			int pos = path.lastIndexOf(NOTEBOOK_DIRECTORY_PREFIX);
			pos += NOTEBOOK_DIRECTORY_PREFIX.length();
			UUID uuid = UUID.fromString(path.substring(pos));
			uuids.add(uuid);
		}
		return uuids;
	}

		
	////////////////////////////////////////////////////
	/// import/export archives
	
	public UUID importArchive(File file) throws StorageIOException {
		Bookshelf.assertNoCurrentBook();
		File destFolder = getFilesDir();
		TarInputStream tis;
		UUID uuid = null;
		try {
			tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
			TarEntry entry;
			while((entry = tis.getNextEntry()) != null) {
				// LogMessage(TAG, "importArchive "+entry);
				if (uuid == null)
					uuid = getBookUUIDfromDirectoryName(entry.getName());
				else if (!uuid.equals(getBookUUIDfromDirectoryName(entry.getName())))
					throw new StorageIOException("Incorrect book archive file");
				int count;
				byte data[] = new byte[2048];
				FileOutputStream fos = new FileOutputStream(destFolder + "/" + entry.getName());
				BufferedOutputStream dest = new BufferedOutputStream(fos);
				while((count = tis.read(data)) != -1) {
					dest.write(data, 0, count);
				}	
				dest.flush();
				dest.close();
			}	
		   	
			tis.close();
		} catch (IOException e) {
			throw new StorageIOException(e.getMessage());
		}
		if (uuid == null)
			throw new StorageIOException("No ID in book archive file.");
		return uuid;
	}
	
	public void exportArchive(UUID uuid, File dest) throws StorageIOException {
		try {
			TarOutputStream out = new TarOutputStream( new BufferedOutputStream( new FileOutputStream(dest)));

			File dir = getBookDirectory(uuid);
			File[] filesToTar = dir.listFiles();
			int count;
			byte data[] = new byte[2048];
			for(File f:filesToTar){
				// LogMessage(TAG, "exportiArchive "+f);
				String name = dir.getName() + File.separator + f.getName();
				out.putNextEntry(new TarEntry(f, name));
				BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));
				while((count = origin.read(data)) != -1)
					out.write(data, 0, count);
		      out.flush();
		      origin.close();
		   }
		   out.close();
		} catch (IOException e) {
			throw new StorageIOException(e.getMessage());
		}
	}
	
	////////////////////////////////////////////////////
	/// import old version
	
	public UUID importOldArchive(File file) throws StorageIOException {
		Bookshelf.assertNoCurrentBook();
		try {
			Book book = new BookOldFormat(file);
			book.save(this);
			return book.getUUID();
		} catch (BookLoadException e) {
			throw new StorageIOException(e.getMessage());
		}
	}

	
	
	////////////////////////////////////////////////////
	/// update from old data file versions
	
	public boolean needUpdate() {
		File index = new File(getFilesDir(), "quill.index");
		return index.exists();
	}
	
	public void update() throws BookLoadException  {
		File dir = getFilesDir();
		Book book = new BookOldFormat(this);
		UUID currentUuid = book.getUUID();
		ArrayList<String> files = listBookFiles(dir);
		for (String name : files) {
			File file = new File(name);
			book = new BookOldFormat(file);
			if (!book.getUUID().equals(currentUuid))
				book.save(this);
		}
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
		}
		return files;
	}

	
	

	
	


	
}
