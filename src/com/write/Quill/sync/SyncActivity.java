package com.write.Quill.sync;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;

public class SyncActivity extends ActivityBase {
	private final static String TAG = "SyncActivity";
		
	public final static String SYNC_PREFERENCES = "sync_preferences";
	
	public final static int LOADER_LOGIN = 0;
	public final static int LOADER_METADATA = 1;
	
	private ViewSwitcher switcher;
	private TextView syncStatusBig, syncStatus;
	private TextView nameTextView, emailTextView;
	private ListView listView;
	private SyncData data = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_activity);
		switcher = (ViewSwitcher) findViewById(R.id.sync_view_switcher);
		syncStatusBig = (TextView) findViewById(R.id.sync_status_big);
		syncStatus = (TextView) findViewById(R.id.sync_status);
		nameTextView = (TextView) findViewById(R.id.sync_name);
		emailTextView = (TextView) findViewById(R.id.sync_email);
		listView = (ListView) findViewById(R.id.sync_list);
		data = (SyncData) getLastNonConfigurationInstance();
		if (data != null)
			listView.setAdapter(new SyncDataAdapter(this, R.layout.sync_item, data));
		Log.e(TAG, "onCreate "+ (data==null));
	}
	
	
	private SyncActivityLogin syncActivityLogin = null;
	private SyncActivityMetadata syncActivityMetadata = null;
	
	/**
	 * The entry point for logging in
	 */
	protected void startLogin() {
		showStatus();
		syncStatusBig.setText("Logging in");
		syncStatus.setText("Contacting server...");
		listView.setAdapter(null);
		syncActivityLogin = new SyncActivityLogin(this);
	}
	
	/**
	 * This method is called from SyncActivityLogin when finished
	 */
	protected void onLoginFinished(boolean success, String msg, String sessionToken) {
		Log.e(TAG, "onLoginFinished token = "+sessionToken);
		syncActivityLogin = null;
		if (!success) {
			syncStatusBig.setText("Error logging in");
			syncStatus.setText(msg); 
		} else {
			startMetadata(sessionToken);
		}
	}

	/*
	 * Called from onLoginFinished
	 * Part 2: retrieve age of data stored on server
	 */
	protected void startMetadata(String sessionToken) {
		syncStatusBig.setText("Getting metadata");
		syncStatus.setText("Contacting server...");	
		syncActivityMetadata = new SyncActivityMetadata(this, sessionToken);
	}

	protected void onMetadataFinished(boolean success, String msg, SyncData data) {
		Log.e(TAG, "onMetadataFinished msg = "+msg);
		syncActivityMetadata = null;
		if (!success) {
			syncStatusBig.setText("Error getting metadata");
			syncStatus.setText(msg);
		} else {
			syncStatusBig.setText("Ready");
			syncStatus.setText("");	
			listView.setAdapter(new SyncDataAdapter(this, R.layout.sync_item, data));
			showList();
		}
	}
	
	private void showStatus() {
		switcher.setDisplayedChild(0);
	}
	
	private void showList() {
		switcher.setDisplayedChild(1);
	}
	
	private String name;
	private String email;
	private String password;
	
	private void initAccountData() {
		QuillAccount account = new QuillAccount(this);
		name = account.name();
		email = account.email();
		password = account.password();
		nameTextView.setText(name);
		emailTextView.setText(email);
	}
	
	protected String getEmail () {
		return email;
	}
	
	protected String getName() {
		return name;
	}
	
	protected String getPassword() {
		return password;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		initAccountData();
		if (data == null)
			startLogin();
		else
			showList();
	}
	
	
	@Override
	protected void onPause() {
		syncActivityMetadata = null;
		syncActivityLogin = null;
		super.onPause();
	}
	
	
	
}
