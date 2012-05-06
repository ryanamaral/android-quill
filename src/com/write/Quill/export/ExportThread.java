package com.write.Quill.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.write.Quill.R;
import com.write.Quill.data.Book;

import name.vbraun.view.write.Page;

public class ExportThread extends Thread {
	private final static String TAG = "ThreadBase";
	
	protected SendDialog fragment;
	protected int progress = 0;
	private ArrayList<File> result = null;
	
	protected final Book book;
	protected final LinkedList<Page> pages;
	protected final File dir;
	
	protected ExportThread(Book book, LinkedList<Page> pages, File dir) {
		this.dir = dir;
		this.book = book;
		this.pages = pages;
	}
	
	protected void incrementProgress() {
		progress++;
	}
	
	protected synchronized void setSendDialog(SendDialog sendDialog) {
		this.fragment = sendDialog;
	}
	
	protected synchronized void toast(int resId, Object... values) {
		if (fragment != null)
			fragment.toast(resId, values);
	}

	protected synchronized void toast(String s) {
		if (fragment != null)
			fragment.toast(s);
	}

	public void run() {
		renderPages();
	}

	public ArrayList<File> getFileList() {
		return result;
	}

	public boolean isFinished() {
		return result != null;
	}
	
	private final int width = 800;
	private final int height = 1280;


	protected void renderPages() {
		final ArrayList<File> fileList = new ArrayList<File>();
		if (dir == null) {
			toast(R.string.export_evernote_err_no_cache_dir);
			return;
		}
		ListIterator<Page> iter = pages.listIterator();
		FileOutputStream outStream;
		Log.d(TAG, dir.getAbsolutePath());
		progress = 0;
		while (iter.hasNext()) {
			if (isInterrupted())
				return;
			UUID id = UUID.randomUUID();
			File file = new File(dir, id.toString() + ".png");
			incrementProgress();
			Log.e(TAG, "Writing file " + file.toString());
			try {
				outStream = new FileOutputStream(file);
			} catch (IOException e) {
				Log.e(TAG, "Error writing file " + e.toString());
				toast(R.string.export_evernote_err_cannot_write,
						file.toString());
				return;
			}
			Page page = iter.next();
			int w, h;
			if (page.getAspectRatio() <= 1) {
				w = width;
				h = height;
			} else {
				w = height;
				h = width;
			}
			Bitmap bitmap = page.renderBitmap(w, h, false);
			bitmap.compress(CompressFormat.PNG, 0, outStream);
			try {
				outStream.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing file " + e.toString());
				toast(R.string.export_evernote_err_cannot_close,
						file.toString());
				return;
			}
			file.deleteOnExit();
			fileList.add(file);
		}
		result = fileList;
	}


}
