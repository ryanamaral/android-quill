package com.write.Quill;

import junit.framework.Assert;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

public class OverviewActivity extends Activity implements 
	AdapterView.OnItemClickListener {
	
	private static final String TAG = "Overview";
	
	private View layout;
	protected TagListView tagList;
	protected ThumbnailView thumbnailGrid;
	
	protected TagManager tagManager = TagManager.getTagManager();
	protected TagSet tags = TagManager.newTagSet();

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Tag t = tagManager.get(position);
		if (tags.contains(t))
			tags.remove(t);
		else
			tags.add(t);
		Log.d(TAG, "Click: "+tags.size());
    	tagsChanged(true);
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

        ActionBar bar = getActionBar();
        bar.setTitle(R.string.title_filter);
        bar.setDisplayHomeAsUpEnabled(true);
	}
}
