package com.write.Quill.image;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import name.vbraun.view.write.Page;

/**
 * Base class dialogs that output a single (image) file
 * 
 * @author vbraun
 * 
 */
public abstract class DialogBase extends DialogFragment {
	private final static String TAG = "DialogBase";

	abstract protected String getCancelMessage();
	abstract protected void initProgresDialog(ProgressDialog dialog);
	abstract protected void setProgress(int progress);
	abstract protected ThreadBase makeThread(Uri source, File destination);
	abstract protected void onFinish(File file);

	protected static ThreadBase thread = null;
	protected Uri sourceUri;
	protected File destinationFile;
	
	private final static String KEY_SOURCE_URI = "source_uri";
	private final static String KEY_DESTINATION_FILE = "destination_file";
	
	protected static Bundle storeArgs(Uri uri, File file) {
		Bundle bundle = new Bundle();
		bundle.putString(KEY_SOURCE_URI, uri.toString());
		bundle.putString(KEY_DESTINATION_FILE, file.getAbsolutePath());
		return bundle;
	}
	
	protected void loadArgs(Bundle bundle) {
		Assert.assertNotNull(bundle);
		sourceUri = Uri.parse(bundle.getString(KEY_SOURCE_URI));
		destinationFile = new File(bundle.getString(KEY_DESTINATION_FILE));
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadArgs(getArguments());
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		initProgresDialog(dialog);
		return dialog;
	}

	protected ProgressDialog getProgressDialog() {
		return (ProgressDialog) getDialog();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (thread != null)
			thread.interrupt();
		thread = null;
		toast(getCancelMessage());
		super.onCancel(dialog);
	}
	
	@Override
	public void onPause() {
		// Log.e(TAG, "onPause "+thread);
		handler.removeCallbacks(mUpdateProgress);
		if (thread != null)
			thread.setDialog(null);
		super.onPause();
	}
	
	@Override
	public void onResume() {
		handler.post(mUpdateProgress);
		thread.setDialog(this);
		super.onResume();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Log.e(TAG, "onStart "+thread);
		ProgressDialog dlg = getProgressDialog();
		if (thread == null) {
			dlg.setProgress(0);
			thread = makeThread(sourceUri, destinationFile);
			thread.setDialog(this);
			thread.start();
		}
	}

	private final static int MESSAGE_TOAST = 1;
	private final static int MESSAGE_PROGRESS = 2;

	
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Activity activity = getActivity();
			if (activity == null) return true;
			Context context = activity.getApplicationContext();
			switch (msg.what) {
			case MESSAGE_TOAST:
				String s = (String) msg.obj;
				Toast.makeText(context, s, Toast.LENGTH_LONG).show();
				return false;
			case MESSAGE_PROGRESS:
				Integer progress = (Integer) msg.obj;
				setProgress(progress);				
				return false;
			}
			return true;
		}
	});

	/**
	 * Show a toast from a background thread
	 * @param s
	 */
	protected void toast(String s) {
		Message msg = handler.obtainMessage(MESSAGE_TOAST, s);
		handler.sendMessage(msg);
	}

	/**
	 * Show a toast from a background thread
	 * @param resId
	 * @param values
	 */
	protected void toast(int resId, Object... values) {
		Message msg = handler.obtainMessage(MESSAGE_TOAST, getString(resId, values));
		handler.sendMessage(msg);
	}

	private Runnable mUpdateProgress = new Runnable() {
		public void run() {
			if (thread == null) return;
			ProgressDialog dlg = getProgressDialog();
			if (dlg == null) return;
			if (thread.isFinished()) {
				onFinish(thread.file);
				dismiss();
				thread = null;
			} else {
				Message msg = handler.obtainMessage(MESSAGE_PROGRESS, new Integer(thread.getProgress()));
				handler.sendMessage(msg);
				handler.postDelayed(mUpdateProgress, 100);
			}
		}
	};

}
