package com.write.Quill.sync;

import org.json.JSONException;

import com.write.Quill.R;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Handle the login loader part of SyncActivity
 * @author vbraun
 *
 */
public class SyncActivityLogin 
	implements LoaderCallbacks<LoginLoader.Response> {
	
	private final static String TAG = "SyncActivityLogin";
	private SyncActivity activity;
	
	public SyncActivityLogin(SyncActivity activity) {
		this.activity = activity;
		activity.getLoaderManager().initLoader(SyncActivity.LOADER_LOGIN, null, this);
	}
	
	@Override
	public Loader<LoginLoader.Response> onCreateLoader(int id, Bundle args) {
		return new LoginLoader(activity, activity.getEmail(), activity.getPassword());
	}

	@Override
	public void onLoadFinished(Loader<LoginLoader.Response> loader, LoginLoader.Response response) {
		
		Log.e(TAG, "onLoadFinished "+response.getHttpCode() + " " + response.getMessage()); 
		if (!response.isSuccess()) {
			String msg = activity.getResources().getString(R.string.account_error_login, response.getMessage());
			activity.onLoginFinished(false, msg, null);
			return;
		}
		
		String sessionToken;
		try {
			sessionToken = response.getJSON().getString("session_token");
		} catch (JSONException e) {
			Log.e(TAG, "JSON[session_token] "+e.getMessage());
			String msg = activity.getResources().getString(R.string.account_error_login, "Invalid JSON");
			activity.onLoginFinished(false, msg, null);
			return;
		}

		activity.onLoginFinished(true, response.getMessage(), sessionToken);
	}

	@Override
	public void onLoaderReset(Loader<LoginLoader.Response> loader) {
	}

}
