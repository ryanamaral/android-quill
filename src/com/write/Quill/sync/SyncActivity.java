package com.write.Quill.sync;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;

public class SyncActivity 
	extends ActivityBase {
	
	@SuppressWarnings("unused")
	private final static String TAG = "SyncActivity";

	public final static String SYNC_PREFERENCES = "sync_preferences";
	
	private MenuItem syncMenuItem;
	private SyncListFragment syncList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_activity);
		syncList = (SyncListFragment) getFragmentManager().findFragmentById(R.id.sync_list_fragment);
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
	}

	public final static int REQUEST_LOGIN = 123;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_LOGIN:
			if (resultCode != RESULT_OK) {
				finish();
				return;
			}
			QuillAccount account = new QuillAccount(this);
			if (account.exists())
				syncList.setAccount(account);			
			return;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private boolean checkAccountExists() {
		QuillAccount account = new QuillAccount(this);
		if (account.exists())
			return true;
		Intent newAccount = new Intent(this, LoginActivity.class);
		startActivityForResult(newAccount, REQUEST_LOGIN);
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Bookshelf.getCurrentBook().save();
		if (!checkAccountExists())
			return;
		QuillAccount account = new QuillAccount(this);
		syncList.setAccount(account);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sync, menu);
        syncMenuItem = (MenuItem) menu.findItem(R.id.sync_now);
        return true;
    }

    public MenuItem getSyncMenuItem() {
    	return syncMenuItem;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.sync_now:
    		syncList.runBackgroundTask();
    		return true;
    	case android.R.id.home:
    		finish();
    		return true;
    	default:
        	return super.onOptionsItemSelected(item);    			
    	}
    }
 
}
