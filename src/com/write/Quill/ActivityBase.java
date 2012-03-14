package com.write.Quill;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
      	StorageAndroid.initialize(context);
	}
	
	@Override
	protected void onResume() {
		backupHandler.removeCallbacks(backupAtExit);
		quillActivitiesRunning += 1;
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
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
    		Toast.makeText(context, "Quill notebooks backed up.", Toast.LENGTH_SHORT).show();
    	}
    };
    
}
