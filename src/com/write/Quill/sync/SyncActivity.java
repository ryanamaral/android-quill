package com.write.Quill.sync;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;

public class SyncActivity 
	extends ActivityBase {
	
	private final static String TAG = "SyncActivity";

	public final static String SYNC_PREFERENCES = "sync_preferences";
	
	private MenuItem syncMenuItem;
	private SyncListFragment syncList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_activity);
		syncList = (SyncListFragment) getFragmentManager().findFragmentById(R.id.sync_list_fragment);
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
    	if (item == syncMenuItem) {
    		syncList.runBackgroundTask();
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
}
