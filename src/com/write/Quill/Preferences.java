package com.write.Quill;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import name.vbraun.lib.pen.HideBar;


import com.write.Quill.R;
import com.write.Quill.data.Book.BookIOException;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class Preferences 
	extends PreferenceActivity 
	implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {
	private static final String TAG = "Preferences";

	protected final static String PREFERENCE_RESTORE = "restore_backup";
	protected final static String PREFERENCE_BACKUP_DIR = "backup_directory";
	
	protected static final int RESULT_RESTORE_BACKUP = 0x1234;
	protected static final String RESULT_FILENAME = "Preferences.filename";

	ListPreference penMode;
	
	protected static final String KEY_LIST_PEN_INPUT_MODE = "pen_input_mode";
	protected static final String KEY_DOUBLE_TAP_WHILE_WRITE = "double_tap_while_write";
	protected static final String KEY_MOVE_GESTURE_WHILE_WRITING = "move_gesture_while_writing";
	protected static final String KEY_PALM_SHIELD = "palm_shield";
	protected static final String KEY_HIDE_SYSTEM_BAR = "hide_system_bar";
	protected static final String KEY_BACKUP_DIR = "backup_directory";

    protected static final String STYLUS_ONLY = "STYLUS_ONLY";
    protected static final String STYLUS_WITH_GESTURES = "STYLUS_WITH_GESTURES";
    protected static final String STYLUS_AND_TOUCH = "STYLUS_AND_TOUCH";
	
    private boolean hasPenDigitizer;
    
    private Preference restorePreference;
    private Preference backupDirPreference;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
      	StorageAndroid.initialize(getApplicationContext());

		super.onCreate(savedInstanceState);  
		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (ClassCastException e) {
			Log.e(TAG, e.toString());
		}
		
		name.vbraun.lib.pen.Hardware hw = new name.vbraun.lib.pen.Hardware(getApplicationContext());
		hasPenDigitizer = hw.hasPenDigitizer();
		penMode = (ListPreference)findPreference(KEY_LIST_PEN_INPUT_MODE);
		
		restorePreference = findPreference(PREFERENCE_RESTORE);
		restorePreference.setOnPreferenceClickListener(this);
	
		backupDirPreference = findPreference(PREFERENCE_BACKUP_DIR);
		backupDirPreference.setOnPreferenceClickListener(this);

		updatePreferences();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		   switch (item.getItemId()) 
		   {        
		      case android.R.id.home:            
		    	  finish();
		    	  return true;  
		      default:            
		         return super.onOptionsItemSelected(item);    
		   }
	}
	
	private void updatePreferences() {		
		penMode.setSummary(penMode.getEntry());
		penMode.setEnabled(hasPenDigitizer);
    	
		boolean gestures = penMode.getValue().equals(STYLUS_WITH_GESTURES);
    	findPreference(KEY_DOUBLE_TAP_WHILE_WRITE).setEnabled(gestures);
    	findPreference(KEY_MOVE_GESTURE_WHILE_WRITING).setEnabled(gestures);
    	
    	boolean touch = penMode.getValue().equals(STYLUS_AND_TOUCH);
    	findPreference(KEY_PALM_SHIELD).setEnabled(touch);
    
    	boolean hideBar = HideBar.isPossible();
    	findPreference(KEY_HIDE_SYSTEM_BAR).setEnabled(hideBar);
    	
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String dirName = settings.getString(KEY_BACKUP_DIR, "/mnt/sdcard/Quill");
		File dir = new File(dirName);
		if (!dir.isDirectory())
			dirName += " (Error: not a directory)";
		else if (!dir.canWrite()) 
			dirName += " (Error: no write permissions)";
		backupDirPreference.setSummary(dirName);    	
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	Log.e(TAG, "onSharedPreferenceChanged");
        if (key.equals(KEY_LIST_PEN_INPUT_MODE) || key.equals(KEY_BACKUP_DIR)) 
        	updatePreferences();
    }
    
    protected static final int REQUEST_CODE_PICK_BACKUP = 1;
    protected static final int REQUEST_CODE_PICK_BACKUP_DIRECTORY = 2;

    @Override
	public boolean onPreferenceClick(Preference preference) {
    	Log.v(TAG, "oPreferenceClick");
    	if (preference == restorePreference) {
    		Intent intent = new Intent("org.openintents.action.PICK_FILE");
    		intent.putExtra("org.openintents.extra.TITLE", "Pick a backup to restore");
    		startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP);
    		return true;
    	} else if (preference == backupDirPreference) {
    		Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
    		intent.putExtra("org.openintents.extra.TITLE", "Please select a backup folder");
    		startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP_DIRECTORY);
    		return true;
    	}	
    	return false;
    }
    
    private String filenameFromActivityResult(int resultCode, Intent data) {
		if (resultCode != RESULT_OK || data == null) return null; 
		Uri fileUri = data.getData();
		if (fileUri == null) return null;
		return fileUri.getPath();

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode) {
    	case REQUEST_CODE_PICK_BACKUP:
    		String fileName = filenameFromActivityResult(resultCode, data);
    		if (fileName == null) return;
    		
    		Bookshelf bookshelf = Bookshelf.getBookshelf();
    		try {
    			bookshelf.importBook(new File(fileName));
    			finish();
    		} catch (BookIOException e) {
    			Log.e(TAG, "Error loading the backup file.");
    			Toast.makeText(this, "Error loading the backup file.", Toast.LENGTH_LONG).show();
    			return;
    		}
    		break;
    	case REQUEST_CODE_PICK_BACKUP_DIRECTORY:
    		String dirName = filenameFromActivityResult(resultCode, data);
    		if (dirName == null) return;
            SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());            
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(KEY_BACKUP_DIR, dirName);
            editor.commit();
            updatePreferences();
    		break;
    	}
    }
	    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }

	public static final int DIALOG_RESTORE_BACKUP = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_RESTORE_BACKUP:
			return (Dialog)create_dialog_restore();
		}
		return null;
	}

		
	private ArrayList<String> tryMakeFileList() {
		ArrayList<String> files = new ArrayList<String>();
		files.addAll(tryMakeFileList(getExternalFilesDir(null).getPath()));
		files.addAll(tryMakeFileList("/mnt/sdcard"));
		files.addAll(tryMakeFileList("/mnt/external_sd"));
		files.addAll(tryMakeFileList("/mnt/usbdrive"));
		return files;
	}

	private ArrayList<String> tryMakeFileList(String directory) {
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".quill");
		    }};
//		String[] entries = dir.listFiles(filter);
		File[] entries = dir.listFiles();
		ArrayList<String> files = new ArrayList<String>();
		if (entries == null) return files;
		for (int i=0; i<entries.length; i++) {
			files.add(entries[i].getAbsolutePath());
		}
		return files;
	}

	private Dialog create_dialog_restore() {
		final ArrayList<String> files = new ArrayList<String>(); 
		files.clear();
		files.addAll(tryMakeFileList());
		CharSequence[] items = new CharSequence[files.size()];
		files.toArray(items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (files.size() == 0) 
			builder.setTitle("No backups found!");
		else
			builder.setTitle("Backup to restore:");
		builder.setItems(items, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (item<0) {
							dialog.dismiss();
							return;
						}
						dialog.dismiss();						
						setResult(RESULT_RESTORE_BACKUP);
						Intent resultIntent = new Intent();
						resultIntent.putExtra(RESULT_FILENAME, files.get(item));
						setResult(RESULT_RESTORE_BACKUP, resultIntent);
						finish();
			}});
		return builder.create();
	}


}