package com.write.Quill.image;

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
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ImageActivity 
	extends 
		ActivityBase 
	implements 
		OnClickListener, 
		OnCheckedChangeListener {

	private static final String TAG = "ImageActivity";

	private View layout;
	private CropImageView preview;
	private Menu menu;
	private MenuItem menuAspect;
	private Button buttonSave, buttonErase;
	private ImageButton buttonRotateLeft, buttonRotateRight;
	private CheckBox checkBoxCrop;
	private TextView noImageText;

	private Bookshelf bookshelf = null;
	private Book book = null;
	private UUID uuid = null;

	private Bitmap bitmap;
	private File photoFile = null;

	// persistent data
	protected Uri uri = null;
	protected int rotation;
	protected boolean constrainAspect;

	public final static String EXTRA_UUID = "extra_uuid";
	public final static String EXTRA_ROTATION = "extra_rotation";
	public final static String EXTRA_CONSTRAIN_ASPECT = "extra_constrain_aspect";
	public final static String EXTRA_FILE_URI = "extra_file_uri";
	public final static String EXTRA_PHOTO_TMP_FILE = "extra_photo_tmp_file";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bookshelf = Bookshelf.getBookshelf();
		book = Bookshelf.getCurrentBook();

		layout = getLayoutInflater().inflate(R.layout.image_editor, null);
		setContentView(layout);

		preview = (CropImageView) findViewById(R.id.image_editor_preview);
		buttonSave = (Button) findViewById(R.id.image_editor_save);
		buttonErase = (Button) findViewById(R.id.image_editor_erase);
		buttonRotateLeft  = (ImageButton) findViewById(R.id.image_editor_rotate_left);
		buttonRotateRight = (ImageButton) findViewById(R.id.image_editor_rotate_right);
		checkBoxCrop = (CheckBox) findViewById(R.id.image_editor_check_crop);
		noImageText = (TextView) findViewById(R.id.image_editor_no_image_text);

		buttonSave.setOnClickListener(this);
		buttonErase.setOnClickListener(this);
		buttonRotateLeft.setOnClickListener(this);
		buttonRotateRight.setOnClickListener(this);

		ActionBar bar = getActionBar();
		bar.setTitle(R.string.image_editor_title);
		bar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		if (savedInstanceState != null)
			restoreFrom(savedInstanceState);
		else
			restoreFrom(intent.getExtras());
	}

	private void restoreFrom(Bundle bundle) {
		if (bundle == null) return;
		String uuidStr = bundle.getString(EXTRA_UUID);
		if (uuidStr != null)
			uuid = UUID.fromString(uuidStr);		
		else {
			Log.e(TAG, "no image uuid set");
			return;
		}
		constrainAspect = bundle.getBoolean(EXTRA_CONSTRAIN_ASPECT, true);
		rotation = bundle.getInt(EXTRA_ROTATION, 0);
		String uriStr = bundle.getString(EXTRA_FILE_URI);
		if (uriStr != null) {
			uri = Uri.parse(uriStr);
			loadBitmap();
		}
		if (bundle.containsKey(EXTRA_PHOTO_TMP_FILE))
			photoFile = new File(bundle.getString(EXTRA_PHOTO_TMP_FILE));
	}

	private Bundle saveTo(Bundle bundle) {
		Log.d(TAG, "saveTo");
		if (uri != null)
			bundle.putString(EXTRA_FILE_URI, uri.toString());
        bundle.putString(EXTRA_UUID, uuid.toString());
		bundle.putInt(EXTRA_ROTATION, rotation);
		bundle.putBoolean(EXTRA_CONSTRAIN_ASPECT, constrainAspect);
		if (photoFile != null) 
			bundle.putString(EXTRA_PHOTO_TMP_FILE, photoFile.getAbsolutePath());
		return bundle;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveTo(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.image_editor, menu);
		this.menu = menu;
        menuAspect = menu.findItem(R.id.image_editor_aspect);
		menuAspect.setChecked(constrainAspect);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkBoxCrop.setChecked(false);
		checkBoxCrop.setOnCheckedChangeListener(this);
		if (menuAspect != null)
			menuAspect.setChecked(constrainAspect);
		showImageTools(uri != null);
	}

	private void showImageTools(boolean show) {
		noImageText.setVisibility(show ? View.GONE : View.VISIBLE);
		buttonSave.setEnabled(show);
		buttonRotateLeft.setEnabled(show);
		buttonRotateRight.setEnabled(show);
		checkBoxCrop.setEnabled(show);
	}
	
	@Override
	protected void onPause() {
		checkBoxCrop.setOnCheckedChangeListener(null);
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
			startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
			return true;
		case R.id.image_editor_photo:
			intent = new Intent();
			intent.setAction("android.media.action.IMAGE_CAPTURE");
			photoFile = new File(Environment.getExternalStorageDirectory(),
					uuid.toString() + ".jpg");
			photoFile.deleteOnExit();
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
			startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
			return true;
		case R.id.image_editor_aspect:
			constrainAspect = !constrainAspect;
            menuAspect.setChecked(constrainAspect);
		default:
			return super.onOptionsItemSelected(item);
		}
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
			if (photoFile == null || !photoFile.exists()) {
				photoFile = null;
				Toast.makeText(this, R.string.image_editor_err_no_photo,
						Toast.LENGTH_LONG).show();
				Log.e(TAG, "no photo");
				return;
			}
			loadBitmap(Uri.fromFile(photoFile), 0);
			photoFile = null;
			break;
		case REQUEST_CODE_PICK_IMAGE:
			if (resultCode != RESULT_OK)
				break;

			Uri selectedImage = intent.getData();
			if (selectedImage == null) {
				Log.e(TAG, "Selected image is NULL!");
				return;
			} else {
				Log.d(TAG, "Selected image: "+selectedImage);
			}

			if (selectedImage.toString().startsWith(
					"content://com.android.gallery3d.provider")) {
				// some devices/OS versions return an URI of com.android instead
				// of com.google.android
				String str = selectedImage.toString()
						.replace("com.android.gallery3d", "com.google.android.gallery3d");
				selectedImage = Uri.parse(str);
				Log.d(TAG, "Rewrote to: "+selectedImage);
			}
			boolean picasaImage = selectedImage.toString().startsWith(
					"content://com.google.android.gallery3d");
			
			final String[] filePathColumn = { MediaColumns.DATA,
					MediaColumns.DISPLAY_NAME };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			if (cursor == null) {
				Toast.makeText(this, R.string.image_editor_err_no_such_image, Toast.LENGTH_LONG)
						.show();
				Log.e(TAG, "cursor is null");
				return;
			}
			
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
					Toast.makeText(this, R.string.image_editor_err_permissions,
							Toast.LENGTH_LONG).show();
					Log.e(TAG, "image file not readable");
					return;
				}
				loadBitmap(Uri.fromFile(file), 0);
			}
			cursor.close();
			break;
		default:
			Assert.fail("Unknown request code");
		}
	}

	
	protected void loadBitmap(Uri sourceUri, int rotation ) {
		this.uri = sourceUri;
		this.rotation = rotation;
		loadBitmap();
	}
	
	private void loadBitmap() {
		if (uri == null) return;
		Log.i(TAG, "showing "+uri);
		bitmap = Util.getBitmap(getContentResolver(), uri);		
		bitmap = Util.rotate(bitmap, rotation);
		preview.setImageBitmapResetBase(bitmap, true);
	}
	
	private File getBookImageFile() {
		Storage storage = Storage.getInstance();
		File dir = storage.getBookDirectory(book);
		return new File(dir, getImageFileName());
	}

	private void downloadImage(final Uri uri) {
		DialogFragment newFragment = DialogPicasaDownload.newInstance(uri, getCacheFile());
		newFragment.show(getFragmentManager(), "downloadImage");
	}
		
	private String getImageFileName() {
		return Util.getImageFileName(uuid);
	}

	private File getCacheFile() {
 		String randomFileName = getImageFileName();
		File file = new File(getCacheDir(), randomFileName);
		file.deleteOnExit();
		return file;
	}
	
	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.image_editor_erase:
			intent = new Intent();
	        intent.putExtra(EXTRA_UUID, uuid.toString());
			setResult(RESULT_OK, intent);
			finish();
			break;
		case R.id.image_editor_save:
			DialogFragment newFragment = 
				DialogSave.newInstance(uri, getBookImageFile(), 
						rotation, preview.getCropRect());
			newFragment.show(getFragmentManager(), "saveImage");
			break;
		case R.id.image_editor_rotate_right:
			addToRotation(90);
			break;
		case R.id.image_editor_rotate_left:
			addToRotation(270);
			break;
		}
	}
	
	private void addToRotation(int degrees) {
		rotation = (rotation + degrees) % 360;
		bitmap = Util.rotate(bitmap, degrees);
		preview.setImageBitmapResetBase(bitmap, true);
		checkBoxCrop.setChecked(false);
		preview.setHighlight(null);		
	}
	
	@Override
	public void onCheckedChanged(CompoundButton button, boolean isChecked) {
		if (button == checkBoxCrop) {
			if (isChecked)
				makeHighlight();
			else
				preview.setHighlight(null);
		}
	}
		
	private void makeHighlight() {
		if (bitmap == null) return;
		HighlightView hv = new HighlightView(preview);
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Rect imageRect = new Rect(0, 0, width, height);

		// make the default size about 4/5 of the width or height
		int cropWidth = Math.min(width, height) * 4 / 5;
		int cropHeight = cropWidth;
		int x = (width - cropWidth) / 2;
		int y = (height - cropHeight) / 2;
		RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
		hv.setup(preview.getImageMatrix(), imageRect, cropRect, false, false);
		hv.setFocus(true);
		preview.setHighlight(hv);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		setResult(RESULT_CANCELED, intent);
		finish();
	}

	/**
	 * Callback from the save progress dialog
	 */
	protected void onSaveFinished(File file) {
		Intent intent = new Intent();
        intent.putExtra(EXTRA_FILE_URI, Uri.fromFile(file).toString());
        intent.putExtra(EXTRA_UUID, uuid.toString());
		intent.putExtra(EXTRA_CONSTRAIN_ASPECT, constrainAspect);
		setResult(RESULT_OK, intent);
		finish();
	}
	
}
