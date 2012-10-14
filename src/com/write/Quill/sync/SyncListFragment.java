package com.write.Quill.sync;

import com.write.Quill.R;
import com.write.Quill.sync.SyncData.Action;

import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class SyncListFragment extends ListFragment {
	private final static String TAG = "SyncListFragment";

	private SyncData data = null;
	private SyncTask task = null;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);

		if (data == null) {
			SharedPreferences syncPrefs = getActivity().getSharedPreferences(SyncActivity.SYNC_PREFERENCES,
					Context.MODE_PRIVATE);
			QuillAccount account = new QuillAccount(getActivity());
			data = new SyncData(syncPrefs, account);
			data.reset();
		}
		runBackgroundTask();
		setListAdapter(new SyncDataAdapter(getActivity(), R.layout.sync_item, data));
	}
	
	protected void runBackgroundTask() {
		updateMenu(false);
		task = new SyncTask(getActivity());
		task.execute(data);
		task.setSyncListFragment(this);
		SyncStatusFragment status = (SyncStatusFragment) 
				getFragmentManager().findFragmentById(R.id.sync_status_fragment);
		if (status != null) {
			task.setSyncStatusFragment(status);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		task.setSyncListFragment(this);
		SyncStatusFragment status = (SyncStatusFragment) 
				getFragmentManager().findFragmentById(R.id.sync_status_fragment);
		if (status != null) {
			task.setSyncStatusFragment(status);
		}
	}
	
	@Override
	public void onPause() {
		task.setSyncStatusFragment(null);
		task.setSyncListFragment(null);
		super.onPause();
	}
	
	/**
	 * set menu items enabled / disabled depending on current state
	 */
	protected void updateMenu(boolean finished) {
		SyncActivity activity = (SyncActivity) getActivity();
		if (activity == null)
			return;
		MenuItem sync = activity.getSyncMenuItem();
		if (sync == null)
			return;
		sync.setEnabled(finished);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SyncData.SyncItem item = data.get(position);
		item.cycleAction();
		Log.e(TAG, "click: "+item.getAction().toString());
		SyncDataAdapter adapter = (SyncDataAdapter) getListAdapter();
		adapter.notifyDataSetChanged();
		super.onListItemClick(l, v, position, id);
	}
}
