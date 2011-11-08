package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.Page.PaperType;
import com.write.Quill.Stroke.PenType;

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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
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

	private Book book = null;
	
    private HandwriterView mView;
    private Menu mMenu;
    private Toast mToast;
    private Button tagButton;
    
    private boolean volumeKeyNavigation;

    private static final String HAVE_BOOK = "have_book";
    
    private static final DialogThickness dialogThickness = new DialogThickness();
    private static final DialogAspectRatio dialogAspectRatio = new DialogAspectRatio();
    private static final DialogPaperType dialogPaperType = new DialogPaperType();

    
    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      	Book.onCreate(getApplicationContext());
      	book = Book.getBook();
        Assert.assertTrue("Book object not initialized.", book != null);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        // Create and attach the view that is responsible for painting.
        mView = new HandwriterView(this);
        setContentView(mView);
        
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));

        tagButton = new Button(this);
        tagButton.setText(R.string.tag_button);
        // tagButton.setBackgroundResource(R.drawable.actionbar_background);
        bar.setCustomView(tagButton);
        bar.setDisplayShowCustomEnabled(true);

      	tagButton.setOnClickListener(
        new OnClickListener() {
            public void onClick(View v) {
            	Intent i = new Intent(getApplicationContext(), TagsListActivity.class);    
            	startActivity(i);
            }
        });      	
    	mView.setPageAndZoomOut(book.currentPage());
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	DialogInterface.OnClickListener listener;
    	switch (id) {
    	case DIALOG_THICKNESS:
        	listener = new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int i) {
        			Toast.makeText(getApplicationContext(), 
        				dialogThickness.getItem(i), Toast.LENGTH_SHORT).show();
        			mView.setPenThickness(dialogThickness.getValue(i));
        			updatePenHistoryIcon();
        			dialog.dismiss();
        		}};
    		return dialogThickness.create(this, listener);
    	case DIALOG_COLOR:
   		return (Dialog)create_dialog_color();
    	case DIALOG_PAPER_ASPECT:
			listener = new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int i) {
	    			Toast.makeText(getApplicationContext(), 
	    					dialogAspectRatio.getItem(i), Toast.LENGTH_SHORT).show();
	    			mView.setPageAspectRatio(dialogAspectRatio.getValue(i));
	            	dialog.dismiss();
	    		}};
	    	return dialogAspectRatio.create(this, listener);
    	case DIALOG_PAPER_TYPE:
    		listener = new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int i) {
    				Toast.makeText(getApplicationContext(), 
    					dialogPaperType.getItem(i), Toast.LENGTH_SHORT).show();
    				mView.setPagePaperType(dialogPaperType.getValue(i));
    			dialog.dismiss();
    		}};
    		return dialogPaperType.create(this, listener);
    	}
    	return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dlg) {
    	Log.d(TAG, "onPrepareDialog "+id);
    	switch (id) {
    	case DIALOG_THICKNESS:
    		dialogThickness.setSelectionByValue(mView.pen_thickness);
    		return;
    	case DIALOG_COLOR:
    		return;
    	case DIALOG_PAPER_ASPECT:
    		dialogAspectRatio.setSelectionByValue(mView.page.aspect_ratio);
    		return;
    	case DIALOG_PAPER_TYPE:
    		dialogPaperType.setSelectionByValue(mView.page.paper_type);
    		return;
    	}
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
        			mView.setPenColor(color);
        			updatePenHistoryIcon();
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
    	setActionBarIconActive(mView.pen_type);
    	updatePenHistoryIcon();
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
    				Log.e(TAG, "Error loading th)e backup file, sorry");
    				return;
    			}
    		}
        	mView.setPageAndZoomOut(book.currentPage());
        	return;
   		// case ACTIVITY_TAG_FILTER:
   		// case ACTIVITY_TAG_PAGE:
    	}
    }
    
    protected void setActionBarIconActive(Stroke.PenType penType) {
		updatePenHistoryIcon();
    	mMenu.findItem(R.id.fountainpen).setIcon(R.drawable.ic_menu_quill);
    	mMenu.findItem(R.id.pencil).setIcon(R.drawable.ic_menu_pencil);
    	mMenu.findItem(R.id.move).setIcon(R.drawable.ic_menu_resize);
    	mMenu.findItem(R.id.eraser).setIcon(R.drawable.ic_menu_eraser);
    	switch (penType) {
    	case FOUNTAINPEN:
    		mMenu.findItem(R.id.fountainpen).setIcon(R.drawable.ic_menu_quill_active);
    		return;
    	case PENCIL:
    		mMenu.findItem(R.id.pencil).setIcon(R.drawable.ic_menu_pencil_active);
    		return;
    	case MOVE:
    		mMenu.findItem(R.id.move).setIcon(R.drawable.ic_menu_resize_active);
    		return;
    	case ERASER:
    		mMenu.findItem(R.id.eraser).setIcon(R.drawable.ic_menu_eraser_active);
    		return;
    	}
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	Intent i;
    	switch (item.getItemId()) {
    	case R.id.prev_pen:
    		switchPenHistory();
    		return true;
    	case R.id.settings:
    		i = new Intent(QuillWriterActivity.this, Preferences.class);
    		startActivityForResult(i, ACTIVITY_PREFERENCES);
    		return true;
    	case R.id.fountainpen:
    		mView.setPenType(Stroke.PenType.FOUNTAINPEN);
    		setActionBarIconActive(Stroke.PenType.FOUNTAINPEN);
    		return true;
    	case R.id.pencil:
    		mView.setPenType(Stroke.PenType.PENCIL);
    		setActionBarIconActive(Stroke.PenType.PENCIL);
    		return true;
    	case R.id.eraser:
    		mView.setPenType(Stroke.PenType.ERASER);
    		setActionBarIconActive(Stroke.PenType.ERASER);
    		return true;
    	case R.id.move:
    		mView.setPenType(Stroke.PenType.MOVE);
    		setActionBarIconActive(Stroke.PenType.MOVE);
    		return true;
    	case R.id.width:
    		showDialog(DIALOG_THICKNESS);
    		return true;
    	case R.id.color:        	
    		showDialog(DIALOG_COLOR);
    		return true;
    	case R.id.readonly:        	
    		item.setChecked(!item.isChecked());
    		book.currentPage().setReadonly(item.isChecked());
    		return true;
    	case R.id.page_aspect:
    		showDialog(DIALOG_PAPER_ASPECT);
    		return true;
    	case R.id.page_type:
    		showDialog(DIALOG_PAPER_TYPE);
    		return true;
    	case R.id.prev:
    	case R.id.page_prev:
    		flip_page_prev();
    		return true;
    	case R.id.next:
    	case R.id.page_next:
    		flip_page_next();
    		return true;
    	case R.id.page_prev_unfiltered:
    		flip_page_prev_unfiltered();
    		return true;
    	case R.id.page_next_unfiltered:
    		flip_page_next_unfiltered();
    		return true;
    	case R.id.page_last:
    		mView.setPageAndZoomOut(book.lastPage());
    		menu_prepare_page_has_changed();
    		return true;	
    	case R.id.page_insert:
    		mView.setPageAndZoomOut(book.insertPage()); 
    		menu_prepare_page_has_changed();
    		return true;
    	case R.id.page_clear:
    		mView.clear();
    		return true;
    	case R.id.page_delete:
    		toast_page_number("Deleted page "+(book.currentPage+1)+" / "+book.pages.size());
    		mView.setPageAndZoomOut(book.deletePage());
    		menu_prepare_page_has_changed();
    		return true;
    	case R.id.export:
    		Intent mExportIntent = new Intent(QuillWriterActivity.this, ExportActivity.class);
    		startActivity(mExportIntent);
    		return true;
    	case R.id.tag_page:
    		i = new Intent(getApplicationContext(), TagsListActivity.class);    
        	startActivity(i);
    		return true;
    	case R.id.tag_filter:
    	case android.R.id.home:
    		i = new Intent(getApplicationContext(), OverviewActivity.class);    
        	startActivity(i);
    		return true;
   	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    private void flip_page_prev() {
    	if (book.isFirstPage()) 
    		toast_page_number("Already on first tagged page"); 
		else	
			mView.setPageAndZoomOut(book.previousPage());
			if (book.isFirstPage()) 
				toast_page_number("Showing first tagged page"); 
			else
				toast_page_number("Showing page "+(book.currentPage+1)+" / "+book.pages.size());
 		menu_prepare_page_has_changed();
    }
    
    private void flip_page_next() {
		if (book.isLastPage()) {
			mView.setPageAndZoomOut(book.insertPageAtEnd());
			toast_page_number("Inserted new page at end");
		} else {
			mView.setPageAndZoomOut(book.nextPage());
			if (book.isLastPage())
				toast_page_number("Showing last tagged page");
			else 
				toast_page_number("Showing page "+(book.currentPage+1)+" / "+book.pages.size());
		}
		menu_prepare_page_has_changed();
    }
    
    private void flip_page_prev_unfiltered() {
    	if (book.isFirstPageUnfiltered()) 
    		toast_page_number("Already on first page"); 
		else	
			mView.setPageAndZoomOut(book.previousPageUnfiltered());
			if (book.isFirstPageUnfiltered()) 
				toast_page_number("Showing first page"); 
			else
				toast_page_number("Showing page "+(book.currentPage+1)+" / "+book.pages.size());
 		menu_prepare_page_has_changed();
    }
    
    private void flip_page_next_unfiltered() {
		if (book.isLastPageUnfiltered()) {
			mView.setPageAndZoomOut(book.insertPageAtEnd());
			toast_page_number("Inserted new page at end");
		} else {
			mView.setPageAndZoomOut(book.nextPageUnfiltered());
			if (book.isLastPageUnfiltered())
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
    	mMenu.findItem(R.id.readonly).setChecked(book.currentPage().is_readonly);
		boolean first = (book.isFirstPage());
		boolean last  = (book.isLastPage());
		mMenu.findItem(R.id.page_prev).setEnabled(!first);
		mMenu.findItem(R.id.page_next).setEnabled(!last); 
		boolean first_unfiltered = (book.isFirstPageUnfiltered());
		mMenu.findItem(R.id.page_prev_unfiltered).setEnabled(!first_unfiltered);
    }
    

	private void switchPenHistory() {
		PenHistory h = PenHistory.getPenHistory();
		if (h.size() <= 1) {
			Toast.makeText(getApplicationContext(), 
					"No other pen styles in history.", Toast.LENGTH_SHORT).show();
			return;
		}
		int penHistoryItem = h.nextHistoryItem();
		// Log.d(TAG, "switchPenHistory "+penHistoryItem+" "+h.size());
		mView.setPenColor(h.getColor(penHistoryItem));
		mView.setPenThickness(h.getThickness(penHistoryItem));
		mView.setPenType(h.getPenType(penHistoryItem));
		setActionBarIconActive(h.getPenType(penHistoryItem));
	}
  
    private void updatePenHistoryIcon() {
    	if (mMenu == null) return;
    	MenuItem item = mMenu.findItem(R.id.prev_pen);
    	if (item == null) return;
    	Drawable icon = item.getIcon();
    	if (icon == null) return;
    	
    	int w = icon.getIntrinsicWidth();
    	int h = icon.getIntrinsicHeight();
    	Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    	Canvas c = new Canvas(bitmap);
    	c.drawARGB(0xa0, Color.red(mView.pen_color), 
    			Color.green(mView.pen_color), Color.blue(mView.pen_color));
    	if (mView.pen_type == PenType.FOUNTAINPEN) {
        	final Drawable iconStrokeFountainpen = getResources().getDrawable(R.drawable.ic_pen_fountainpen);
    		iconStrokeFountainpen.setBounds(0, 0, w, h);
    		iconStrokeFountainpen.draw(c);
    	} else if (mView.pen_type == PenType.PENCIL) {
        	final Drawable iconStrokePencil = getResources().getDrawable(R.drawable.ic_pen_pencil);
        	iconStrokePencil.setBounds(0, 0, w, h);
    		iconStrokePencil.draw(c);
    	}
        item.setIcon(new BitmapDrawable(bitmap));
    }

    
    @Override protected void onResume() {
        super.onResume();
        if (book != null) {
        	if (mView.page == book.currentPage()) {
        		book.filterChanged();
        		mView.updateOverlay();
        	} else {
        		mView.setPageAndZoomOut(book.currentPage());
        	}
        }
        
        String model = android.os.Build.MODEL;
        Log.v(TAG, "Model = >"+model+"<");
        // TODO set defaults
        
        // Restore preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
       
    	int penColor = settings.getInt("pen_color", mView.pen_color);
    	int penThickness = settings.getInt("pen_thickness", mView.pen_thickness);
    	int penTypeInt = settings.getInt("pen_type", mView.pen_type.ordinal());
    	Stroke.PenType penType = Stroke.PenType.values()[penTypeInt];
    	mView.setPenColor(penColor);
    	mView.setPenThickness(penThickness);
    	mView.setPenType(penType);
    	PenHistory.add(penType, penThickness, penColor);
    	updatePenHistoryIcon();

    	mView.onlyPenInput = settings.getBoolean("only_pen_input", true);
    	mView.doubleTapWhileWriting = settings.getBoolean("double_tap_while_write", true);
    	volumeKeyNavigation = settings.getBoolean("volume_key_navigation", true);
    	Log.d(TAG, "only_pen_input: "+mView.onlyPenInput);
    	mView.requestFocus();
    }
    
    @Override protected void onPause() {
        super.onPause();
        if (book != null)
        	book.save(getApplicationContext());
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("pen_type", mView.pen_type.ordinal());
        editor.putInt("pen_color", mView.pen_color);
        editor.putInt("pen_thickness", mView.pen_thickness);
        editor.putBoolean("volume_key_navigation", volumeKeyNavigation);
        editor.putBoolean("only_pen_input", mView.onlyPenInput);
        editor.putBoolean("double_tap_while_writing", mView.doubleTapWhileWriting);
        editor.commit();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    }
    
    
//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v,
//                                    ContextMenuInfo menuInfo) {
//      super.onCreateContextMenu(menu, v, menuInfo);
//      MenuInflater inflater = getMenuInflater();
//      inflater.inflate(R.menu.menu, menu);
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
//      switch (item.getItemId()) {
//      default:
//        return super.onContextItemSelected(item);
//      }
//    }
}

