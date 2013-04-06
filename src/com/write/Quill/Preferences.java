package com.write.Quill;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.HideBar;
import name.vbraun.view.write.HandwriterView;


import com.write.Quill.R;
import com.write.Quill.data.Book.BookIOException;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Storage;
import com.write.Quill.data.StorageAndroid;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
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

	protected ListPreference penMode;
	protected ListPreference overridePen;
	protected ListPreference smoothenFilter;

	protected static final String KEY_LIST_PEN_INPUT_MODE = HandwriterView.KEY_LIST_PEN_INPUT_MODE;
	protected static final String KEY_DOUBLE_TAP_WHILE_WRITE = HandwriterView.KEY_DOUBLE_TAP_WHILE_WRITE;
	protected static final String KEY_MOVE_GESTURE_WHILE_WRITING = HandwriterView.KEY_MOVE_GESTURE_WHILE_WRITING;
	protected static final String KEY_MOVE_GESTURE_FIX_ZOOM = HandwriterView.KEY_MOVE_GESTURE_FIX_ZOOM;
	protected static final String KEY_PALM_SHIELD = HandwriterView.KEY_PALM_SHIELD;
	protected static final String KEY_BACKUP_DIR = com.write.Quill.data.StorageAndroid.KEY_BACKUP_DIR;
	protected static final String KEY_OVERRIDE_PEN_TYPE = Hardware.KEY_OVERRIDE_PEN_TYPE;
	protected static final String KEY_VOLUME_KEY_NAVIGATION = "volume_key_navigation";
	protected static final String KEY_SHOW_ACTION_BAR = "show_action_bar";
	protected static final String KEY_HIDE_SYSTEM_BAR = "hide_system_bar";
	protected static final String KEY_TOOLS_SWITCH_BACK = "some_tools_switch_back";
	protected static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
	protected static final String KEY_DEBUG_OPTIONS = HandwriterView.KEY_DEBUG_OPTIONS;
	protected static final String KEY_PEN_SMOOTH_FILTER = HandwriterView.KEY_PEN_SMOOTH_FILTER;
	
	// values for KEY_LIST_PEN_INPUT_MODE
    protected static final String STYLUS_ONLY = HandwriterView.STYLUS_ONLY;
    protected static final String STYLUS_WITH_GESTURES = HandwriterView.STYLUS_WITH_GESTURES;
    protected static final String STYLUS_AND_TOUCH = HandwriterView.STYLUS_AND_TOUCH;

    // values for KEY_OVERRIDE_PEN_TYPE
    protected static final String PEN_TYPE_AUTO = Hardware.PEN_TYPE_AUTO;
    protected static final String PEN_TYPE_CAPACITIVE = Hardware.PEN_TYPE_CAPACITIVE;
    protected static final String PEN_TYPE_SAMSUNG_NOTE = Hardware.PEN_TYPE_SAMSUNG_NOTE;
    protected static final String PEN_TYPE_THINKPAD_TABLET = Hardware.PEN_TYPE_THINKPAD_TABLET;
    protected static final String PEN_TYPE_HTC = Hardware.PEN_TYPE_HTC;
    protected static final String PEN_TYPE_ICS = Hardware.PEN_TYPE_ICS;
    
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

		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		
		penMode = (ListPreference)findPreference(KEY_LIST_PEN_INPUT_MODE);
		if (penMode.getValue() == null)
			if (Hardware.hasPenDigitizer()) 
				penMode.setValue(STYLUS_WITH_GESTURES);
			else
				penMode.setValue(STYLUS_AND_TOUCH);
		
		smoothenFilter = (ListPreference)findPreference(KEY_PEN_SMOOTH_FILTER);
		overridePen = (ListPreference)findPreference(KEY_OVERRIDE_PEN_TYPE);
		
		restorePreference = findPreference(PREFERENCE_RESTORE);
		restorePreference.setOnPreferenceClickListener(this);
	
		backupDirPreference = findPreference(PREFERENCE_BACKUP_DIR);
		backupDirPreference.setOnPreferenceClickListener(this);

		if (Global.releaseModeOEM) releaseModeOEM();
		updatePreferences();
	}
	
	private void releaseModeOEM() {
		// hide debug prefs
		PreferenceCategory debugCategory = (PreferenceCategory) findPreference("debug_preferences_category");
		debugCategory.removeAll();
		getPreferenceScreen().removePreference(debugCategory);
		
		// hide root options
		PreferenceCategory inputCategory = (PreferenceCategory) findPreference("input_preferences_category");
    	Preference hide_system_bar = findPreference(KEY_HIDE_SYSTEM_BAR);
    	inputCategory.removePreference(hide_system_bar);
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
		Context context = getApplicationContext();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean hasPenDigitizer = Hardware.hasPenDigitizer();
		
		penMode.setSummary(penMode.getEntry());
		penMode.setEnabled(hasPenDigitizer);
		
		smoothenFilter.setSummary(smoothenFilter.getEntry());
		overridePen.setSummary(overridePen.getEntry());
    	
		boolean gestures = penMode.getValue().equals(STYLUS_WITH_GESTURES);
    	findPreference(KEY_DOUBLE_TAP_WHILE_WRITE).setEnabled(gestures);
    	findPreference(KEY_MOVE_GESTURE_WHILE_WRITING).setEnabled(gestures);
    	findPreference(KEY_MOVE_GESTURE_FIX_ZOOM).setEnabled(gestures);
    	
    	boolean touch = penMode.getValue().equals(STYLUS_AND_TOUCH);
    	findPreference(KEY_PALM_SHIELD).setEnabled(touch);

    	if (!Global.releaseModeOEM) {
    		boolean hideBar = HideBar.isPossible();
    		findPreference(KEY_HIDE_SYSTEM_BAR).setEnabled(hideBar);
    	}
    	
    	Storage storage = Storage.getInstance();
		String dirName = settings.getString(KEY_BACKUP_DIR, storage.getDefaultBackupDir().getAbsolutePath());
		File dir = new File(dirName);
		if (!dir.isDirectory())
			dirName += " " + getString(R.string.preferences_err_not_a_directory);
		else if (!dir.canWrite()) 
			dirName += " " + getString(R.string.preferences_err_permissions);
		backupDirPreference.setSummary(dirName);    	
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals(KEY_OVERRIDE_PEN_TYPE)) { 
    		Context context = getApplicationContext();
    		name.vbraun.lib.pen.Hardware hw = name.vbraun.lib.pen.Hardware.getInstance(context);
    		hw.forceFromPreferences(context);
    		if (!Hardware.hasPenDigitizer())
    			penMode.setValue(STYLUS_AND_TOUCH);
    		updatePreferences();
    	}

    	if (key.equals(KEY_LIST_PEN_INPUT_MODE) || 
        	key.equals(KEY_BACKUP_DIR) ||
        	key.equals(KEY_PEN_SMOOTH_FILTER))
        	updatePreferences();
    }
    
    protected static final int REQUEST_CODE_PICK_BACKUP = 1;
    protected static final int REQUEST_CODE_PICK_BACKUP_DIRECTORY = 2;

    @Override
	public boolean onPreferenceClick(Preference preference) {
    	Log.v(TAG, "oPreferenceClick");
    	if (preference == restorePreference) {
    		Intent intent = new Intent(getApplicationContext(), name.vbraun.filepicker.FilePickerActivity.class);
    		intent.setAction("org.openintents.action.PICK_FILE");
    		intent.putExtra("org.openintents.extra.TITLE", getString(R.string.preferences_pick_backup));
    		startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP);
    		return true;
    	} else if (preference == backupDirPreference) {
    		Intent intent = new Intent(getApplicationContext(), name.vbraun.filepicker.FilePickerActivity.class);
    		intent.setAction("org.openintents.action.PICK_DIRECTORY");
    		intent.putExtra("org.openintents.extra.TITLE", getString(R.string.preferences_select_backup_folder));
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
    			Toast.makeText(this, R.string.preferences_err_loading_backup, Toast.LENGTH_LONG).show();
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
        ActivityBase.quillIncRefcount();
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        ActivityBase.quillDecRefcount();
    }

}