package com.write.Quill.sync;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.write.Quill.R;
import com.write.Quill.sync.HttpPostJson.Response;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

/**
 * Do the synchronization in a background task
 * 
 * @author vbraun
 * 
 */
public class SyncTask extends AsyncTask<SyncData, SyncTask.Progress, Void> {
	private final static String TAG = "SyncTask";

	private SyncStatusFragment statusFragment = null;
	private SyncDataAdapter dataAdapter = null;
	private QuillAccount account = null;
	private SyncData data;
	private Context context;

	public SyncTask(Context context) {
		this.context = context.getApplicationContext();
	}

	/**
	 * If set, progress information will be shown in this fragment
	 * @param fragment
	 */
	public void setSyncStatusFragment(SyncStatusFragment fragment) {
		statusFragment = fragment;
		showAccountData();
	}
	
	/**
	 * If set, the adapter to the SyncData will be notified of changes
	 */
	public void setSyncAdapter(SyncDataAdapter adapter) {
		dataAdapter = adapter;
	}

	private void showAccountData() {
		Log.e(TAG, "show account " + statusFragment + " " + account);
		if (statusFragment != null && account != null)
			statusFragment.setAccount(account);
		if (statusFragment != null && lastProgress != null)
			statusFragment.setStatus(lastProgress.title, lastProgress.status, lastProgress.finished);
	}
	
	private void notifyDataChanged() {
		if (dataAdapter == null)
			return;
		dataAdapter.notifyDataSetChanged();
	}
	
	private Progress lastProgress;

	@Override
	protected void onProgressUpdate(Progress... values) {
		Progress progress = values[0];
		notifyDataChanged();
		if (statusFragment == null)
			return;
		statusFragment.setStatus(progress.title, progress.status, progress.finished);
		lastProgress = progress;
	}

	private final static String URL_BASE = "http://quill.sagepad.org";
	private final static String URL_LOGIN = URL_BASE + "/_login";
	private final static String URL_METADATA = URL_BASE + "/_metadata";
	private final static String URL_SYNC = URL_BASE + "/_sync";

	public static class Progress {
		protected final String title;
		protected final String status;
		protected final boolean finished;
		public Progress(String title, String status, boolean finished) {
			this.title = title;
			this.status = status;
			this.finished = finished;
		}
		public Progress() {
			this.title = "Ready";
			this.status = "";
			this.finished = true;
		}
	}
	
	void publishProgress(String title, String status, boolean finished) {
		Progress progress = new Progress(title, status, finished);
		publishProgress(progress);
	}
	
	public static class SyncException extends Exception {
		private static final long serialVersionUID = -4944278660640930791L;
		Response response;

		public SyncException(Response response) {
			this.response = response;
		}

		public Response getResponse() {
			return response;
		}
	}

	@Override
	protected Void doInBackground(SyncData... param) {
		data = param[0];
		account = data.getAccount();
		showAccountData();

		try {
			publishProgress("Logging in", "Contacting server...", false);
			login();

			publishProgress("Fetching metadata", "Contacting server...", false);
			fetchMetadata();

		} catch (SyncException e) {
		}
		data = null;
		return null;
	}

	private void login() throws SyncException {
		HttpPostJson http = new HttpPostJson(URL_LOGIN);
		http.send("email", account.email());
		http.send("password", account.password());
		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.account_error_login, response.getMessage());
			publishProgress("Error logging in", msg, true);
			throw new SyncException(response);
		}
		String sessionToken;
		try {
			sessionToken = response.getJSON().getString("session_token");
		} catch (JSONException e) {
			Log.e(TAG, "JSON[session_token] "+e.getMessage());
			String msg = context.getResources().getString(R.string.account_error_login, "Invalid JSON");
			publishProgress("Error logging in", msg, true);
			throw new SyncException(response);
		}
		data.setSessionToken(sessionToken);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void fetchMetadata() throws SyncException {
		HttpPostJson http = new HttpPostJson(URL_METADATA);
		http.send("email", account.email());
		http.send("session_token", data.getSessionToken());
		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.sync_error, response.getMessage());
			publishProgress("Error logging in", msg, true);
			throw new SyncException(response);
		}
		try {
			JSONArray metadata = response.getJSON().getJSONArray("metadata");
			Time time = new Time();
			for (int i = 0; i < metadata.length(); i++) {
				JSONObject obj = metadata.getJSONObject(i);
				String uuid = obj.getString("uuid");
				String title = obj.getString("title");
				long millis = obj.getLong("mtime");
				time.set(millis);
				data.addRemote(UUID.fromString(uuid), title, time);
				Log.e(TAG, "Meta " + uuid + " " + time);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON[metadata] " + e.getMessage());
			String msg = context.getResources().getString(R.string.sync_error, "Invalid JSON");
			publishProgress("Invalid server response", msg, true);
			return;
		}
		data.sort();
		publishProgress("Ready", "Got metadata", true);
	}
}
