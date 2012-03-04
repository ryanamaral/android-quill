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

import junit.framework.Assert;

/** Abstract base class for storage
 * @author vbraun
 *
 */
public abstract class Storage {
	public final static String TAG = "Storage";
	protected static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";

	protected static Storage instance;
	
	protected static Storage getInstance() {
		Assert.assertNotNull(instance);
		return Storage.instance;
	}
	
	protected void postInitializaton() {
		update();
		Bookshelf.initialize(this);
	}
	
	abstract public File getFilesDir();
	
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
			LogMessage(TAG, "Found notebook: "+path);
			int pos = path.lastIndexOf(NOTEBOOK_DIRECTORY_PREFIX);
			pos += NOTEBOOK_DIRECTORY_PREFIX.length();
			UUID uuid = UUID.fromString(path.substring(pos));
			LogMessage(TAG, "Found notebook: "+uuid);
			uuids.add(uuid);
		}
		return uuids;
	}

		
	////////////////////////////////////////////////////
	/// import/export archives
	
	public UUID importArchive(File file) throws StorageIOException {
		Assert.assertNull(Bookshelf.getCurrentBook());
		File destFolder = getFilesDir();
		TarInputStream tis;
		UUID uuid = null;
		try {
			tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
			TarEntry entry;
			while((entry = tis.getNextEntry()) != null) {
				LogError(TAG, "importArchive "+entry);
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

			// Files to tar
		   File[] filesToTar=new File[2];
		   filesToTar[0]=new File("c:/test/myfile1.txt");
		   filesToTar[1]=new File("c:/test/myfile2.txt");

		   for(File f:filesToTar){
		      out.putNextEntry(new TarEntry(f, f.getName()));
		      BufferedInputStream origin = new BufferedInputStream(new FileInputStream( f ));

		      int count;
		      byte data[] = new byte[2048];
		      while((count = origin.read(data)) != -1) {
		         out.write(data, 0, count);
		      }

		      out.flush();
		      origin.close();
		   }

		   out.close();

		} catch (IOException e) {
			throw new StorageIOException(e.getMessage());
		}
	}
	
	
	////////////////////////////////////////////////////
	/// update from old data file versions
	
	protected void update() {
		File dir = getFilesDir();
    	// TODO

		File index = new File(dir, "quill.index");
		
		
		ArrayList<String> files = listBookFiles(dir);
	}
	
//	private File fileFromUUID(UUID uuid) {
//	 String filename = "nb_"+uuid.toString()+QUILL_EXTENSION;
//	 return new File(homeDirectory.getPath() + File.separator + filename);
//}

	
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
