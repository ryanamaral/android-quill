package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import com.write.Quill.R;
import com.write.Quill.Page.PaperType;

import android.app.ActionBar;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;


public class QuillWriterActivity extends Activity {
	private static final String TAG = "Quill";
    private static final String FILENAME_PREFERENCES = "preferences";
	public static final int DIALOG_COLOR = 1;
	public static final int DIALOG_THICKNESS = 2;
	public static final int DIALOG_PAPER_ASPECT = 3;
	public static final int DIALOG_PAPER_TYPE = 4;

	private Book book = Book.getBook();
	
    private HandwriterView mView;
    private Menu mMenu;
    private Toast mToast;
    private Button tagButton;
    
    private boolean volumeKeyNavigation;

    private static final String HAVE_BOOK = "have_book";
    
    @Override
	public void onSaveInstanceState(Bundle state) {
    	state.putBoolean(HAVE_BOOK, true);
    }
    
    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        // Create and attach the view that is responsible for painting.
        mView = new HandwriterView(this);
        setContentView(mView);
        
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));

        tagButton = new Button(this);
        tagButton.setText(R.string.tag_button);
        bar.setCustomView(tagButton);
        bar.setDisplayShowCustomEnabled(true);

      	tagButton.setOnClickListener(
        new OnClickListener() {
            public void onClick(View v) {
            	Intent i = new Intent(getApplicationContext(), TagsListActivity.class);    
            	startActivity(i);
            }
        });


        if (savedInstanceState != null &&
        	savedInstanceState.getBoolean(HAVE_BOOK)) {
        	Log.v(TAG, "Got book handed through.");
        	book = Book.getBook();
        } else {
        	Log.v(TAG, "Reading book from storage.");
        	book.load(getApplicationContext());
        }
        assert (book != null) : "Book object not initialized.";
    	mView.set_page_and_zoom_out(book.current_page());       
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    	case DIALOG_THICKNESS:
    		return (Dialog)create_dialog_thickness();
    	case DIALOG_COLOR:
    		return (Dialog)create_dialog_color();
    	case DIALOG_PAPER_ASPECT:
    		return (Dialog)create_dialog_paper_aspect();
    	case DIALOG_PAPER_TYPE:
    		return (Dialog)create_dialog_paper_type();
    	}
    	return null;
    }
    
    private Dialog create_dialog_thickness() { 
    	final CharSequence[] items = {"Single pixel", "Ultra-fine", "Thin", "Medium", "Thick", "Giant"};
    	final int[] actual_thickness = {0, 1, 2, 5, 12, 40};
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Pen thickness");
    	int pen_thickness_index = 0;
    	for (int i=0; i<actual_thickness.length; i++)
    		if (actual_thickness[i] == mView.pen_thickness)
    			pen_thickness_index = i;
    	builder.setSingleChoiceItems(items, pen_thickness_index, 
    			new DialogInterface.OnClickListener() {
    	    		public void onClick(DialogInterface dialog, int item) {
    	    			Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	            	mView.set_pen_thickness(actual_thickness[item]);
    	            	dialog.dismiss();
    	    		}
    			});
    	return builder.create();
    }    	

    private Dialog create_dialog_paper_aspect() { 
    	final CharSequence[] items = new CharSequence[AspectRatio.Table.length];
    	final float[] values = new float[AspectRatio.Table.length];
    	for (int i=0; i<AspectRatio.Table.length; i++) {
    		items[i] = AspectRatio.Table[i].name;
    		values[i] = AspectRatio.Table[i].ratio;
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Paper type");
    	int select_item = -1;
    	for (int i=0; i<values.length; i++)
    		if (values[i] == mView.page.aspect_ratio)
    			select_item = i;
    	builder.setSingleChoiceItems(items, select_item, 
    			new DialogInterface.OnClickListener() {
    	    		public void onClick(DialogInterface dialog, int item) {
    	    			if (item == -1) return;
    	    			Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	    			mView.set_page_aspect_ratio(values[item]);
    	            	dialog.dismiss();
    	    		}
    			});
    	return builder.create();
    }    
    
    private Dialog create_dialog_paper_type() { 
    	final CharSequence[] items = new CharSequence[Page.PaperTypes.length];
    	final Page.PaperType[] values = new Page.PaperType[Page.PaperTypes.length];
    	for (int i=0; i<Page.PaperTypes.length; i++) {
    		items[i] = Page.PaperTypes[i].name;
    		values[i] = Page.PaperTypes[i].type;
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Paper type");
    	int select_item = -1;
    	for (int i=0; i<values.length; i++)
    		if (values[i] == mView.page.paper_type)
    			select_item = i;
    	builder.setSingleChoiceItems(items, select_item, 
    			new DialogInterface.OnClickListener() {
    	    		public void onClick(DialogInterface dialog, int item) {
    	    			if (item == -1) return;
    	    			Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	    			mView.set_page_paper_type(Page.PaperType.values()[item]);
    	    			dialog.dismiss();
    	    		}
    			});
    	return builder.create();
    }    	

    // The HandWriterView is not focussable and therefore does not receive KeyEvents
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		Log.v(TAG, "KeyEvent "+action+" "+keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_UP) {
				flip_page_prev();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				flip_page_next();
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
    }

    private Dialog create_dialog_color() {
        AmbilWarnaDialog dlg = new AmbilWarnaDialog(QuillWriterActivity.this, mView.pen_color, 
        	new OnAmbilWarnaListener()
        	{	
        		@Override
        		public void onCancel(AmbilWarnaDialog dialog) {
        		}
        		@Override
        		public void onOk(AmbilWarnaDialog dialog, int color) {
        			mView.set_pen_color(color);
        		}
        	});
        dlg.viewSatVal.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        return dlg.getDialog();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        mMenu = menu;
        menu_prepare_page_has_changed();
        return true;
    }
    
    protected static final int ACTIVITY_PREFERENCES = 0;
    protected static final int ACTIVITY_TAG_PAGE = 1;
    protected static final int ACTIVITY_TAG_FILTER = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.v(TAG, "onActivityResult "+requestCode+" "+resultCode);
    	switch (requestCode) {
    	case ACTIVITY_PREFERENCES:
    		if (resultCode == Preferences.RESULT_RESTORE_BACKUP) {
    			String filename = (String)data.getCharSequenceExtra(Preferences.RESULT_FILENAME);
    			try {
    				book.loadArchive(new File(filename));
    			} catch (IOException e) {
    				Log.e(TAG, "Error loading the backup file, sorry");
    				return;
    			}
    		}
        	mView.set_page_and_zoom_out(book.current_page());
        	return;
   		case ACTIVITY_TAG_FILTER:
   		case ACTIVITY_TAG_PAGE:
        	mView.updateOverlay();
        	return;
    	}
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	Intent i;
    	switch (item.getItemId()) {
    	case R.id.settings:
    		i = new Intent(QuillWriterActivity.this, Preferences.class);
    		startActivityForResult(i, ACTIVITY_PREFERENCES);
    		return true;
    	case R.id.fountainpen:
    		mView.set_pen_type(Stroke.PenType.FOUNTAINPEN);
    		item.setEnabled(true);
    		return true;
    	case R.id.pencil:
    		mView.set_pen_type(Stroke.PenType.PENCIL);
    		return true;
    	case R.id.eraser:
    		mView.set_pen_type(Stroke.PenType.ERASER);
    		return true;
    	case R.id.move:
    		mView.set_pen_type(Stroke.PenType.MOVE);
    		return true;
    	case R.id.width:
    		showDialog(DIALOG_THICKNESS);
    		return true;
    	case R.id.color:        	
    		showDialog(DIALOG_COLOR);
    		return true;
    	case R.id.readonly:        	
    		item.setChecked(!item.isChecked());
    		book.current_page().set_readonly(item.isChecked());
    		return true;
    	case R.id.page_aspect:
    		showDialog(DIALOG_PAPER_ASPECT);
    		return true;
    	case R.id.page_type:
    		showDialog(DIALOG_PAPER_TYPE);
    		return true;
    	case R.id.prev:
    	case R.id.page_prev:
    		flip_page_next();
    		return true;
    	case R.id.next:
    	case R.id.page_next:
    		flip_page_prev();
    		return true;
    	case R.id.page_last:
    		mView.set_page_and_zoom_out(book.last_page());
    		menu_prepare_page_has_changed();
    		return true;	
    	case R.id.page_insert:
    		mView.set_page_and_zoom_out(book.insert_page()); 
    		menu_prepare_page_has_changed();
    		return true;
    	case R.id.page_clear:
    		mView.clear();
    		return true;
    	case R.id.page_delete:
    		toast_page_number("Deleted page "+(book.currentPage+1)+" / "+book.pages.size());
    		mView.set_page_and_zoom_out(book.delete_page());
    		menu_prepare_page_has_changed();
    		return true;
    	case R.id.export:
    		Intent mExportIntent = new Intent(QuillWriterActivity.this, ExportActivity.class);
    		startActivity(mExportIntent);
    		return true;
    	case R.id.tag_page:
    		i = new Intent(getApplicationContext(), TagsListActivity.class);    
        	startActivityForResult(i, ACTIVITY_TAG_PAGE);
    		return true;
    	case R.id.tag_filter:
    	case android.R.id.home:
    		i = new Intent(getApplicationContext(), OverviewActivity.class);    
        	startActivityForResult(i, ACTIVITY_TAG_FILTER);
    		return true;
   	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

    private void flip_page_next() {
    	if (book.is_first_page()) 
    		toast_page_number("Already on first page"); 
		else	
			mView.set_page_and_zoom_out(book.previous_page());
			if (book.is_first_page()) 
				toast_page_number("Showing first page"); 
			else
				toast_page_number("Showing page "+(book.currentPage+1)+" / "+book.pages.size());
 		menu_prepare_page_has_changed();
   	
    }
    
    private void flip_page_prev() {
		if (book.is_last_page()) {
			mView.set_page_and_zoom_out(book.insert_page());
			toast_page_number("Inserted new page at end");
		} else {
			mView.set_page_and_zoom_out(book.next_page());
			if (book.is_last_page())
				toast_page_number("Showing last page");
			else 
				toast_page_number("Showing page "+(book.currentPage+1)+" / "+book.pages.size());
		}
		menu_prepare_page_has_changed();
    }
    
    private void toast_page_number(String s) {
    	if (mToast == null)
        	mToast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
    	else {
    		mToast.setText(s);
    	}
    	mToast.show();
    }
    
    private void menu_prepare_page_has_changed() {
    	mMenu.findItem(R.id.readonly).setChecked(book.current_page().is_readonly);
		boolean first = (book.is_first_page());
		boolean last  = (book.is_last_page());
		mMenu.findItem(R.id.page_prev).setEnabled(!first);
		mMenu.findItem(R.id.page_next).setEnabled(!last); 
    }
    
    @Override protected void onResume() {
        super.onResume();
        String model = android.os.Build.MODEL;
        Log.v(TAG, "Model = >"+model+"<");
        // TODO set defaults
        
        // Restore preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	mView.set_pen_color(settings.getInt("pen_color", mView.pen_color));
    	mView.set_pen_thickness(settings.getInt("pen_thickness", mView.pen_thickness));
    	int type = settings.getInt("pen_type", mView.pen_type.ordinal());
    	mView.set_pen_type(Stroke.PenType.values()[type]);
    	mView.only_pen_input = settings.getBoolean("only_pen_input", true);
    	mView.doubleTapWhileWriting = settings.getBoolean("double_tap_while_write", true);
    	volumeKeyNavigation = settings.getBoolean("volume_key_navigation", true);
    	Log.d(TAG, "only_pen_input: "+mView.only_pen_input);
    	mView.requestFocus();
    }
    
    @Override protected void onPause() {
        super.onPause();
        book.save(getApplicationContext());
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("pen_type", mView.pen_type.ordinal());
        editor.putInt("pen_color", mView.pen_color);
        editor.putInt("pen_thickness", mView.pen_thickness);
        editor.putBoolean("volume_key_navigation", volumeKeyNavigation);
        editor.putBoolean("only_pen_input", mView.only_pen_input);
        editor.putBoolean("double_tap_while_writing", mView.doubleTapWhileWriting);
        editor.commit();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    }
    
    
}

