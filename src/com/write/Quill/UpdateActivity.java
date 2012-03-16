package com.write.Quill;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import com.write.Quill.data.Book.BookLoadException;
import com.write.Quill.data.Storage;
import com.write.Quill.data.StorageAndroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateActivity 
	extends Activity 
	implements OnClickListener {
	private final static String TAG = "UpdateActivity";
	
	private ProgressBar progress;
	private TextView path, space, available;
	private Button okButton, changeButton;

	private final File defaultPath = new File(Environment.getExternalStorageDirectory(), "Quill"); 
	private String dirName;
	private static boolean done = false;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
      	StorageAndroid.initialize(getApplicationContext());
      	
    	LayoutInflater inflater = getLayoutInflater();
    	View layout = inflater.inflate(R.layout.update_activity, null);
    	setContentView(layout);
    	setTitle("Quill Update");
    	
    	progress = (ProgressBar) layout.findViewById(R.id.update_progress);
    	path = (TextView) layout.findViewById(R.id.update_path);
    	space = (TextView) layout.findViewById(R.id.update_space);
    	available = (TextView) layout.findViewById(R.id.update_available);
    	okButton = (Button) layout.findViewById(R.id.update_button_ok);
    	changeButton = (Button) layout.findViewById(R.id.update_button_change);

    	okButton.setOnClickListener(this);
    	changeButton.setOnClickListener(this);
    	progress.setVisibility(View.GONE);
	}
	
	/**
	 * Static method to call from the main activity. Will run the updater if necessary.
	 * @param activity The main activity must be passed as the argument.
	 * @return Whether the update is required. If true, the main activity has been finish()ed.
	 */
	public static boolean needUpdate(Activity activity) {
		if (done) return false;		
		Context context = activity.getApplicationContext();
        Storage storage = Storage.getInstance();
		if (!storage.needUpdate()) {
			done = true;
			return false;
		}
		Intent i = new Intent(context, UpdateActivity.class);    
    	activity.startActivity(i);
    	activity.finish();
		return true;
	}
	
	private void showToast(final String message) {
		runOnUiThread(new Runnable() {
		    public void run() {
		        Toast.makeText(UpdateActivity.this, message, Toast.LENGTH_LONG).show();
		    }
		});
	}

	private void update() {
		Context context = getApplicationContext();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		dirName = settings.getString(Preferences.KEY_BACKUP_DIR, defaultPath.getAbsolutePath());
		path.setText(dirName);
		
		final File f = new File(dirName);
		File parent = f.getParentFile();
		if (parent == null) parent = f;
		if (parent == null || !parent.exists() || !parent.isDirectory() || !parent.canWrite()) {
			okButton.setEnabled(false);
			return;
		} else {
			okButton.setEnabled(true);
		}
		
		StatFs stat = new StatFs(parent.getPath());
		long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
		available.setText("Available Space: " + String.format("%4.3f", bytesAvailable/(1024.f * 1024.f)) + "MB");
		
		long bytesRequired = usedDiskSpace();
		space.setText("Required space: " + String.format("%4.3f", bytesRequired/(1024.f * 1024.f)) + "MB");

	}
	
	
	protected static class SizeCounter implements FileFilter
	{
	    protected long total = 0;
	    
		@Override
		public boolean accept(File file) {
	        if ( file.isFile())
	            total+=file.length();
	        else
	            file.listFiles(this);
			return false;
		}
	}

	protected static class DirectoryWalker implements FileFilter
	{
	    protected LinkedList<File> files = new LinkedList<File>();
		@Override
		public boolean accept(File file) {
	        if ( file.isFile())
	        	files.add(file);
	        else
	            file.listFiles(this);
			return false;
		}
	}

	
	private long usedDiskSpace() {
		SizeCounter counter = new SizeCounter();
		getFilesDir().listFiles(counter);
		return counter.total;
	}
	
	@Override
	public void onClick(View view) {
		if (view == okButton) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Preferences.KEY_BACKUP_DIR, dirName);
            editor.commit();
			progress.setVisibility(View.VISIBLE);
			okButton.setEnabled(false);
			changeButton.setEnabled(false);
			run();
		} else if (view == changeButton) {
    		Intent intent = new Intent(getApplicationContext(), name.vbraun.filepicker.FilePickerActivity.class);
    		intent.setAction("org.openintents.action.PICK_DIRECTORY");
    		intent.putExtra("org.openintents.extra.TITLE", "Please select a backup folder");
    		startActivityForResult(intent, Preferences.REQUEST_CODE_PICK_BACKUP_DIRECTORY);
		}
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
    	case Preferences.REQUEST_CODE_PICK_BACKUP_DIRECTORY:
    		String dirName = filenameFromActivityResult(resultCode, data);
    		if (dirName == null) return;
            SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());            
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Preferences.KEY_BACKUP_DIR, dirName);
            editor.commit();
            update();
    		break;
    	}
    }
	    

	
	@Override
	protected void onResume() {
		super.onResume();
    	update();
	}

	
	private void run() {
		File dir = new File(dirName);
		if (!dir.exists()) dir.mkdir();
		final Storage storage = Storage.getInstance();
		new Thread(new Runnable(){
			public void run(){
				try {
					backupFilesDir();
					storage.update();
					showToast("Finished converting notebooks!");
					done = true;
					storage.destroy();
					Intent intent = new Intent(getApplicationContext(), QuillWriterActivity.class);
					startActivity(intent);
					finish();
				} catch (IOException e) {
					Log.e(TAG, "Error backing up notebooks " + e.getMessage());
					showToast("Error backing up notebooks!");				
				} catch (BookLoadException e) {
					Log.e(TAG, "Error converting notebooks " + e.getMessage());
					showToast("Error converting notebooks!");				
				}
			}
		}).start();
	}
	
	private void backupFilesDir() throws IOException {
		File dest = new File(dirName, "pre_update.tar");
		TarOutputStream out = new TarOutputStream( new BufferedOutputStream( new FileOutputStream(dest)));

		File dir = getFilesDir();
		DirectoryWalker walker = new DirectoryWalker();
		getFilesDir().listFiles(walker);
		LinkedList<File> filesToTar = walker.files;
		
		int offset = dir.getAbsolutePath().length();
		int count;
		byte data[] = new byte[2048];
		for(File f:filesToTar){
			Log.d(TAG, "backup "+f.getAbsolutePath());
			String name = f.getAbsolutePath().substring(offset);
			out.putNextEntry(new TarEntry(f, name));
			BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));
			while((count = origin.read(data)) != -1)
				out.write(data, 0, count);
			out.flush();
			origin.close();
		}
		out.close();
	}
}
