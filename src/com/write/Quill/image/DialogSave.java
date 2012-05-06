package com.write.Quill.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class DialogSave extends DialogBase {
	private final static String TAG = "DialogSave";
	
	@Override
	protected void initProgresDialog(ProgressDialog dialog) {
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setTitle(getTitle());
		dialog.setMessage(getMessage());
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);		
	}

	private String getTitle() {
		return "Saving...";
	}

	private String getMessage() {
		return "Compressing and saving image";
	}

	@Override
	protected String getCancelMessage() {
		return "Cancelled saving image.";
	}
	
	@Override
	protected void setProgress(int progress) {}

	@Override
	protected ThreadBase makeThread(Uri source, File destination) {
		String path = source.getPath();
		if (path == null) return null;
		File sourceFile = new File(path);
		return new ThreadSave(sourceFile, destination);
	}

	@Override
	protected void onFinish(File file) {
		// TODO
	}

}
