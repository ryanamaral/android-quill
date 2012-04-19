package com.write.Quill.data;

import java.io.File;
import java.util.UUID;

import com.write.Quill.Preferences;

import junit.framework.Assert;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class StorageAndroid extends Storage {
	public final static String TAG = "StorageAndroid";
	
	private final Context context;
	private final Handler handler;
	
	public static void initialize(Context context) {
		if (instance != null) return;
		instance = new StorageAndroid(context);
		instance.postInitializaton();
	}
	
	private StorageAndroid(Context context) {
		Assert.assertNull(Storage.instance); // only construct once
		this.context = context.getApplicationContext();
		handler = new Handler();
	}
	
	public File getFilesDir() {
		return context.getFilesDir();
	}
	
	public File getExternalStorageDirectory() {
		return Environment.getExternalStorageDirectory();
	}
	
	public String formatDateTime(long millis) {
		int fmt = DateUtils.FORMAT_SHOW_DATE + DateUtils.FORMAT_SHOW_TIME + 
				DateUtils.FORMAT_SHOW_YEAR + DateUtils.FORMAT_SHOW_WEEKDAY;
		return  DateUtils.formatDateTime(context, millis, fmt);
	}


	public static final String KEY_CURRENT_BOOK_UUID = "current_book_uuid"; 
	
	protected UUID loadCurrentBookUUID() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String s = settings.getString(KEY_CURRENT_BOOK_UUID, null);
        if (s == null)
        	return null;
        else
        	return UUID.fromString(s); 
	}

	protected void saveCurrentBookUUID(UUID uuid) {
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_CURRENT_BOOK_UUID, uuid.toString());
        editor.commit();
	}

	public void LogMessage(String TAG, String message) {
		final String msg;
		if (message != null)
			msg = message;
		else
			msg = "No message details provided.";
		Log.d(TAG, msg);
		// showToast(message, Toast.LENGTH_LONG);
	}

	public void LogError(String TAG, String message) {
		final String msg;
		if (message != null)
			msg = message;
		else
			msg = "Unknown error.";		
		Log.e(TAG, msg);
		showToast(msg, Toast.LENGTH_LONG);
	}
	
	private void showToast(final String message, final int length) {
		handler.post(new Runnable() {
		    public void run() {
		        Toast.makeText(context, message, length).show();
		    }
		});
	}

	public static final String KEY_AUTO_BACKUP = "backup_automatic";
	public static final String KEY_BACKUP_DIR  = "backup_directory";
	
	/**
	 * Return the backup directory 
	 * @return File or null. The latter means it is not desired to make backups.
	 */
	public File getBackupDir() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean backup_automatic = settings.getBoolean(KEY_AUTO_BACKUP, true);
		if (!backup_automatic) return null;
		String backup_directory = settings.getString(KEY_BACKUP_DIR, getDefaultBackupDir().getAbsolutePath());
		File dir = new File(backup_directory);
		if (!dir.exists())
			dir.mkdir();
		return dir; 
	}
	
	
}
