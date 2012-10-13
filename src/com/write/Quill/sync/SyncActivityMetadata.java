package com.write.Quill.sync;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.write.Quill.R;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * Handle the metadata loader part of SyncActivity
 * @author vbraun
 *
 */
public class SyncActivityMetadata 
implements LoaderCallbacks<MetadataLoader.Response> {

	private final static String TAG = "SyncActivityMetadata";
	private final SyncActivity activity;
	private final String sessionToken;
	
	public SyncActivityMetadata(SyncActivity activity, String sessionToken) {
		this.activity = activity;
		this.sessionToken = sessionToken;
		activity.getLoaderManager().initLoader(SyncActivity.LOADER_METADATA, null, this);
	}

	@Override
	public Loader<MetadataLoader.Response> onCreateLoader(int id, Bundle args) {
		return new MetadataLoader(activity, activity.getEmail(), sessionToken);
	}

	@Override
	public void onLoadFinished(Loader<MetadataLoader.Response> loader, MetadataLoader.Response response) {

		Log.e(TAG, "onLoadFinished "+response.getHttpCode() + " " + response.getMessage()); 
		if (!response.isSuccess()) {
			String msg = activity.getResources().getString(R.string.sync_error, response.getMessage());
			activity.onMetadataFinished(false, msg, null);
			return;
		}

		SharedPreferences syncPrefs = 
				activity.getSharedPreferences(SyncActivity.SYNC_PREFERENCES, Context.MODE_PRIVATE);
		SyncData data = new SyncData(syncPrefs);
		data.setSyncSessionToken(sessionToken);
		data.reset();
		
		JSONArray metadata;
		try {
			metadata = response.getJSON().getJSONArray("metadata");
			Time time = new Time();
			for (int i=0; i<metadata.length(); i++) {
				JSONObject obj = metadata.getJSONObject(i);
				String uuid = obj.getString("uuid");
				String title = obj.getString("title");
				long millis = obj.getLong("mtime");
				time.set(millis);
				data.addRemote(UUID.fromString(uuid), title, time);
				Log.e(TAG, "Meta "+uuid+ " "+time);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON[metadata] "+e.getMessage());
			String msg = activity.getResources().getString(R.string.sync_error, "Invalid JSON");
			activity.onMetadataFinished(false, msg, null);
			return;
		}
		data.sort();
		activity.onMetadataFinished(true, response.getMessage(), data);
	}

	@Override
	public void onLoaderReset(Loader<MetadataLoader.Response> loader) {
	}


}
