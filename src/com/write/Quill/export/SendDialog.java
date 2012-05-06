package com.write.Quill.export;

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
 * Base class for one-click "export to..." dialog
 * 
 * @author vbraun
 * 
 */
public abstract class SendDialog extends DialogFragment {
	private final static String TAG = "DialogBase";

	abstract protected String getTitle();
	abstract protected String getMessage();
	abstract protected String getCancelMessage();
	
	abstract protected void send(ArrayList<File> fileList);

	protected static ExportThread exportThread = null;
	
	protected Book book;
	protected final LinkedList<Page> pages = new LinkedList<Page>();

	protected static Bundle storePages(Book book, LinkedList<Page> pages) {
		Bundle bundle = new Bundle();
		bundle.putString("uuid", book.getUUID().toString());
		bundle.putInt("n_pages", pages.size());
		ArrayList<Integer> pageNumbers = new ArrayList<Integer>();
		for (Page page: pages) {
			Integer n = book.getPageNumber(page);
			pageNumbers.add(n);
		}
		bundle.putIntegerArrayList("page_numbers", pageNumbers);	
		return bundle;
	}
	
	protected void loadPages(Bundle bundle) {
		Assert.assertNotNull(bundle);

		UUID uuid = UUID.fromString(bundle.getString("uuid"));
		book = Bookshelf.getCurrentBook();
		Assert.assertEquals(book.getUUID(), uuid);
		
		ArrayList<Integer> pageNumbers = bundle.getIntegerArrayList("page_numbers");
		pages.clear();
		for (Integer n: pageNumbers) 
			pages.add(book.getPage(n));
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadPages(getArguments());
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setTitle(getTitle());
		dialog.setMessage(getMessage());
		dialog.setIndeterminate(false);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}

	private ProgressDialog getProgressDialog() {
		return (ProgressDialog) getDialog();
	}

	protected ArrayList<String> getTags() {
		ArrayList<String> tags = new ArrayList<String>();
		for (Page page: pages) {
			ListIterator<Tag> iter = page.getTags().tagIterator();
			while (iter.hasNext()) {
				String tag = iter.next().toString();
				if (!tags.contains(tag))
					tags.add(tag);
			}
		}
		Collections.sort(tags);
		return tags;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (exportThread != null)
			exportThread.interrupt();
		exportThread = null;
		toast(getCancelMessage());
		super.onCancel(dialog);
	}
	
	@Override
	public void onPause() {
		// Log.e(TAG, "onPause "+exportThread);
		handler.removeCallbacks(mUpdateProgress);
		if (exportThread != null)
			exportThread.setSendDialog(null);
		super.onPause();
	}
	
	@Override
	public void onResume() {
		handler.post(mUpdateProgress);
		exportThread.setSendDialog(this);
		super.onResume();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.e(TAG, "onStart "+exportThread);
		ProgressDialog dlg = getProgressDialog();
		dlg.setMax(pages.size() + 1);
		if (exportThread == null) {
			dlg.setProgress(0);
			File dir = getActivity().getExternalCacheDir();
			exportThread = new ExportThread(book, pages, dir);
			exportThread.setSendDialog(this);
			exportThread.start();
		}
	}

	/**
	 * Convenience method to convert list of files to list of Uri
	 * 
	 * @param fileList
	 * @return
	 */
	protected ArrayList<Uri> uriListFromFiles(ArrayList<File> fileList) {
		final ArrayList<Uri> uriList = new ArrayList<Uri>();
		for (File file : fileList)
			uriList.add(Uri.fromFile(file));
		return uriList;
	}

	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			String s = (String) msg.obj;
			Context context = getActivity().getApplicationContext();
			Toast.makeText(context, s, Toast.LENGTH_LONG).show();
			return false;
		}
	});

	/**
	 * Show a toast from a background thread
	 * @param s
	 */
	protected void toast(String s) {
		Message msg = handler.obtainMessage(0, s);
		handler.sendMessage(msg);
	}

	/**
	 * Show a toast from a background thread
	 * @param resId
	 * @param values
	 */
	protected void toast(int resId, Object... values) {
		Message msg = handler.obtainMessage(0, getString(resId, values));
		handler.sendMessage(msg);
	}

	private Runnable mUpdateProgress = new Runnable() {
		public void run() {
			if (exportThread == null) return;
			ProgressDialog dlg = getProgressDialog();
			if (dlg == null) return;
			if (exportThread.isFinished()) {
				ArrayList<File> fileList = exportThread.getFileList();
				send(fileList);
				dismiss();
				exportThread = null;
			} else {
				dlg.setProgress(exportThread.progress);
				handler.postDelayed(mUpdateProgress, 100);
			}
		}
	};

}
