package com.write.Quill.ImageEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Storage;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import name.vbraun.view.write.GraphicsImage.FileType;

public class ImageActivity
	extends ActivityBase
	implements OnClickListener {

	private static final String TAG = "ImageActivity";

	private View layout;
	private TextView textSourceUri;
	private FrameLayout preview;
	private ImageView imageView;
	private Menu menu;
	private MenuItem menuCropped;
	private Button buttonOK, buttonCancel;
	private CheckBox aspectCheckbox;

	private Bookshelf bookshelf = null;
	private Book book = null;

	private File sourceFile = null;
	private File imageFile = null;

	// persistent data
	protected Uri sourceUri = null;
	protected UUID uuid = null;
	protected boolean constrainAspect = false;
	protected FileType fileType = FileType.FILETYPE_NONE;

	public final static String ACTION_NEW_IMAGE = "action_new_image";
	public final static String ACTION_EDIT_EXISTING_IMAGE = "action_edit_existing_image";
	
	public final static String EXTRA_SOURCE_URI = "extra_source_uri";
	public final static String EXTRA_UUID = "extra_uuid";
	public final static String EXTRA_CONSTRAIN_ASPECT = "extra_constrain_aspect";
	public final static String EXTRA_FILE_TYPE = "extra_file_type";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bookshelf = Bookshelf.getBookshelf();
		book = Bookshelf.getCurrentBook();

		layout = getLayoutInflater().inflate(R.layout.image_editor, null);
		setContentView(layout);

		preview = (FrameLayout) findViewById(R.id.image_editor_preview);
		textSourceUri = (TextView) findViewById(R.id.image_editor_source_uri);
		buttonOK = (Button) findViewById(R.id.image_editor_ok);
		buttonCancel = (Button) findViewById(R.id.image_editor_cancel);
		aspectCheckbox = (CheckBox) findViewById(R.id.image_editor_aspect_checkbox);
		buttonOK.setOnClickListener(this);
		buttonCancel.setOnClickListener(this);

		ActionBar bar = getActionBar();
		bar.setTitle(R.string.image_editor_title);
		bar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		String action = null;
		if (intent != null)
			action = intent.getAction();
		if (action == null || action.equals(ACTION_NEW_IMAGE))
			initNewImage();
		else if (action.equals(ACTION_EDIT_EXISTING_IMAGE))
			initFromIntent(intent);
		else 
			initNewImage();
			//Assert.fail("Unknown action");
	}

	private void initNewImage() {
		uuid = UUID.randomUUID();
	}

	private void initFromIntent(Intent intent) {
		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.image_editor, menu);
		this.menu = menu;
		menuCropped = menu.findItem(R.id.image_editor_capture_cropped);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.image_editor_pick:
			intent = new Intent();
			intent.setAction(Intent.ACTION_PICK);
			intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			addCropToMediaStoreIntent(intent);
			startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
			return true;
		case R.id.image_editor_photo:
			intent = new Intent();
			intent.setAction("android.media.action.IMAGE_CAPTURE");
			addCropToMediaStoreIntent(intent);
			startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
			return true;
		case R.id.image_editor_capture_cropped:
			menuCropped.setChecked(!menuCropped.isChecked());
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void addCropToMediaStoreIntent(Intent intent) {
		if (menuCropped.isChecked()) 
			intent.putExtra("crop", "true");
		imageFile = new File(Environment.getExternalStorageDirectory(),
				uuid.toString() + ".jpg");
		imageFile.deleteOnExit();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
	}

	
	private final static int REQUEST_CODE_PICK_IMAGE = 1;
	private final static int REQUEST_CODE_TAKE_PHOTO = 2;

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case REQUEST_CODE_TAKE_PHOTO:
			if (resultCode != RESULT_OK)
				break;
			if (!imageFile.exists()) {
				imageFile = null;
				Toast.makeText(this, "Camera did not capture photo!", Toast.LENGTH_LONG).show();
				Log.e(TAG, "no photo");
				return;
			}
			setSourceUri(Uri.fromFile(imageFile));
			break;
		case REQUEST_CODE_PICK_IMAGE:
			if (resultCode != RESULT_OK)
				break;

			Uri selectedImage = intent.getData();
			final String[] filePathColumn = { MediaColumns.DATA,
					MediaColumns.DISPLAY_NAME };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			if (cursor == null) {
				Toast.makeText(this, "No such image!", Toast.LENGTH_LONG)
						.show();
				Log.e(TAG, "cursor is null");
				return;
			}

			if (selectedImage.toString().startsWith(
					"content://com.android.gallery3d.provider")) {
				// some devices/OS versions return an URI of com.android instead
				// of com.google.android
				String str = selectedImage.toString()
						.replace("com.android.gallery3d",
								"com.google.android.gallery3d");
				selectedImage = Uri.parse(str);
			}
			boolean picasaImage = selectedImage.toString().startsWith(
					"content://com.google.android.gallery3d");

			cursor.moveToFirst();
			if (picasaImage) {
				int columnIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
				if (columnIndex == -1) {
					Log.e(TAG, "no DISPLAY_NAME column");
					return;
				}
				downloadImage(selectedImage);
			} else {
				int columnIndex = cursor.getColumnIndex(MediaColumns.DATA);
				if (columnIndex == -1) {
					Log.e(TAG, "no DATA column");
					return;
				}
				String name = cursor.getString(columnIndex);
				File file = new File(name);
				if (!file.exists() || !file.canRead()) {
					Toast.makeText(this, "Image does not exist, or insufficient permissions!",
							Toast.LENGTH_LONG).show();
					Log.e(TAG, "image file not readable");
					return;
				}
				Uri uri = Uri.fromFile(file);
				setSourceUri(uri);
			}
			cursor.close();
			break;
		default:
			Assert.fail("Unknown request code");
		}
	}

	protected void setSourceUri(Uri uri) {
    	String uriStr = uri.toString();
    	String fileExt;
    	if (uriStr.substring(uriStr.length()-4).equalsIgnoreCase(".jpg")) {
    		fileType = FileType.FILETYPE_JPG;
    		fileExt = ".jpg";
    	} else if (uriStr.substring(uriStr.length()-4).equalsIgnoreCase(".png")) {
    		fileType = FileType.FILETYPE_PNG;
    		fileExt = ".png";
    	} else {
    		fileType = FileType.FILETYPE_NONE;
    		return;
    	}
    	sourceUri = uri;    		
    	textSourceUri.setText(uriStr);
    	
		Storage storage = Storage.getInstance();
		File dir = storage.getBookDirectory(book.getUUID());
		
		imageFile = new File(dir, uuid.toString() + fileExt);
		sourceFile = new File(sourceUri.getPath());
		copyfile(sourceFile, imageFile);
	}
	
	private void updatePreview() {
		imageView = new ImageView(getApplicationContext());
		imageView.setImageURI(Uri.fromFile(imageFile));
		preview.removeAllViews();
		preview.addView(imageView);
    }

	protected Uri getSourceUri() {
		return sourceUri;
	}

	private void downloadImage(final Uri uri) {
		DialogFragment newFragment = DownloadImageFragment.newInstance(uri);
		newFragment.show(getFragmentManager(), "downloadImage");
	}

	private static void copyfile(File source, File dest) {
		try {
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
			case R.id.image_editor_cancel:
		    	intent = new Intent();
		    	setResult(RESULT_CANCELED, intent);
				finish();
				break;
			case R.id.image_editor_ok:
		    	intent = new Intent();
		    	intent.putExtra(RESULT_BACK_KEY_PRESSED, false);
		    	setResult(RESULT_OK, intent);
				finish();
				break;
		}
	}

}
