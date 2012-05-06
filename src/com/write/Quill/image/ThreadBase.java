package com.write.Quill.image;

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

public abstract class ThreadBase extends Thread {
	private final static String TAG = "ThreadBase";
	
	protected DialogBase fragment;
	protected int progress = 0;
	protected boolean finished = false;
		
	protected final File file;
	
	protected ThreadBase(File file) {
		this.file = file;
	}
	
	protected void incrementProgress() {
		progress++;
	}
	
	protected int getProgress() {
		return progress;
	}
	
	protected synchronized void setDialog(DialogBase dialog) {
		this.fragment = dialog;
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
		worker();
		finished = true;
	}

	public boolean isFinished() {
		return finished;
	}
	
	protected void debuggingDelay() {
		return;
//		try {
//			sleep(100);
//		} catch (InterruptedException e) {}
	}

	
	abstract protected void worker();

}
