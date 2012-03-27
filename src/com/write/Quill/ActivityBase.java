package com.write.Quill;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.HandwriterView;

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
 * It keeps track of how many activities are running, and will backup notebooks when 
 * no more activities are visible (i.e. the user has navigated away)
 * @author vbraun
 *
 */
public class ActivityBase extends Activity {
	private final static String TAG = "ActivityBase";
	
	private static int quillActivitiesRunning = 0;
	private static Context context;
	private static final Handler backupHandler = new Handler();
	private static boolean firstRun = true;
	
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
	
	@Override
	protected void onResume() {
		quillIncRefcount();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		quillDecRefcount();
	}
	
	protected static void quillIncRefcount() {
		backupHandler.removeCallbacks(backupAtExit);
		quillActivitiesRunning += 1;		
	}
	
	protected static void quillDecRefcount() {
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
    
    
	private static final String KEY_DEBUG_OPTIONS = HandwriterView.KEY_DEBUG_OPTIONS;
	private static final String KEY_HIDE_SYSTEM_BAR = Preferences.KEY_HIDE_SYSTEM_BAR;
	private static final String KEY_OVERRIDE_PEN_TYPE = Hardware.KEY_OVERRIDE_PEN_TYPE;
	private static final String KEY_ONLY_PEN_INPUT_OBSOLETE = "only_pen_input";

    private void removeUnusedPreferences() {
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());            
        SharedPreferences.Editor editor = settings.edit();
    	
        editor.remove(KEY_ONLY_PEN_INPUT_OBSOLETE);  // obsoleted

    	if (ReleaseMode.OEM) {
    		editor.remove(KEY_HIDE_SYSTEM_BAR);
    		editor.remove(KEY_DEBUG_OPTIONS);
    		editor.remove(KEY_OVERRIDE_PEN_TYPE);
    	}
    	editor.commit();
    }
    
}
