package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import javax.net.ssl.HandshakeCompletedListener;

import sheetrock.panda.changelog.ChangeLog;

import name.vbraun.lib.help.HelpBrowser;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.HideBar;
import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.GraphicsImage;
import name.vbraun.view.write.HandwriterView;
import name.vbraun.view.write.Page;
import name.vbraun.view.write.ToolHistory;
import name.vbraun.view.write.Stroke;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.HandwriterView;
import name.vbraun.view.write.ToolHistory.HistoryItem;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.artist.ArtistPDF;
import com.write.Quill.artist.PaperType;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Storage;
import com.write.Quill.data.StorageAndroid;
import com.write.Quill.data.Book.BookIOException;
import com.write.Quill.export.ExportActivity;
import com.write.Quill.image.ImageActivity;
import com.write.Quill.thumbnail.ThumbnailActivity;

import android.app.ActionBar;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
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
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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
		ActivityBase
	implements 
		name.vbraun.view.write.Toolbox.OnToolboxListener,
		name.vbraun.view.write.InputListener {
	private static final String TAG = "Quill";

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
    private boolean someToolsSwitchBack;
    private boolean hideSystembar;

    private static final DialogThickness dialogThickness = new DialogThickness();
    private static final DialogAspectRatio dialogAspectRatio = new DialogAspectRatio();
    private static final DialogBackground dialogPaperType = new DialogBackground();
		
    /**
     *  Delete some preferences to test the default behavior
     */
    @SuppressWarnings("unused")
	private void testDefaultPreferences() {
    	Log.e(TAG, "Deleting some preferences, only use to debug!");
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(HandwriterView.KEY_PEN_SMOOTH_FILTER);
        editor.remove(HandwriterView.KEY_MOVE_GESTURE_FIX_ZOOM);
        editor.remove(Preferences.KEY_OVERRIDE_PEN_TYPE);
        editor.commit();
    }
    
    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //testDefaultPreferences();
      	if (UpdateActivity.needUpdate(this)) return;
      	      	
      	if (!Global.releaseModeOEM) {
      		ChangeLog changeLog = new ChangeLog(this);
      		if (changeLog.firstRun())
      			changeLog.getLogDialog().show();
      	}
      	book = Bookshelf.getCurrentBook();
      	book.setOnBookModifiedListener(UndoManager.getUndoManager());
        Assert.assertTrue("Book object not initialized.", book != null);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
        name.vbraun.lib.pen.Hardware.getInstance(getApplicationContext()); 

        ToolHistory history = ToolHistory.getToolHistory();
        history.onCreate(getApplicationContext());

        // Create and attach the view that is responsible for painting.
        mView = new HandwriterView(this);
        setContentView(mView);
        mView.setOnGraphicsModifiedListener(UndoManager.getUndoManager());
        mView.setOnToolboxListener(this);
        mView.setOnInputListener(this);

        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
        bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
        
        Display display = getWindowManager().getDefaultDisplay();
        int w = display.getWidth();
        if (w<600) Log.e(TAG, "A screen width of 600px is required");
        if (w>=800) {
        	createTagButton(bar);
        }
    	switchToPage(book.currentPage());
    	setKeepScreenOn();
    	UndoManager.setApplication(this);
    }


    private void setKeepScreenOn() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean screenOn = settings.getBoolean(Preferences.KEY_KEEP_SCREEN_ON, true);
        if (screenOn) 
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
        	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
    
    

    // The HandWriterView is not focusable and therefore does not receive KeyEvents
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		Log.v(TAG, "KeyEvent "+action+" "+keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (!volumeKeyNavigation) return false;
			if (action == KeyEvent.ACTION_UP) {
				flip_page_next();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (!volumeKeyNavigation) return false;
			if (action == KeyEvent.ACTION_DOWN) {
				flip_page_prev();
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
        return dlg.getDialog();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quill, menu);
        mMenu = menu;        
        if (!Hardware.hasPressureSensor()) {
        	MenuItem fountainPen = mMenu.findItem(R.id.fountainpen);
        	fountainPen.setVisible(false);
        	fountainPen.setEnabled(false);
        }
		int w = getWindowManager().getDefaultDisplay().getWidth();
		if (w>=800) {
    		mMenu.findItem(R.id.undo).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	}
    	if (w>=1280) {
    		mMenu.findItem(R.id.redo).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		mMenu.findItem(R.id.prev).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		mMenu.findItem(R.id.next).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	}
        menu_prepare_page_has_changed();
    	updatePenHistoryIcon();
    	updateUndoRedoIcons();
    	setActionBarIconActive(mView.getToolType());
    	return true;
    }
        
    protected static final int ACTIVITY_TAG_PAGE = 1;
    protected static final int ACTIVITY_TAG_FILTER = 2;

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
		case R.id.toolbox_line:
			setActiveTool(Tool.LINE);
			break;
		case R.id.toolbox_photo:
			setActiveTool(Tool.IMAGE);
			break;
		case R.id.toolbox_text:
			setActiveTool(Tool.TEXT);
			break;			
		case R.id.toolbox_resize:
			setActiveTool(Tool.MOVE);
			break;
		case R.id.toolbox_eraser:
			setActiveTool(Tool.ERASER);
			break;
		case R.id.toolbox_next:
		case R.id.toolbox_action_next:
    		flip_page_next();
			break;
		case R.id.toolbox_prev:
		case R.id.toolbox_action_prev:
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
    	switch (item.getItemId()) {
    	case R.id.prev_pen:
    		switchPenHistory();
    		return true;
    	case R.id.settings:
    		Intent preferencesIntent = new Intent(QuillWriterActivity.this, Preferences.class);
    		startActivity(preferencesIntent);
    		return true;
    	case R.id.fountainpen:
    	case R.id.tools_fountainpen:
    		setActiveTool(Tool.FOUNTAINPEN);
    		return true;
    	case R.id.pencil:
    	case R.id.tools_pencil:
    		setActiveTool(Tool.PENCIL);
    		return true;
		case R.id.tools_line:
			setActiveTool(Tool.LINE);
			return true;
    	case R.id.eraser:
    	case R.id.tools_eraser:
    		setActiveTool(Tool.ERASER);
    		return true;
    	case R.id.move:
    	case R.id.tools_move:
    		setActiveTool(Tool.MOVE);
    		return true;
    	case R.id.tools_typewriter:
    		setActiveTool(Tool.TEXT);
    		return true;
    	case R.id.tools_image:
    		setActiveTool(Tool.IMAGE);
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
    	case R.id.page_duplicate:
    		switchToPage(book.duplicatePage()); 
    		return true;	
    	case R.id.page_clear:
    		mView.clear();
    		return true;
    	case R.id.page_delete:
    		toast(getString(R.string.quill_deleted_page, 
    				book.currentPageNumber()+1, book.pagesSize()));
    		book.deletePage();
    		switchToPage(book.currentPage());
    		return true;
    	case R.id.export:
    		Intent exportIntent = new Intent(QuillWriterActivity.this, ExportActivity.class);
    		exportIntent.putExtra("filename", book.getTitle());
    		startActivity(exportIntent);
    		return true;
    	case R.id.tag_page:
    		launchTagActivity();
    		return true;
    	case R.id.thumbnails:
    	case android.R.id.home:
    		launchOverviewActivity();
    		return true;
    	case R.id.changelog:
    	    new ChangeLog(this).getFullLogDialog().show();
    	    return true;
    	case R.id.undo:
    		undo();
    		return true;
    	case R.id.redo:
    		redo();
    		return true;
    	case R.id.manual:
    		Intent helpIntent = new Intent(QuillWriterActivity.this, HelpBrowser.class);
    		startActivity(helpIntent);
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
    	if (mMenu == null || tool == null) return;
		updatePenHistoryIcon();
		MenuItem item_fountainpen = mMenu.findItem(R.id.fountainpen);
		MenuItem item_pencil      = mMenu.findItem(R.id.pencil);
		MenuItem item_move        = mMenu.findItem(R.id.move);
		MenuItem item_eraser      = mMenu.findItem(R.id.eraser);
		// MenuItem item_typewriter  = mMenu.findItem(R.id.typewriter);
		MenuItem tools_fountainpen = mMenu.findItem(R.id.tools_fountainpen);
		MenuItem tools_pencil      = mMenu.findItem(R.id.tools_pencil);
		MenuItem tools_line        = mMenu.findItem(R.id.tools_line);
		MenuItem tools_move        = mMenu.findItem(R.id.tools_move);
		MenuItem tools_eraser      = mMenu.findItem(R.id.tools_eraser);
		MenuItem tools_image       = mMenu.findItem(R.id.tools_image);
		MenuItem tools_typewriter  = mMenu.findItem(R.id.tools_typewriter);
		item_fountainpen.setIcon(R.drawable.ic_menu_quill);
		item_pencil.setIcon(R.drawable.ic_menu_pencil);
    	item_move.setIcon(R.drawable.ic_menu_resize);
    	item_eraser.setIcon(R.drawable.ic_menu_eraser);
    	// item_typewriter.setIcon(R.drawable.ic_menu_text);
    	switch (tool) {
    	case FOUNTAINPEN:
    		item_fountainpen.setIcon(R.drawable.ic_menu_quill_active);
    		tools_fountainpen.setChecked(true);
    		return;
    	case PENCIL:
    		item_pencil.setIcon(R.drawable.ic_menu_pencil_active);
    		tools_pencil.setChecked(true);
    		return;
    	case LINE:
    		tools_line.setChecked(true);
    		return;
    	case MOVE:
    		item_move.setIcon(R.drawable.ic_menu_resize_active);
    		tools_move.setChecked(true);
    		return;
    	case ERASER:
    		item_eraser.setIcon(R.drawable.ic_menu_eraser_active);
    		tools_eraser.setChecked(true);
    		return;
    	case IMAGE:
    		tools_image.setChecked(true);
    		return;
    	case TEXT:
    		// item_typewriter.setIcon(R.drawable.ic_menu_text_active);
    		tools_typewriter.setChecked(true);
    		return;
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
    	TagOverlay overlay = new TagOverlay(getApplicationContext(), 
    				page.tags, book.currentPageNumber(), mView.isToolboxOnLeft());
    	mView.setOverlay(overlay);
    	menu_prepare_page_has_changed();
    }
    
    private void flip_page_prev() {
    	if (book.isFirstPage()) 
    		toast(R.string.quill_already_first_tagged_page); 
		else	
			switchToPage(book.previousPage());
			if (book.isFirstPage()) 
				toast(R.string.quill_showing_first_tagged_page); 
			else
				toast(getString(R.string.quill_showing_page,
					book.currentPageNumber()+1, book.pagesSize()));
    }
    
    private void flip_page_next() {
		if (book.isLastPage()) {
			switchToPage(book.insertPageAtEnd());
			toast(R.string.quill_inserted_at_end);
		} else {
			switchToPage(book.nextPage());
			if (book.isLastPage())
				toast(R.string.quill_showing_last_tagged_page);
			else 
				toast(getString(R.string.quill_showing_page,
					book.currentPageNumber()+1, book.pagesSize()));
		}
    }
    
    private void flip_page_prev_unfiltered() {
    	if (book.isFirstPageUnfiltered()) 
    		toast(R.string.quill_already_first_page); 
		else	
			switchToPage(book.previousPageUnfiltered());
			if (book.isFirstPageUnfiltered()) 
				toast(R.string.quill_showing_first_page);
			else
				toast(getString(R.string.quill_showing_page,
					book.currentPageNumber()+1, book.pagesSize()));
    }
    
    private void flip_page_next_unfiltered() {
		if (book.isLastPageUnfiltered()) {
			switchToPage(book.insertPageAtEnd());
			toast(R.string.quill_inserted_at_end);
		} else {
			switchToPage(book.nextPageUnfiltered());
			if (book.isLastPageUnfiltered())
				toast(R.string.quill_showing_last_page);
			else 
				toast(getString(R.string.quill_showing_page, 
					book.currentPageNumber()+1, book.pagesSize()));
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
    
    public void toast(int resId) {
    	toast(getString(resId));
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
			toast(R.string.quill_no_other_pen_styles);
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
		setActiveTool(h.getTool(history));
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

        
    @Override protected void onResume() {
    	mView.stopInput();
    	UndoManager.setApplication(this);
        mView.setOnToolboxListener(null);
        super.onResume();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mView.loadSettings(settings);
        setActiveTool(mView.getToolType());
    	someToolsSwitchBack = settings.getBoolean(Preferences.KEY_TOOLS_SWITCH_BACK, true);
    	volumeKeyNavigation = settings.getBoolean(Preferences.KEY_VOLUME_KEY_NAVIGATION, true);
        
        hideSystembar = settings.getBoolean(Preferences.KEY_HIDE_SYSTEM_BAR, false);
        if (hideSystembar)
        	HideBar.hideSystembar(getApplicationContext());

    	book = Bookshelf.getCurrentBook();
        if (book != null) {
        	Page p = book.currentPage();
        	if (mView.getPage() == p) {
        		book.filterChanged();
            	TagOverlay overlay = new TagOverlay(getApplicationContext(), 
            			p.getTags(), book.currentPageNumber(), mView.isToolboxOnLeft());
            	mView.setOverlay(overlay);
        	} else {
        		switchToPage(p);
        	}
        }
            	
    	boolean showActionBar = settings.getBoolean(Preferences.KEY_SHOW_ACTION_BAR, true);
		ActionBar bar = getActionBar();
		if (showActionBar && (bar.isShowing() != showActionBar))
			bar.show();
		else if (!showActionBar && (bar.isShowing() != showActionBar))
			bar.hide();
		mView.getToolBox().setActionBarReplacementVisible(!showActionBar);

        mView.setOnToolboxListener(this);
        mView.setOnInputListener(this);
    	updateUndoRedoIcons();
    	setKeepScreenOn();
    	mView.startInput();
    }
    
    @Override 
    protected void onPause() {
    	Log.d(TAG, "onPause");
    	mView.stopInput();
        if (hideSystembar)
        	HideBar.showSystembar(getApplicationContext());
        super.onPause();
    	mView.interrupt();
        book.save();
        
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);     
        SharedPreferences.Editor editor = settings.edit();
        mView.saveSettings(editor);
        editor.commit();
    	UndoManager.setApplication(null);
    }
    
    @Override
    protected void onStop() {
		// bookshelf.backup();
    	super.onStop();
    }
    
    private final static int REQUEST_REPORT_BACK_KEY = 1;
    private final static int REQUEST_PICK_IMAGE = 2;
    private final static int REQUEST_EDIT_IMAGE = 3;

    private void launchOverviewActivity() {
		Intent i = new Intent(getApplicationContext(), ThumbnailActivity.class);    
    	startActivity(i);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case REQUEST_REPORT_BACK_KEY:
    		if (resultCode != RESULT_OK) return;
    		boolean backPressed = data.getBooleanExtra(ThumbnailActivity.RESULT_BACK_KEY_PRESSED, false);
    		if (backPressed) 
    			finish();
    		break;
    	case REQUEST_PICK_IMAGE:
    	case REQUEST_EDIT_IMAGE:
    		if (resultCode != RESULT_OK) return;
    		String uuidStr = data.getStringExtra(ImageActivity.EXTRA_UUID);
    		Assert.assertNotNull(uuidStr);
    		UUID uuid = UUID.fromString(uuidStr);
    		boolean constrain = data.getBooleanExtra(ImageActivity.EXTRA_CONSTRAIN_ASPECT, true);
    		String uriStr = data.getStringExtra(ImageActivity.EXTRA_FILE_URI);
    		if (uriStr == null)
        		mView.setImage(uuid, null, constrain);
    		else {
    			Uri uri = Uri.parse(uriStr);
    			String name = uri.getPath();
    			mView.setImage(uuid, name, constrain);
    		}
    		break;
    	}
    }
    
    @Override
    public void onBackPressed() {
		Intent i = new Intent(getApplicationContext(), ThumbnailActivity.class);    
    	startActivityForResult(i, REQUEST_REPORT_BACK_KEY);
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

    
    public void add(Page page, LinkedList<Stroke> strokes) {
    	if (page != mView.getPage()) {
        	Assert.assertTrue("page not in book", book.getPages().contains(page));
        	book.setCurrentPage(page);
    		switchToPage(page);
    	}
    	mView.add(strokes);
    	updateUndoRedoIcons();
    }
    
    public void remove(Page page, LinkedList<Stroke> strokes) {
    	if (page != mView.getPage()) {
        	Assert.assertTrue("page not in book", book.getPages().contains(page));
        	book.setCurrentPage(page);
    		switchToPage(page);
    	}
    	mView.remove(strokes);
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
		if (!someToolsSwitchBack) return;
		Tool tool = mView.getToolType();
		if (tool != Tool.MOVE && tool != Tool.ERASER) return;
		ToolHistory h = ToolHistory.getToolHistory();
		setActiveTool(h.getTool());
	}

	@Override
	public void onPickImageListener(GraphicsImage image) {
    	Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
    	intent.putExtra(ImageActivity.EXTRA_UUID, image.getUuid().toString());
    	startActivityForResult(intent, REQUEST_PICK_IMAGE);
	}
	
	@Override
	public void onEditImageListener(GraphicsImage image) {
    	Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.EXTRA_UUID, image.getUuid().toString());
		intent.putExtra(ImageActivity.EXTRA_CONSTRAIN_ASPECT, image.getConstrainAspect());
        intent.putExtra(ImageActivity.EXTRA_FILE_URI, image.getFileUri().toString());
    	startActivityForResult(intent, REQUEST_EDIT_IMAGE);
	}
	
}

