package com.write.Quill.sync;

import java.util.ArrayList;
import java.util.List;

import name.vbraun.view.write.GraphicsControlpoint.Controlpoint;

import android.annotation.TargetApi;
import android.app.Activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

@TargetApi(14)
public class UserProfileICS extends UserProfile {

	private final static String TAG = "UserProfileICS";

	private String profile_name, profile_email;

	protected UserProfileICS(Activity activity) {
		Log.e(TAG, "UserProfileICS");
		initialize(activity);
	}

	@Override
	String name() {
		return profile_name;
	}

	@Override
	String email() {
		return profile_email;
	}

	private interface QueryName {
		String[] PROJECTION = {
				ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.StructuredName.IS_PRIMARY };
		String SELECTION = ContactsContract.Contacts.Data.MIMETYPE + " = ?";
		String[] ARGS = { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE };
		int NAME = 0;
		int IS_PRIMARY = 1;
	}

	private interface QueryEmail {
		String[] PROJECTION = { ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY };
		String SELECTION = ContactsContract.Contacts.Data.MIMETYPE + " = ?";
		String[] ARGS = { ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE };
		int ADDRESS = 0;
		int IS_PRIMARY = 1;
	}

	/**
	 * Read profile data
	 * 
	 * @param activity
	 *            The activity
	 */
	private void initialize(Activity activity) {
		ContentResolver cr = activity.getContentResolver();
		Uri profile_uri = Uri.withAppendedPath(
				ContactsContract.Profile.CONTENT_URI,
				ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
		 
		Cursor cursor;
		cursor = cr.query(profile_uri, QueryEmail.PROJECTION,
				QueryEmail.SELECTION, QueryEmail.ARGS, null);
		Log.e(TAG, "cursor "+cursor.getCount());
		cursor.moveToFirst();
		boolean havePrimary = false;
		while (!cursor.isAfterLast()) {
			//Log.e(TAG, "email = " + cursor.getString(QueryEmail.ADDRESS) + " "
			//		+ cursor.getString(QueryEmail.IS_PRIMARY));
			boolean primary = (cursor.getInt(QueryEmail.IS_PRIMARY) != 0);
			String email = cursor.getString(QueryEmail.ADDRESS);
			if (primary || profile_email == null
					|| (!havePrimary && email.contains("gmail")))
				profile_email = email;
			havePrimary |= primary;
			cursor.moveToNext();
		}
		cursor.close();
		if (profile_email == null)
			profile_email = "my.address@gmail.com";
		
		cursor = cr.query(profile_uri, QueryName.PROJECTION,
				QueryName.SELECTION, QueryName.ARGS, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			// Log.e(TAG, "name = " + cursor.getString(QueryName.NAME) + " "
			//		+ cursor.getString(QueryName.IS_PRIMARY));
			boolean primary = (cursor.getInt(QueryName.IS_PRIMARY) > 0);
			if (primary || profile_name == null)
				profile_name = cursor.getString(QueryName.NAME);
			cursor.moveToNext();
		}
		cursor.close();
		if (profile_name == null)
			profile_name = "Firstname Lastname";
	}

}
