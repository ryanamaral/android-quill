package com.write.Quill;

import java.util.LinkedList;
import java.util.ListIterator;

import name.vbraun.view.tag.TagEditDialog;
import name.vbraun.view.tag.TagListView;
import name.vbraun.view.write.Page;
import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.ThumbnailAdapter.Thumbnail;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;
import com.write.Quill.data.TagManager;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.content.DialogInterface;


public class ThumbnailActivity 
	extends 
		ActivityBase
	implements 
		AdapterView.OnItemClickListener, 
		AdapterView.OnItemLongClickListener,
		DialogInterface.OnDismissListener {
	
	private static final String TAG = "Overview";
	
	private boolean backPressedImmediately = true;
	public static final String RESULT_BACK_KEY_PRESSED = "result_back_key_pressed";
	
	private View layout;
	private Menu menu;
	
	protected TagListView tagList;
	protected ThumbnailView thumbnailGrid;
	
	protected TagManager tagManager;
	protected TagSet tags;
	
    private void launchQuillWriterActivity(Page page) {
		Book book = Bookshelf.getCurrentBook();
		book.setCurrentPage(page);
		launchQuillWriterActivity();
    }
    
    private void launchQuillWriterActivity() {
    	Intent i = new Intent();
    	i.putExtra(RESULT_BACK_KEY_PRESSED, false);
    	setResult(RESULT_OK, i);
    	finish();
    }

    @Override
    public void onBackPressed() {
    	Intent i = new Intent();
    	i.putExtra(RESULT_BACK_KEY_PRESSED, backPressedImmediately);
    	setResult(RESULT_OK, i);
    	finish();
    	super.onBackPressed();
    }
    	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		backPressedImmediately = false;
		switch (parent.getId()) {
		case R.id.tag_list:
			Tag t = tagManager.get(position);
			if (tags.contains(t))
				tags.remove(t);
			else
				tags.add(t);
	    	dataChanged();
			break;
		case R.id.thumbnail_grid:
			Thumbnail thumb = (Thumbnail)view; 
			launchQuillWriterActivity(thumb.page);
			break;
		}
		Log.d(TAG, "Click: "+tags.size());
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		backPressedImmediately = false;
		Log.d(TAG, "onItemLongClick "+parent.getId());
		switch (parent.getId()) {
		case R.id.tag_list:
			longClickedTag = tagManager.get(position);
			Assert.assertNotNull(longClickedTag);
			showDialog(DIALOG_EDIT_TAG);
			return true;
		default:
			return false;
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu mMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thumbnail, mMenu);
        menu = mMenu;
        return true;
    }
	
	protected void dataChanged() {
		Bookshelf.getCurrentBook().filterChanged();
       	tagList.notifyTagsChanged();
       	thumbnailGrid.notifyTagsChanged();
	}
	
	private Tag longClickedTag;
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
		switch (id) {
		case DIALOG_EDIT_TAG:
			TagEditDialog dlg = (TagEditDialog)dialog;
			dlg.setTag(longClickedTag);
			longClickedTag = null;
			dlg.setOnDismissListener(this);
			break;
        case DIALOG_SEND_TO_EVERNOTE:
        	EvernoteExportDialog evernote = (EvernoteExportDialog)dialog;
    		Book book = Bookshelf.getCurrentBook();
    		LinkedList<Page> pages = book.getFilteredPages();
        	evernote.setPages(book, pages);
        	evernote.setUUID(book.getUUID());
        	break;
		default:
			super.onPrepareDialog(id, dialog);
		}
	}
		
	@Override
	public void onDismiss(DialogInterface dialog) {
		dataChanged();
	}
	
	protected static final int RESULT_FILTER_CHANGED = 2;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		backPressedImmediately = false;
		Intent i;
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	launchQuillWriterActivity();
	        	return true;
	        case R.id.switch_notebook:
	    		i = new Intent(getApplicationContext(), BookshelfActivity.class);    
	        	startActivity(i);
	        	return true;
	        case R.id.new_notebook:
                showDialog(DIALOG_NEW_NOTEBOOK);
	        	return true;
	        case R.id.send_to_evernote:
                showDialog(DIALOG_SEND_TO_EVERNOTE);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
    private static final int DIALOG_NEW_NOTEBOOK = 0;
    private static final int DIALOG_SEND_TO_EVERNOTE = 1;
	private static final int DIALOG_EDIT_TAG = 2;
	

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NEW_NOTEBOOK:
        	return createNewNotebookDialog();
        case DIALOG_SEND_TO_EVERNOTE:
        	return new EvernoteExportDialog(this);
		case DIALOG_EDIT_TAG:
			return new TagEditDialog(this);
        }
		return null;
    }
    
    private Dialog createNewNotebookDialog() {
    	LayoutInflater factory = LayoutInflater.from(this);
    	final View textEntryView = factory.inflate(R.layout.new_notebook, null);
    	AlertDialog dlg = new AlertDialog.Builder(this)
    		.setIconAttribute(android.R.attr.alertDialogIcon)
    		.setTitle("Create new notebook")
    		.setView(textEntryView)
    		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				EditText text = (EditText) textEntryView.findViewById(R.id.new_notebook_title);
    				String title = text.getText().toString();
    				Bookshelf.getBookshelf().newBook(title);
    				reloadTags();
    		}})
    		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				// do nothing
    		}})
    		.create();
    	
    	textEntryView.findViewById(R.id.new_notebook_title).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER
                		&& event.getAction() == KeyEvent.ACTION_UP) {
                	EditText editText = (EditText)v;
                	String text = editText.getText().toString();
                    int editTextRowCount = text.split("\\n").length;
                    if (editTextRowCount >= 3) {
                        int lastBreakIndex = text.lastIndexOf("\n");
                        String newText = text.substring(0, lastBreakIndex);
                        editText.setText("");
                        editText.append(newText);
                    }
                    return true;
                }
                return false;
            }});
    	return dlg;
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		layout = getLayoutInflater().inflate(R.layout.thumbnail_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagList.setOnItemClickListener(this);
		tagList.setOnItemLongClickListener(this);
		Assert.assertTrue("Tag list not created.", tagList != null);
		tagList.showNewTextEdit(false);
		
		thumbnailGrid = (ThumbnailView) findViewById(R.id.thumbnail_grid);
		Assert.assertTrue("Thumbnail grid not created.", thumbnailGrid != null);
		thumbnailGrid.setOnItemClickListener(this);
		thumbnailGrid.setMultiChoiceModeListener(new MultiselectCallback());
		
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.title_filter);
        bar.setDisplayHomeAsUpEnabled(true);
	}
	
	private void reloadTags() {
		Book book = Bookshelf.getCurrentBook();
      	tagManager = book.getTagManager();
		tagManager.sort();
		tags = Bookshelf.getCurrentBook().getFilter();
		tagList.setTagSet(tags);
		dataChanged();	
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		tool.debug.MemDebug.logHeap(this.getClass());
		thumbnailGrid.setAdapter(null); // stop updates
	}
	
	@Override
	protected void onResume() {
		reloadTags();
		Log.d(TAG, "onResume");
		super.onResume();
	}

	private class MultiselectCallback implements ThumbnailView.MultiChoiceModeListener {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        	// MenuInflater inflater = getMenuInflater();
            // inflater.inflate(R.menu.menu, menu);
            mode.setTitle("Select Items");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            default:
                Toast.makeText(getBaseContext(), "Clicked " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
                mode.finish();
                break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        	thumbnailGrid.uncheckAll();
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            final int checkedCount = thumbnailGrid.getCheckedItemCount();
            thumbnailGrid.checkedStateChanged(position, checked);
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle("One page selected");
                    break;
                default:
                    mode.setSubtitle("" + checkedCount + " pages selected");
                    break;
            }
        }
    }

}
