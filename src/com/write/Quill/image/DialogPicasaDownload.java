package com.write.Quill.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

import name.vbraun.view.write.GraphicsImage.FileType;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class DialogPicasaDownload extends DialogBase {
	private static final String TAG = "DialogPicasaDownload";

	public static DialogPicasaDownload newInstance(Uri sourceUri, File destination) {
		DialogPicasaDownload fragment = new DialogPicasaDownload();
		Bundle args = DialogBase.storeArgs(sourceUri, destination);
		fragment.setArguments(args);
		return fragment;
	}

	
	@Override
	protected void initProgresDialog(ProgressDialog dialog) {
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setTitle(getTitle());
		dialog.setMessage(getMessage());
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);		
	}

	private String getTitle() {
		return "Downloading...";
	}

	private String getMessage() {
		return "Image is stored in Picasa";
	}

	@Override
	protected String getCancelMessage() {
		return "Cancelled downloading image from Picasa";
	}

	@Override
	protected ThreadBase makeThread(Uri source, File destination) {
        BitmapFactory.Options options = new BitmapFactory.Options();
		InputStream input;
		try {
			input = getActivity().getContentResolver().openInputStream(source);
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
		return new ThreadPicasaDownload(input, destination);
	}

	@Override
	protected void setProgress(int total) {
		ProgressDialog dlg = getProgressDialog();
    	dlg.setProgress(total);
    	int totalKB = total / 1024;
    	dlg.setMessage("Got "+totalKB+"kb");
	}


	/* 
	 * To be called when the dialog is finished. Use the result here
	 */
	@Override
	protected void onFinish(File file) {		
		ImageActivity activity = (ImageActivity) getActivity();
		activity.loadBitmap(Uri.fromFile(file), 0);
	}


	
	
}
