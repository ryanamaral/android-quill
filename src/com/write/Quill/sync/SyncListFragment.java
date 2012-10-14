package com.write.Quill.sync;

import com.write.Quill.R;

import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.util.Log;

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
		if (task == null) {
			task = new SyncTask(getActivity());
			task.execute(data);
		}
		setListAdapter(new SyncDataAdapter(getActivity(), R.layout.sync_item, data));
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SyncStatusFragment status = (SyncStatusFragment) 
				getFragmentManager().findFragmentById(R.id.sync_status_fragment);
		Log.e(TAG, "onResume :"+status);
		if (status != null) {
			task.setSyncStatusFragment(status);
		}
		task.setSyncAdapter((SyncDataAdapter) getListAdapter());
	}
	
	@Override
	public void onPause() {
		task.setSyncStatusFragment(null);
		task.setSyncAdapter(null);
		super.onPause();
	}
	
}
