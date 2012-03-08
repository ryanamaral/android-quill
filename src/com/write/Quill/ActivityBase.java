package com.write.Quill;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;

import android.app.Activity;
import android.os.Bundle;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
      	StorageAndroid.initialize(getApplicationContext());
	}
	
	@Override
	protected void onResume() {
		quillActivitiesRunning += 1;
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		quillActivitiesRunning -= 1;		
	}
	
    @Override
    protected void onStop() {
    	super.onStop();
		Log.d(TAG, "quillActivitiesRunning = "+quillActivitiesRunning);
		if (quillActivitiesRunning == 0)
			backupAtExit();
    }

    private void backupAtExit() {
		Bookshelf.getBookshelf().backup();
		Toast.makeText(getApplicationContext(), "Quill notebooks backed up.", Toast.LENGTH_SHORT).show();
    }
    
}
