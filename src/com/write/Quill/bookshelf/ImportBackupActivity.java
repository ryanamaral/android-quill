package com.write.Quill.bookshelf;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;

/**
 * This class is called if the user picks a .quill file with a file manager
 * @author vbraun
 *
 */
public class ImportBackupActivity extends ActivityBase {
	@SuppressWarnings("unused")
	private final static String TAG = "ImportBackupActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.import_backup);
	}
	
	private File file = null;
	
	@Override
	protected void onStart() {
		super.onStart();
	    final Intent intent = getIntent ();
	    if (intent == null) return;
	    Uri uri = intent.getData();
	    if (uri == null) return;
        final String filename = uri.getEncodedPath();
        file = new File(filename);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.e(TAG, "onResume");
	}
}
