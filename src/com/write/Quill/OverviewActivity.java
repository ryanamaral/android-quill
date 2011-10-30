package com.write.Quill;

import junit.framework.Assert;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;
import com.write.Quill.ThumbnailAdapter.Thumbnail;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

public class OverviewActivity extends Activity implements 
	AdapterView.OnItemClickListener {
	
	private static final String TAG = "Overview";
	
	private View layout;
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
			Log.d(TAG, "onItemClick "+parent.getId()+" "+id+ " ");
			Book book = Book.getBook();
			Thumbnail thumb = (Thumbnail)view; 
			book.setCurrentPage(thumb.page);
			finish();
			break;
		}
		Log.d(TAG, "Click: "+tags.size());
	}

	protected void tagsChanged(boolean onlySelection) {
       	tagList.notifyTagsChanged();
		Book.getBook().filterChanged();
       	thumbnailGrid.notifyTagsChanged();
	}
	
	protected static final int RESULT_FILTER_CHANGED = 2;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	finish();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
      	Book.onCreate(getApplicationContext());
		
		TagManager tm = TagManager.getTagManager();
		tm.sort();
		tags = Book.getBook().filter;
		
		layout = getLayoutInflater().inflate(R.layout.overview, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagList.setOnItemClickListener(this);
		Assert.assertTrue("Tag list not created.", tagList != null);
		tagList.setTagSet(tags);
		tagList.showNewTextEdit(false);
		
		thumbnailGrid = (ThumbnailView) findViewById(R.id.thumbnail_grid);
		Assert.assertTrue("Thumbnail grid not created.", thumbnailGrid != null);
		thumbnailGrid.setOnItemClickListener(this);
		thumbnailGrid.setMultiChoiceModeListener(new MultiselectCallback());
		
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.title_filter);
        bar.setDisplayHomeAsUpEnabled(true);
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
