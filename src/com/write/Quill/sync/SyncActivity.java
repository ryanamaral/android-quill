package com.write.Quill.sync;

import java.util.UUID;

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
	
	private ViewSwitcher switcher;
	private TextView nameTextView, emailTextView;
	private ListView listView;
	private SyncData data;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sync_activity);
		switcher = (ViewSwitcher) findViewById(R.id.sync_view_switcher);
		nameTextView = (TextView) findViewById(R.id.sync_name);
		emailTextView = (TextView) findViewById(R.id.sync_email);
		listView = (ListView) findViewById(R.id.sync_list);
		data = new SyncData();
		listView.setAdapter(new SyncDataAdapter(this, R.layout.sync_item, data));
	}

	private void showStatus() {
		switcher.showPrevious();
	}
	
	private void showList() {
		switcher.showNext();
	}
	
	private void refresh() {
		showStatus();
		data.reset();
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
	
	@Override
	protected void onResume() {
		super.onResume();
		initAccountData();
		refresh();
	}
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	
	
}
