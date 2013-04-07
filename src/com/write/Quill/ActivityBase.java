package com.write.Quill;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.HandwriterView;

import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Base class for all Quill activities
 * 
 *  - Reference counter keeps track of how many activities are running
 *  - automatic backup notebooks when no more activities are visible 
 *    (i.e. the user has navigated away)
 *  - periodically save the current notebook
 *  - Removal of deprecated preferences
 * 
 * @author vbraun
 *
 */
public class ActivityBase extends Activity {
	private final static String TAG = "ActivityBase";
	
	private static int quillActivitiesRunning = 0;
	private static Context context;
	private static final Handler backupHandler = new Handler();
	private static boolean firstRun = true;
	private static long lastUserInteraction = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
      	StorageAndroid.initialize(context);
      	if (firstRun) {
      		removeUnusedPreferences();
          	firstRun = false;
      	}
	}
	
	/* Keep track of the last user interaction so we do not save while he is doing something
	 */
	@Override
	public void onUserInteraction() {
		lastUserInteraction = System.currentTimeMillis();
		super.onUserInteraction();
	}
	
	@Override
	protected void onResume() {
		quillIncRefcount();
		backupHandler.postDelayed(automaticSave, automaticSaveDelay);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		backupHandler.removeCallbacks(automaticSave);
		quillDecRefcount();
	}
	
	public static void quillIncRefcount() {
		backupHandler.removeCallbacks(backupAtExit);
		quillActivitiesRunning += 1;		
	}
	
	public static void quillDecRefcount() {
		quillActivitiesRunning -= 1;				
	}
	
	private final static int backupDelay = 5 * 1000;
	
    @Override
    protected void onStop() {
    	super.onStop();
		Log.d(TAG, "quillActivitiesRunning = "+quillActivitiesRunning);
		if (quillActivitiesRunning == 0)
			backupHandler.postDelayed(backupAtExit, backupDelay);
    }

    private static Runnable backupAtExit = new Runnable() {
    	public void run() {
    		Bookshelf.getBookshelf().backup();
    		Toast.makeText(context, R.string.activity_base_backed_up, Toast.LENGTH_SHORT).show();
    	}
    };
    
    private final static int automaticSaveDelay = 15 * 60 * 1000;
    private final static int automaticSaveIdle = backupDelay;
    
    private static Runnable automaticSave = new Runnable() {
    	public void run() {
    		long idle = System.currentTimeMillis() - lastUserInteraction;
    		if (idle < automaticSaveIdle) {
    			backupHandler.postDelayed(automaticSave, automaticSaveIdle/3*2);
    		} else {
    			Book book = Bookshelf.getCurrentBook();
    			book.save();
    			Toast.makeText(context, R.string.activity_base_automatic_saved, Toast.LENGTH_SHORT).show();
    			backupHandler.postDelayed(automaticSave, automaticSaveDelay);
    		}
    	}
    };
    

    private void removeUnusedPreferences() {
    	final String KEY_DEBUG_OPTIONS = HandwriterView.KEY_DEBUG_OPTIONS;
    	final String KEY_HIDE_SYSTEM_BAR = Preferences.KEY_HIDE_SYSTEM_BAR;
    	final String KEY_OVERRIDE_PEN_TYPE = Hardware.KEY_OVERRIDE_PEN_TYPE;
    	final String KEY_ONLY_PEN_INPUT_OBSOLETE = "only_pen_input";

    	SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());            
        SharedPreferences.Editor editor = settings.edit();
    	
        editor.remove(KEY_ONLY_PEN_INPUT_OBSOLETE);  // obsoleted

    	if (Global.releaseModeOEM) {
    		editor.remove(KEY_HIDE_SYSTEM_BAR);
    		editor.remove(KEY_DEBUG_OPTIONS);
    		editor.remove(KEY_OVERRIDE_PEN_TYPE);
    	}
    	editor.commit();
    }
    
}
