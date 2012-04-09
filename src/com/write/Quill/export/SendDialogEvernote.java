package com.write.Quill.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import name.vbraun.view.write.Page;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.TagManager.Tag;

public class SendDialogEvernote extends SendDialogImage {
	private final static String TAG = "SendDialogEvernote";

	public static SendDialogEvernote newInstance(Book book,
			LinkedList<Page> pages) {
		SendDialogEvernote fragment = new SendDialogEvernote();
		Bundle args = SendDialog.storePages(book, pages);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	protected String getTitle() {
		return getString(R.string.export_evernote_title);
	}

	@Override
	protected String getMessage() {
		return getString(R.string.export_evernote_message);
	}

	@Override
	protected String getCancelMessage() {
		return getString(R.string.export_evernote_cancel_message);
	}

	// Names of Evernote-specific Intent actions and extras
	public static final String ACTION_NEW_NOTE = "com.evernote.action.CREATE_NEW_NOTE";
	public static final String EXTRA_NOTE_GUID = "NOTE_GUID";
	public static final String EXTRA_SOURCE_APP = "SOURCE_APP";
	public static final String EXTRA_AUTHOR = "AUTHOR";
	public static final String EXTRA_QUICK_SEND = "QUICK_SEND";
	public static final String EXTRA_TAGS = "TAG_NAME_LIST";

	protected void send(ArrayList<File> fileList) {
		final ArrayList<Uri> uriList = uriListFromFiles(fileList);

		Intent intent = new Intent();
		intent.setAction(ACTION_NEW_NOTE);
		intent.putExtra(Intent.EXTRA_TITLE, book.getTitle());
		// intent.putExtra(Intent.EXTRA_TEXT, text);

		// Add tags, which will be created if they don't exist
		intent.putExtra(EXTRA_TAGS, getTags());
		UUID uuid = book.getUUID();
		if (uuid != null) {
			String notebookGuid = uuid.toString();
			intent.putExtra(EXTRA_NOTE_GUID, uuid);
		}

		intent.putExtra(EXTRA_SOURCE_APP, "Quill");
		intent.putExtra(EXTRA_QUICK_SEND, true);
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
		try {
			getActivity().startActivity(intent);
		} catch (android.content.ActivityNotFoundException ex) {
			toast(R.string.export_evernote_err_not_found);
		}
	}

}
