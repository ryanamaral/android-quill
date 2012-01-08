package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.net.ssl.HandshakeCompletedListener;

import sheetrock.panda.changelog.ChangeLog;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.HandwriterView;
import name.vbraun.view.write.Page;
import name.vbraun.view.write.ToolHistory;
import name.vbraun.view.write.Stroke;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.HandwriterView.OnStrokeFinishedListener;
import name.vbraun.view.write.ToolHistory.HistoryItem;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;

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
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import android.widget.ToggleButton;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;


public class QuillWriterActivity 
	extends	
		Activity 
	implements 
		name.vbraun.view.write.Toolbox.OnToolboxListener,
		OnStrokeFinishedListener {
	private static final String TAG = "Quill";

	public static final int DIALOG_COLOR = 1;
	public static final int DIALOG_THICKNESS = 2;
	public static final int DIALOG_PAPER_ASPECT = 3;
	public static final int DIALOG_PAPER_TYPE = 4;

	private Bookshelf bookshelf = null;
	private Book book = null;
	
    private HandwriterView mView;
    private Menu mMenu;
    private Toast mToast;
    private Button tagButton;
    
    private boolean volumeKeyNavigation;
    private boolean toolboxIsOnLeft;
    private boolean eraserSwitchesBack;

    private static final DialogThickness dialogThickness = new DialogThickness();
    private static final DialogAspectRatio dialogAspectRatio = new DialogAspectRatio();
    private static final DialogPaperType dialogPaperType = new DialogPaperType();

	private name.vbraun.lib.pen.Hardware hw; 
	private ChangeLog changeLog;
		
    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        changeLog = new ChangeLog(this);
        if (changeLog.firstRun())
            changeLog.getLogDialog().show();

      	Bookshelf.onCreate(getApplicationContext());
      	bookshelf = Bookshelf.getBookshelf();
      	book = Bookshelf.getCurrentBook();
      	book.setOnBookModifiedListener(UndoManager.getUndoManager());
        Assert.assertTrue("Book object not initialized.", book != null);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        ToolHistory history = ToolHistory.getToolHistory();
        history.onCreate(getApplicationContext());

        // Create and attach the view that is responsible for painting.
        mView = new HandwriterView(this);
        setContentView(mView);
        mView.setOnGraphicsModifiedListener(UndoManager.getUndoManager());
        mView.setOnToolboxListener(this);
        mView.setOnStrokeFinishedListener(this);
        
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
        
        Display display = getWindowManager().getDefaultDisplay();
        int w = display.getWidth();
        if (w<600) Log.e(TAG, "A screen width of 600px is required");
        if (w>=800) {
        	createTagButton(bar);
        }
    	switchToPage(book.currentPage());
    }
    
    private void createTagButton(ActionBar bar) {
        tagButton = new Button(this);
        tagButton.setText(R.string.tag_button);
        tagButton.setBackgroundResource(R.drawable.btn_default_holo_light);
        bar.setCustomView(tagButton);
        bar.setDisplayShowCustomEnabled(true);

      	tagButton.setOnClickListener(
        new OnClickListener() {
            public void onClick(View v) {
            	Intent i = new Intent(getApplicationContext(), TagsListActivity.class);    
            	startActivity(i);
            }
        });      	    	
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
        			setPenThickness(dialogThickness.getValue(i));
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
    		dialogThickness.setSelectionByValue(mView.getPenThickness());
    		return;
    	case DIALOG_COLOR:
    		return;
    	case DIALOG_PAPER_ASPECT:
    		dialogAspectRatio.setSelectionByValue(mView.getPageAspectRatio());
    		return;
    	case DIALOG_PAPER_TYPE:
    		dialogPaperType.setSelectionByValue(mView.getPagePaperType());
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
        AmbilWarnaDialog dlg = new AmbilWarnaDialog(QuillWriterActivity.this, mView.getPenColor(), 
        	new OnAmbilWarnaListener()
        	{	
        		@Override
        		public void onCancel(AmbilWarnaDialog dialog) {
        		}
        		@Override
        		public void onOk(AmbilWarnaDialog dialog, int color) {
        			setPenColor(color);
        		}
        	});
   //     dlg.viewSatVal.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        return dlg.getDialog();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        mMenu = menu;
		int w = getWindowManager().getDefaultDisplay().getWidth();
    	if (w>=800) {
    		mMenu.findItem(R.id.undo).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	}
    	if (w>=1280) {
    		mMenu.findItem(R.id.redo).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		mMenu.findItem(R.id.typewriter).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		mMenu.findItem(R.id.prev).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		mMenu.findItem(R.id.next).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	}
        menu_prepare_page_has_changed();
    	setActionBarIconActive(mView.getToolType());
    	updatePenHistoryIcon();
    	updateUndoRedoIcons();
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
    				bookshelf.importBook(new File(filename));
    			} catch (IOException e) {
    				Log.e(TAG, "Error loading th)e backup file, sorry");
    				return;
    			}
    		}
    		book = Bookshelf.getCurrentBook();
    		UndoManager.getUndoManager().clearHistory();
        	switchToPage(book.currentPage());
        	return;
    	}
    }
    
	@Override
	public void onToolboxListener(View view) {
		Log.d(TAG, "onToolboxListener "+view.getId());
		switch (view.getId()) {
		case R.id.toolbox_redbutton:
			break;
		case R.id.toolbox_quill_icon:
    		launchOverviewActivity();
			break;
		case R.id.toolbox_tag:
			launchTagActivity();
			break;
		case R.id.toolbox_menu:
			openOptionsMenu();
			break;
		case R.id.toolbox_undo:
			undo();
			break;
		case R.id.toolbox_redo:
			redo();
			break;
		case R.id.toolbox_fountainpen:
			setActiveTool(Tool.FOUNTAINPEN);
			break;
		case R.id.toolbox_pencil:
			setActiveTool(Tool.PENCIL);
			break;
		case R.id.toolbox_resize:
			setActiveTool(Tool.MOVE);
			break;
		case R.id.toolbox_eraser:
			setActiveTool(Tool.ERASER);
			break;
		case R.id.toolbox_text:
			setActiveTool(Tool.TEXT);
			break;
		case R.id.toolbox_next:
    		flip_page_next();
			break;
		case R.id.toolbox_prev:
    		flip_page_prev();
			break;
		case R.id.toolbox_history_1:
			switchPenHistory();
			break;
		case R.id.toolbox_history_2:
			switchPenHistory(0);
			break;
		case R.id.toolbox_history_3:
			switchPenHistory(1);
			break;
		case R.id.toolbox_history_4:
			switchPenHistory(2);
			break;
		}
	}
	
	@Override
	public void onToolboxColorListener(int color) {
		setPenColor(color);		
	}

	@Override
	public void onToolboxLineThicknessListener(int thickness) {
		setPenThickness(thickness);
	}
  
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) {
    	mView.interrupt();
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
    		setActiveTool(Tool.FOUNTAINPEN);
    		return true;
    	case R.id.pencil:
    		setActiveTool(Tool.PENCIL);
    		return true;
    	case R.id.eraser:
    		setActiveTool(Tool.ERASER);
    		return true;
    	case R.id.move:
    		setActiveTool(Tool.MOVE);
    		return true;
    	case R.id.typewriter:
    		setActiveTool(Tool.TEXT);
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
    		switchToPage(book.lastPage());
    		return true;	
    	case R.id.page_insert:
    		switchToPage(book.insertPage()); 
    		return true;
    	case R.id.page_clear:
    		mView.clear();
    		return true;
    	case R.id.page_delete:
    		toast("Deleted page "+(book.currentPageNumber()+1)+
    				" / "+book.pagesSize());
    		book.deletePage();
    		switchToPage(book.currentPage());
    		return true;
    	case R.id.export:
    		Intent mExportIntent = new Intent(QuillWriterActivity.this, ExportActivity.class);
    		mExportIntent.putExtra("filename", book.getTitle());
    		startActivity(mExportIntent);
    		return true;
    	case R.id.tag_page:
    		launchTagActivity();
    		return true;
    	case R.id.tag_filter:
    	case android.R.id.home:
    		launchOverviewActivity();
    		return true;
    	case R.id.about:
    	    changeLog.getFullLogDialog().show();
    	    return true;
    	case R.id.undo:
    		undo();
    		return true;
    	case R.id.redo:
    		redo();
    		return true;
   	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Actual implementations of actions
    ///////////////////////////////////////////////////////////////////////////////

	private void setActiveTool(Tool tool) {
		mView.setToolType(tool);
		setActionBarIconActive(tool);
	}

    protected void setActionBarIconActive(Tool tool) {
    	if (mMenu == null) return;
		updatePenHistoryIcon();
		MenuItem item_fountainpen = mMenu.findItem(R.id.fountainpen);
		MenuItem item_pencil      = mMenu.findItem(R.id.pencil);
		MenuItem item_move        = mMenu.findItem(R.id.move);
		MenuItem item_eraser      = mMenu.findItem(R.id.eraser);
		MenuItem item_typewriter  = mMenu.findItem(R.id.typewriter);
		item_fountainpen.setIcon(R.drawable.ic_menu_quill);
		item_pencil.setIcon(R.drawable.ic_menu_pencil);
    	item_move.setIcon(R.drawable.ic_menu_resize);
    	item_eraser.setIcon(R.drawable.ic_menu_eraser);
    	item_typewriter.setIcon(R.drawable.ic_menu_text);
    	switch (tool) {
    	case FOUNTAINPEN:
    		item_fountainpen.setIcon(R.drawable.ic_menu_quill_active);
    		return;
    	case PENCIL:
    		item_pencil.setIcon(R.drawable.ic_menu_pencil_active);
    		return;
    	case MOVE:
    		item_move.setIcon(R.drawable.ic_menu_resize_active);
    		return;
    	case ERASER:
    		item_eraser.setIcon(R.drawable.ic_menu_eraser_active);
    		return;
    	case TEXT:
    		item_typewriter.setIcon(R.drawable.ic_menu_text_active);
    	}
    }
    
	private void launchTagActivity() {
    	Intent i = new Intent(getApplicationContext(), TagsListActivity.class);    
    	startActivity(i);
	}
	
    private void setPenThickness(int thickness) {
    	if (thickness == mView.getPenThickness()) return;
		mView.setPenThickness(thickness);
		updatePenHistoryIcon();
    }
    
    private void setPenColor(int color) {
    	if (color == mView.getPenColor()) return;
		mView.setPenColor(color);
		updatePenHistoryIcon();
    }
    
    private void undo() {
		UndoManager.getUndoManager().undo();
		updateUndoRedoIcons();
    }
    
    private void redo() {
		UndoManager.getUndoManager().redo();
		updateUndoRedoIcons();	
    }
    
    private void switchToPage(Page page) {
    	mView.setPageAndZoomOut(page);
    	mView.setOverlay(new TagOverlay(page.tags, toolboxIsOnLeft));
    	menu_prepare_page_has_changed();
    }
    
    private void flip_page_prev() {
    	if (book.isFirstPage()) 
    		toast("Already on first tagged page"); 
		else	
			switchToPage(book.previousPage());
			if (book.isFirstPage()) 
				toast("Showing first tagged page"); 
			else
				toast("Showing page "+(book.currentPageNumber()+1)+
						" / "+book.pagesSize());
    }
    
    private void flip_page_next() {
		if (book.isLastPage()) {
			switchToPage(book.insertPageAtEnd());
			toast("Inserted new page at end");
		} else {
			switchToPage(book.nextPage());
			if (book.isLastPage())
				toast("Showing last tagged page");
			else 
				toast("Showing page "+(book.currentPageNumber()+1)+
						" / "+book.pagesSize());
		}
    }
    
    private void flip_page_prev_unfiltered() {
    	if (book.isFirstPageUnfiltered()) 
    		toast("Already on first page"); 
		else	
			switchToPage(book.previousPageUnfiltered());
			if (book.isFirstPageUnfiltered()) 
				toast("Showing first page"); 
			else
				toast("Showing page "+(book.currentPageNumber()+1)+
						" / "+book.pagesSize());
    }
    
    private void flip_page_next_unfiltered() {
		if (book.isLastPageUnfiltered()) {
			switchToPage(book.insertPageAtEnd());
			toast("Inserted new page at end");
		} else {
			switchToPage(book.nextPageUnfiltered());
			if (book.isLastPageUnfiltered())
				toast("Showing last page");
			else 
				toast("Showing page "+(book.currentPageNumber()+1)+
						" / "+book.pagesSize());
		}
    }
    
    public void toast(String s) {
    	if (mToast == null)
        	mToast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
    	else {
    		mToast.setText(s);
    	}
    	mToast.show();
    }
    
    private void menu_prepare_page_has_changed() {
    	if (mMenu == null) return;
    	mMenu.findItem(R.id.readonly).setChecked(book.currentPage().isReadonly());
		boolean first = (book.isFirstPage());
		boolean last  = (book.isLastPage());

		MenuItem prev = mMenu.findItem(R.id.page_prev);  // in the page submenu
		prev.setEnabled(!first);		
		prev = mMenu.findItem(R.id.prev);   // in the action bar 
		prev.setEnabled(!first);
		mView.getToolBox().setPrevIconEnabled(!first);
		
		MenuItem next = mMenu.findItem(R.id.page_next);
		next.setEnabled(!last);
		// next from the action bar inserts new page
		//		next = mMenu.findItem(R.id.next);
		//		next.setEnabled(!last);
		//		mView.getToolBox().setNextIconEnabled(!last);

		boolean first_unfiltered = (book.isFirstPageUnfiltered());
		mMenu.findItem(R.id.page_prev_unfiltered).setEnabled(!first_unfiltered);
    }
    

	private void switchPenHistory() {
		ToolHistory h = ToolHistory.getToolHistory();
		if (h.size() == 0) {
			Toast.makeText(getApplicationContext(), 
					"No other pen styles in history.", Toast.LENGTH_SHORT).show();
			return;
		}
		h.previous();
		switchPenHistory(0);
	}
  
	private void switchPenHistory(int history) {
		ToolHistory h = ToolHistory.getToolHistory();
		if (history >= h.size()) return;
		mView.setPenColor(h.getColor(history));
		mView.setPenThickness(h.getThickness(history));
		mView.setToolType(h.getTool(history));
		setActionBarIconActive(h.getTool(history));		
	}
	
    private void updatePenHistoryIcon() {
    	if (mMenu == null) return;
    	MenuItem item = mMenu.findItem(R.id.prev_pen);
    	if (item == null) return;
    	Drawable icon = item.getIcon();
    	if (icon == null) return; 	
    	ToolHistory history = ToolHistory.getToolHistory();
    	item.setIcon(history.getIcon());
   }

    
    private String pen_input_mode;
    
    @Override protected void onResume() {
        super.onResume();
        // Restore preferences
        mView.setOnToolboxListener(null);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	toolboxIsOnLeft = settings.getBoolean("toolbox_left", true);
       
    	UndoManager.setApplication(this);
    	book = Bookshelf.getCurrentBook();
        if (book != null) {
        	Page p = book.currentPage();
        	if (mView.getPage() == p) {
        		book.filterChanged();
        		mView.setOverlay(new TagOverlay(p.getTags(), toolboxIsOnLeft));
        	} else {
        		switchToPage(p);
        	}
        }

        if (hw==null)
    		hw = new name.vbraun.lib.pen.Hardware(getApplicationContext()); 
        boolean hwPen = hw.hasPenDigitizer();
        
    	int penColor = settings.getInt("pen_color", mView.getPenColor());
    	int penThickness = settings.getInt("pen_thickness", mView.getPenThickness());
    	int penTypeInt = settings.getInt("pen_type", mView.getToolType().ordinal());
    	Stroke.Tool penType = Stroke.Tool.values()[penTypeInt];
    	if (penType==Tool.ERASER)  // don't start with sharp whirling blades 
    		penType = Tool.MOVE;
    	mView.setPenColor(penColor);
    	mView.setPenThickness(penThickness);
    	mView.setToolType(penType);
    	
    	ToolHistory history = ToolHistory.getToolHistory();
    	history.restoreFromSettings(settings);
    	mView.getToolBox().onToolHistoryChanged(false);
    	
		if (settings.contains("only_pen_input")) { 
			// import obsoleted setting
			if (settings.getBoolean("only_pen_input", false)) 
				pen_input_mode = Preferences.STYLUS_WITH_GESTURES;
			else 
				pen_input_mode = Preferences.STYLUS_AND_TOUCH;
		} else if (hwPen)
			pen_input_mode = settings.getString(Preferences.KEY_LIST_PEN_INPUT_MODE, Preferences.STYLUS_WITH_GESTURES);
		else
			pen_input_mode = Preferences.STYLUS_AND_TOUCH;
		Log.d(TAG, "pen input mode "+pen_input_mode);
		if (pen_input_mode.equals(Preferences.STYLUS_ONLY)) {
			mView.setOnlyPenInput(true);
			mView.setDoubleTapWhileWriting(false);
			mView.setMoveGestureWhileWriting(false);
			mView.setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(Preferences.STYLUS_WITH_GESTURES)) {
			mView.setOnlyPenInput(true);
			mView.setDoubleTapWhileWriting(settings.getBoolean(
					Preferences.KEY_DOUBLE_TAP_WHILE_WRITE, hwPen));
    		mView.setMoveGestureWhileWriting(settings.getBoolean(
    				Preferences.KEY_MOVE_GESTURE_WHILE_WRITING, hwPen));
    		mView.setPalmShieldEnabled(false);
		}
		else if (pen_input_mode.equals(Preferences.STYLUS_AND_TOUCH)) {
			mView.setOnlyPenInput(false);
			mView.setDoubleTapWhileWriting(false);
			mView.setMoveGestureWhileWriting(false);
			mView.setPalmShieldEnabled(settings.getBoolean(Preferences.KEY_PALM_SHIELD, false));
		}
		else Assert.fail();
    	mView.setMoveGestureMinDistance(settings.getInt("move_gesture_min_distance", 400));
    	
    	volumeKeyNavigation = settings.getBoolean("volume_key_navigation", true);
    	
    	mView.setToolbox(toolboxIsOnLeft);
        mView.getToolBox().setToolboxVisible(settings.getBoolean("toolbox_is_visible", false));
   	
    	boolean showActionBar = settings.getBoolean("show_action_bar", true);
		ActionBar bar = getActionBar();
		if (showActionBar && (bar.isShowing() != showActionBar))
			bar.show();
		else if (!showActionBar && (bar.isShowing() != showActionBar))
			bar.hide();
		mView.getToolBox().setActionBarReplacementVisible(!showActionBar);

        mView.setOnToolboxListener(this);
    	updateUndoRedoIcons();
        mView.setOnStrokeFinishedListener(this);

    	eraserSwitchesBack = settings.getBoolean("eraser_switches_back", true);
    }
    
    @Override 
    protected void onPause() {
    	Log.d(TAG, "onPause");
    	super.onPause();
    	mView.interrupt();
        book.save(getApplicationContext());
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("pen_type", mView.getToolType().ordinal());
        editor.putInt("pen_color", mView.getPenColor());
        editor.putInt("pen_thickness", mView.getPenThickness());
        
    	ToolHistory history = ToolHistory.getToolHistory();
    	history.saveToSettings(editor);

        editor.putBoolean("volume_key_navigation", volumeKeyNavigation);
    	editor.putString(Preferences.KEY_LIST_PEN_INPUT_MODE, pen_input_mode);
        editor.remove("only_pen_input");  // obsoleted

        if (pen_input_mode.equals(Preferences.STYLUS_WITH_GESTURES)) {
        	editor.putBoolean(Preferences.KEY_DOUBLE_TAP_WHILE_WRITE, mView.getDoubleTapWhileWriting());
        	editor.putBoolean(Preferences.KEY_MOVE_GESTURE_WHILE_WRITING, mView.getMoveGestureWhileWriting());
        }
    	editor.putInt("move_gesture_min_distance", mView.getMoveGestureMinDistance());
    	editor.putBoolean("toolbox_is_visible", mView.getToolBox().isToolboxVisible());
        editor.commit();
    	UndoManager.setApplication(null);
    }
    
    private void launchOverviewActivity() {
		Intent i = new Intent(getApplicationContext(), ThumbnailActivity.class);    
    	startActivity(i);
    	finish();
    }
    
    @Override
    public void onBackPressed() {
    	if (isTaskRoot())
    		launchOverviewActivity();
    	else
    		super.onBackPressed();
    }
    
    
    public void add(Page page, int position) {
    	book.addPage(page, position);
    	switchToPage(book.currentPage());
    	updateUndoRedoIcons();
    }

    public void remove(Page page, int position) {
    	book.removePage(page, position);
    	switchToPage(book.currentPage());
    	updateUndoRedoIcons();
    }

    public void add(Page page, Graphics graphics) {
    	if (page != mView.getPage()) {
        	Assert.assertTrue("page not in book", book.getPages().contains(page));
        	book.setCurrentPage(page);
    		switchToPage(page);
    	}
    	mView.add(graphics);
    	updateUndoRedoIcons();
    }
    
    public void remove(Page page, Graphics graphics) {
    	if (page != mView.getPage()) {
        	Assert.assertTrue("page not in book", book.getPages().contains(page));
        	book.setCurrentPage(page);
    		switchToPage(page);
    	}
    	mView.remove(graphics);
    	updateUndoRedoIcons();
    }
    
    private void updateUndoRedoIcons() {
    	if (mMenu==null) return;
    	UndoManager mgr = UndoManager.getUndoManager();
    	MenuItem undo = mMenu.findItem(R.id.undo);
    	if (mgr.haveUndo() != undo.isEnabled()) {
        	mView.getToolBox().setUndoIconEnabled(mgr.haveUndo());
    		undo.setEnabled(mgr.haveUndo());
    		if (mgr.haveUndo())
    			undo.setIcon(R.drawable.ic_menu_undo);
    		else
    			undo.setIcon(R.drawable.ic_menu_undo_disabled);
    	}
    	MenuItem redo = mMenu.findItem(R.id.redo);
    	if (mgr.haveRedo() != redo.isEnabled()) {
        	mView.getToolBox().setRedoIconEnabled(mgr.haveRedo());
    		redo.setEnabled(mgr.haveRedo());
    		if (mgr.haveRedo())
    			redo.setIcon(R.drawable.ic_menu_redo);
    		else
    			redo.setIcon(R.drawable.ic_menu_redo_disabled);
    	}
    }

	@Override
	public void onStrokeFinishedListener() {
		if (!eraserSwitchesBack) return;
		Tool tool = mView.getToolType();
		if (tool != Tool.ERASER) return;
		ToolHistory h = ToolHistory.getToolHistory();
		setActiveTool(h.getTool());
	}

}

