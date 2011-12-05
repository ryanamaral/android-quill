package com.write.Quill;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class Preferences 
	extends PreferenceActivity 
	implements OnSharedPreferenceChangeListener {
	private static final String TAG = "Preferences";

	protected static final int RESULT_RESTORE_BACKUP = 0x1234;
	protected static final String RESULT_FILENAME = "Preferences.filename";

	ListPreference penMode;
	
	protected static final String KEY_LIST_PEN_INPUT_MODE = "pen_input_mode";
	protected static final String KEY_DOUBLE_TAP_WHILE_WRITE = "double_tap_while_write";
	protected static final String KEY_MOVE_GESTURE_WHILE_WRITING = "move_gesture_while_writing";
	
    protected static final String STYLUS_ONLY = "STYLUS_ONLY";
    protected static final String STYLUS_WITH_GESTURES = "STYLUS_WITH_GESTURES";
    protected static final String STYLUS_AND_TOUCH = "STYLUS_AND_TOUCH";
	
    private boolean hasPenDigitizer;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);  
		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (ClassCastException e) {
			Log.e(TAG, e.toString());
		}

		name.vbraun.lib.pen.Hardware hw = new name.vbraun.lib.pen.Hardware(getApplicationContext());
		hasPenDigitizer = hw.hasPenDigitizer();
		
		Preference restore = findPreference("restore_backup");
		if (restore == null) {
			Log.e(TAG, "restore_backup not found");
			return;
		}
		restore.setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Log.v(TAG, "oPreferenceClick");
						showDialog(DIALOG_RESTORE_BACKUP);
						return true;
					}});

		Preference recovery = findPreference("recovery");
		recovery.setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						showDialog(DIALOG_RECOVERY);
						return true;
					}});
		penMode = (ListPreference)findPreference(KEY_LIST_PEN_INPUT_MODE);
		updatePreferences();
	}

	private void updatePreferences() {		
		penMode.setSummary(penMode.getEntry());
		penMode.setEnabled(hasPenDigitizer);
    	boolean gestures = penMode.getValue().equals(STYLUS_WITH_GESTURES);
    	findPreference(KEY_DOUBLE_TAP_WHILE_WRITE).setEnabled(gestures);
    	findPreference(KEY_MOVE_GESTURE_WHILE_WRITING).setEnabled(gestures);
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_LIST_PEN_INPUT_MODE)) {
        	updatePreferences();
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
	public static final int DIALOG_RECOVERY = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_RESTORE_BACKUP:
			return (Dialog)create_dialog_restore();
		case DIALOG_RECOVERY:
			return (Dialog)create_dialog_recovery();
		}
		return null;
	}

	
	private AlertDialog create_dialog_recovery() {
		DialogInterface.OnClickListener dialogClickListener = 
			new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int button) {
		        switch (button){
		        case DialogInterface.BUTTON_NEGATIVE:  break;
		        case DialogInterface.BUTTON_POSITIVE:
		        	Bookshelf.getBookshelf().getCurrentBook().recoverFromMissingIndex(getApplicationContext());
		        	dialog.dismiss();
		            break;
		        }
		    }};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?")
			.setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener);
		return builder.create();
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