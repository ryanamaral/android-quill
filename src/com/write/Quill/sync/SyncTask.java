package com.write.Quill.sync;

import java.io.File;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.write.Quill.R;
import com.write.Quill.data.Storage;
import com.write.Quill.sync.HttpPostJson.Response;
import com.write.Quill.sync.SyncData.Action;
import com.write.Quill.sync.SyncData.SyncItem;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;
import android.widget.ListAdapter;

/**
 * Do the synchronization in a background task
 * 
 * @author vbraun
 * 
 */
public class SyncTask extends AsyncTask<SyncData, SyncTask.Progress, Void> {
	private final static String TAG = "SyncTask";

	private SyncStatusFragment statusFragment = null;
	private SyncListFragment listFragment = null;
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
	
	public void setSyncListFragment(SyncListFragment fragment) {
		listFragment = fragment;
	}
	
	private void showAccountData() {
		if (statusFragment != null && account != null)
			statusFragment.setAccount(account);
		if (statusFragment != null && lastProgress != null)
			statusFragment.setStatus(lastProgress.title, lastProgress.status, lastProgress.finished);
		if (listFragment != null && lastProgress != null)
			listFragment.updateMenu(lastProgress.finished);
	}
	
	private void notifyDataChanged() {
		if (listFragment == null)
			return;
		SyncDataAdapter adapter = (SyncDataAdapter) listFragment.getListAdapter();
		if (adapter == null)
			return;
		adapter.notifyDataSetChanged();
	}
	
	private Progress lastProgress;

	@Override
	protected void onProgressUpdate(Progress... values) {
		showAccountData();
		Progress progress = values[0];
		notifyDataChanged();
		if (listFragment != null)
			listFragment.updateMenu(progress.finished);
		if (statusFragment == null)
			return;
		statusFragment.setStatus(progress.title, progress.status, progress.finished);
		lastProgress = progress;
	}

	private final static String URL_BASE = "http://quill.sagepad.org";
	private final static String URL_LOGIN = URL_BASE + "/_login";
	private final static String URL_METADATA = URL_BASE + "/_metadata";
	private final static String URL_PUSH = URL_BASE + "/_sync_push_whole";
	private final static String URL_PULL = URL_BASE + "/_sync_pull_whole";

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

		try {
			publishProgress("Logging in", "Contacting server...", false);
			login();

			publishProgress("Fetching metadata", "Contacting server...", false);
			fetchMetadata();
			
			boolean changed = false;
			publishProgress("Synchronizing", "Contacting server...", false);
			for (SyncData.SyncItem item : data)
				switch (item.getAction()) {
				case PULL_TO_ANDROID:
					publishProgress("Downloading", item.getTitle(), false);
					pullNotebook(item);
					changed = true;
				case PUSH_TO_SERVER:
					publishProgress("Uploading", item.getTitle(), false);
					pushNotebook(item);
					changed = false;
				}
			
			if (changed) {
				data.setMetadata(false);
				publishProgress("Reloading metadata", "Contacting server...", false);
				fetchMetadata();
			}
			
			publishProgress("Ready", "Press \"Sync Now\" to start.", true);

		} catch (SyncException e) {
			data.setSessionToken(null);  // force re-login
		}
		data = null;
		return null;
	}

	private void pushNotebook(SyncItem item) throws SyncException {
		// TODO Auto-generated method stub
		HttpPostJson http = new HttpPostJson(URL_PUSH);
		http.send("email", account.email());
		http.send("session_token", data.getSessionToken());
		http.send("book_uuid", item.getUuid().toString());
		http.send("book_title", item.getTitle());
		http.send("book_mtime", String.valueOf(item.getLastModTime().toMillis(false)));

		Storage storage = Storage.getInstance();
		File dir = storage.getBookDirectory(item.getUuid());
		for (File file : dir.listFiles()) {
			http.send(file.getName(), file);
		}
		
		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.sync_error, response.getMessage());
			publishProgress("Error uploading ", msg, true);
			throw new SyncException(response);
		}
	}

	private void pullNotebook(SyncItem item) throws SyncException {
		// TODO Auto-generated method stub
		
	}

	private void login() throws SyncException {
		if (data.getSessionToken() != null) 
			return;
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
		publishProgress("Logged in", "", false);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void fetchMetadata() throws SyncException {
		if (data.haveMetadata())
			return;
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
