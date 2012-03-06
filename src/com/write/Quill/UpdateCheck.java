package com.write.Quill;

import sheetrock.panda.changelog.ChangeLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.write.Quill.data.Book.BookLoadException;
import com.write.Quill.data.Storage;

public class UpdateCheck {
    private static final String TAG = "UpdateCheck";
	
    private final Context context;
    private final boolean needStorageUpdate;
    private Storage storage;
    
    private static final String KEY_VERSION = "PREFS_VERSION_KEY";
   
    private static boolean done = false;
    
    
    public UpdateCheck(Context context) {
        this.context = context;
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String lastVersion, version;
        lastVersion = sp.getString(KEY_VERSION, "none");
        try {
			version = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			version = "Unknown version";
		}
        
    	needStorageUpdate = true;

//        if (version.equals(lastVersion)) {
//        	needStorageUpdate = false;
//        	showChangeLog();
//        } else {
//        	storage = Storage.getInstance();
//        	needStorageUpdate = storage.needUpdate();
//        }
//        if (!necessary()) {
//        	SharedPreferences.Editor editor = sp.edit();
//        	editor.putString(KEY_VERSION, version);
//        	editor.commit();
//        	showChangeLog();
//        }
    }
    
    public void showChangeLog() {
  		ChangeLog changeLog = new ChangeLog(context);
  		if (changeLog.firstRun())
  			changeLog.getLogDialog().show();
    }
    
    public boolean necessary() {
    	if (done) return false;
    	return needStorageUpdate;
    }
    
    public void run() {
    	done = true;
    	if (!necessary()) return;
    	Log.d(TAG, "Updating notebook files "+necessary());
    	try {
			storage.update();
		} catch (BookLoadException e) {
			Log.e(TAG, e.getMessage());
			Toast.makeText(context, "Error converting notebooks!", Toast.LENGTH_LONG).show();
		}
		Toast.makeText(context, "Finished converting notebooks!", Toast.LENGTH_LONG).show();
    }
    
}

