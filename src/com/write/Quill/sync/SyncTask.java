package com.write.Quill.sync;

import java.io.File;
import java.util.LinkedList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Storage;
import com.write.Quill.data.TemporaryDirectory;
import com.write.Quill.sync.HttpPostBase.Response;
import com.write.Quill.sync.SyncData.Action;
import com.write.Quill.sync.SyncData.SyncItem;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager.NameNotFoundException;
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
	private QuillAccount account = null;
	private SyncData data;
	private Context context;

	public SyncTask(Context context) {
		this.context = context.getApplicationContext();
	}

	private int getVersionCode() {
	    int v = 0;
	    try {
	    	String name = context.getPackageName();
	        v = context.getPackageManager().getPackageInfo(name, 0).versionCode;
	    } catch (NameNotFoundException e) {}
	    return v;
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
	
	private Progress lastProgress;

	@Override
	protected void onProgressUpdate(Progress... values) {
		showAccountData();
		Progress progress = values[0];
		if (listFragment != null) {
			if (progress.data != null)
				listFragment.setData(progress.data);
			listFragment.updateMenu(progress.finished);
		}
		if (statusFragment == null)
			return;
		statusFragment.setStatus(progress.title, progress.status, progress.finished);
		lastProgress = progress;
	}

	private final static String URL_BASE = "https://quill.sagepad.org";
	private final static String URL_LOGIN = URL_BASE + "/_login";
	private final static String URL_METADATA = URL_BASE + "/_metadata";
	private final static String URL_PUSH = URL_BASE + "/_sync_push_whole";
	private final static String URL_PULL_INDEX = URL_BASE + "/_sync_pull_index";
	private final static String URL_PULL_FILE = URL_BASE + "/_sync_pull_file";

	public static class Progress {
		protected final String title;
		protected final String status;
		protected final boolean finished;
		protected final SyncData data;
		public Progress(String title, String status, boolean finished, SyncData data) {
			this.title = title;
			this.status = status;
			this.finished = finished;
			this.data = data;
		}
		public Progress() {
			this.title = "Ready";
			this.status = "";
			this.finished = true;
			this.data = null;
		}
	}
	
	void publishProgress(String title, String status, boolean finished) {
		Progress progress = new Progress(title, status, finished, null);
		publishProgress(progress);
	}

	void publishProgress(String title, String status, boolean finished, SyncData data) {
		Progress progress = new Progress(title, status, finished, data);
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

			if (!data.command.equals(SyncData.Command.SYNC_ONLY)) {
				publishProgress("Fetching metadata", "Contacting server...", false);
				fetchMetadata();
			}
			
			if (!data.command.equals(SyncData.Command.METADATA_ONLY)) {
				publishProgress("Synchronizing", "Contacting server...", false);
				for (SyncData.SyncItem item : data) {
					Log.e(TAG, "sync " + item.getAction());
					switch (item.getAction()) {
					case PULL_TO_ANDROID:
						publishProgress("Downloading", item.getTitle(), false);
						pullNotebook(item);
						break;
					case PUSH_TO_SERVER:
						publishProgress("Uploading", item.getTitle(), false);
						pushNotebook(item);
						break;
					case SKIP:
						break;
					}
				}

				publishProgress("Reloading metadata", "Contacting server...", false);
				fetchMetadata();
			}
			
			publishProgress("Ready", "Press \"Sync Now\" to start.", true);

		} catch (SyncException e) {
			publishProgress("Failed", e.getMessage(), true);
			data.setSessionToken(null);  // force re-login
		}

		if (data.command.equals(SyncData.Command.METADATA_ONLY))
			data.command= SyncData.Command.SYNC_ONLY;

		data = null;
		return null;
	}

	private void pushNotebook(SyncItem item) throws SyncException {
		HttpPostJson http = new HttpPostJson(URL_PUSH);
		http.send("email", account.email());
		http.send("session_token", data.getSessionToken());
		http.send("app_version", String.valueOf(getVersionCode()));
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
		item.saveSyncTime(item.getLocalModTime());
	}

	private void pullNotebook(SyncItem item) throws SyncException {
		HttpPostJson http = new HttpPostJson(URL_PULL_INDEX);
		http.send("email", account.email());
		http.send("session_token", data.getSessionToken());
		http.send("app_version", String.valueOf(getVersionCode()));
		http.send("book_uuid", item.getUuid().toString());
		http.send("book_mtime", String.valueOf(item.getRemoteModTime().toMillis(false)));

		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.sync_error, response.getMessage());
			publishProgress("Error downloading index", msg, true);
			throw new SyncException(response);
		}
		
		LinkedList<String> filenames = new LinkedList<String>();
		try {
			JSONArray index = response.getJSON().getJSONArray("filenames");
			for (int i=0; i<index.length(); i++)
				filenames.add(index.getString(i));
		} catch (JSONException e) {
			Log.e(TAG, "JSON[index] "+e.getMessage());
			String msg = context.getResources().getString(R.string.sync_error, "Invalid JSON");
			publishProgress("Index is invalid JSON", msg, true);
			throw new SyncException(response);

		}
		publishProgress("Downloading", "Got files index", false);

		Storage storage = Storage.getInstance();
		TemporaryDirectory tmp = storage.newTemporaryDirectory();
		for (String filename : filenames)
			pullNotebookFile(item, filename, tmp);
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		bookshelf.importBookDirectory(tmp, item.getUuid());
		
		item.saveSyncTime(item.getRemoteModTime());	
	}

	private void pullNotebookFile(SyncItem item, String filename, TemporaryDirectory tmp) throws SyncException {
		Log.e(TAG, "pullNotebookFile "+filename);
		if (filename.contains("/") || filename.startsWith(".")) return;  // no paths allowed
		
		File file = new File(tmp, filename);
		HttpPostFile http = new HttpPostFile(URL_PULL_FILE, file);
		http.send("email", account.email());
		http.send("session_token", data.getSessionToken());
		http.send("book_uuid", item.getUuid().toString());
		http.send("book_mtime", String.valueOf(item.getRemoteModTime().toMillis(false)));
		http.send("filename", filename);

		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.sync_error, response.getMessage());
			publishProgress("Error downloading file", msg, true);
			throw new SyncException(response);
		}
	}

	private void login() throws SyncException {
		if (data.getSessionToken() != null)   // login only once 
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
	}

	private void fetchMetadata() throws SyncException {
		SyncData newData = data.copy();
		HttpPostJson http = new HttpPostJson(URL_METADATA);
		http.send("email", account.email());
		http.send("session_token", newData.getSessionToken());
		Response response = http.receive();
		if (!response.isSuccess()) {
			String msg = context.getResources().getString(R.string.sync_error, response.getMessage());
			publishProgress("Error logging in", msg, true);
			throw new SyncException(response);
		}
	 	try {
			JSONArray metadata = response.getJSON().getJSONArray("metadata");
			for (int i = 0; i < metadata.length(); i++) {
				JSONObject obj = metadata.getJSONObject(i);
				String uuid = obj.getString("uuid");
				String title = obj.getString("title");
				long millis = obj.getLong("mtime");
				Time time = new Time();
				time.set(millis);
				newData.addRemote(UUID.fromString(uuid), title, time);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON[metadata] " + e.getMessage());
			String msg = context.getResources().getString(R.string.sync_error, "Invalid JSON");
			publishProgress("Invalid server response", msg, true);
			return;
		}
		newData.sort();
		data = newData;
		publishProgress("Ready", "Got metadata", false, newData);
	}
}
