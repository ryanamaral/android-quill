package com.write.Quill;

import name.vbraun.view.tag.TagListView;
import name.vbraun.view.write.TagManager;
import name.vbraun.view.write.TagManager.Tag;
import name.vbraun.view.write.TagManager.TagSet;
import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.ThumbnailAdapter.Thumbnail;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.content.DialogInterface;


public class OverviewActivity extends Activity implements 
	AdapterView.OnItemClickListener {
	
	private static final String TAG = "Overview";
	
	private View layout;
	private Menu menu;
	
	protected TagListView tagList;
	protected ThumbnailView thumbnailGrid;
	
	protected TagManager tagManager = TagManager.getTagManager();
	protected TagSet tags = TagManager.newTagSet();
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (parent.getId()) {
		case R.id.tag_list:
			Tag t = tagManager.get(position);
			if (tags.contains(t))
				tags.remove(t);
			else
				tags.add(t);
	    	tagsChanged(true);
			break;
		case R.id.thumbnail_grid:
			Book book = Bookshelf.getCurrentBook();
			Thumbnail thumb = (Thumbnail)view; 
			book.setCurrentPage(thumb.page);
			finish();
			break;
		}
		Log.d(TAG, "Click: "+tags.size());
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu mMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overview, mMenu);
        menu = mMenu;
        return true;
    }
	
	protected void tagsChanged(boolean onlySelection) {
       	tagList.notifyTagsChanged();
		Bookshelf.getCurrentBook().filterChanged();
       	thumbnailGrid.notifyTagsChanged();
	}
	
	protected static final int RESULT_FILTER_CHANGED = 2;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	finish();
	        	return true;
	        case R.id.switch_notebook:
	    		i = new Intent(getApplicationContext(), BookshelfActivity.class);    
	        	startActivity(i);
	        	return true;
	        case R.id.new_notebook:
                showDialog(DIALOG_NEW_NOTEBOOK);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
    private static final int DIALOG_NEW_NOTEBOOK = 0;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NEW_NOTEBOOK:
        	LayoutInflater factory = LayoutInflater.from(this);
        	final View textEntryView = factory.inflate(R.layout.new_notebook, null);
        	return new AlertDialog.Builder(this)
        		.setIconAttribute(android.R.attr.alertDialogIcon)
        		.setTitle("Create new notebook")
        		.setView(textEntryView)
        		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {

        				/* User clicked OK so do some stuff */
        		}})
        		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked cancel so do some stuff */
        		}})
        		.create();
        }
		return null;
    }
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
      	Bookshelf.onCreate(getApplicationContext());

		layout = getLayoutInflater().inflate(R.layout.overview, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagList.setOnItemClickListener(this);
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
	
	@Override
	protected void onResume() {
		super.onResume();
      	TagManager tm = TagManager.getTagManager();
		tm.sort();
		tags = Bookshelf.getCurrentBook().filter;
		tagList.setTagSet(tags);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
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
