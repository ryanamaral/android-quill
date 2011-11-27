package com.write.Quill;
import name.vbraun.view.tag.TagCloudView;
import name.vbraun.view.tag.TagListView;
import name.vbraun.view.write.TagManager;
import name.vbraun.view.write.TagManager.Tag;
import name.vbraun.view.write.TagManager.TagSet;
import junit.framework.Assert;

import com.write.Quill.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class TagsListActivity extends Activity implements 
		AdapterView.OnItemClickListener, 
		View.OnKeyListener, 
		View.OnTouchListener {
	
	private static final String TAG = "TagsListActivity";
	private Button tagButton;
	private View layout;
	protected TagListView tagList;
	protected TagCloudView tagCloud;
	protected TextView status;
	protected Menu menu;
	
	protected TagManager tagManager;
	protected TagSet tags;
	
	protected void tagsChanged(boolean onlySelection) {
       	tagList.notifyTagsChanged();
       	if (onlySelection)
       		tagCloud.notifyTagSelectionChanged();
       	else
       		tagCloud.notifyTagsChanged();
   		updateStatusBar();
    	Bookshelf.getCurrentBook().currentPage().touch();
	}
	
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If the event is a key-down event on the "enter" button
		Log.v(TAG, "onKey "+keyCode);
    	EditText text = (EditText)v;
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
            (keyCode == KeyEvent.KEYCODE_ENTER)) {
        	Tag t = tagManager.makeTag(text.getText().toString());
        	tags.add(t);
        	tagsChanged(false);
        	return true;
        }
        return false;
    }
	
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Tag t = tagManager.get(position);
		if (tags.contains(t))
			tags.remove(t);
		else
			tags.add(t);
		Log.d(TAG, "Click: "+tags.size());
    	tagsChanged(true);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view.getId() == R.id.tag_cloud_view) {
			Log.d(TAG, "onTouch");
			float x = event.getX();
			float y = event.getY();
			Tag t = tagCloud.findTagAt(x,y);
			if (t != null) {
				if (tags.contains(t))
					tags.remove(t);
				else
					tags.add(t);
	        	tagsChanged(true);
			}
		}
		return false;
	}

	protected void updateStatusBar() {
		status.setText("Selected "+tags.size()+" / "+tags.allTags().size()+" tags");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu mMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tag_list, mMenu);
        menu = mMenu;
        return true;
    }
	
    protected static int RESULT_TAGS_CHANGED = 1;
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	finish();
	        	return true;
	        case R.id.menu_tag_filter:
	        	finish();
	    		i = new Intent(getApplicationContext(), OverviewActivity.class);    
	        	startActivity(i);
	    		return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate");
      	Bookshelf.onCreate(getApplicationContext());
      	Book book = Bookshelf.getCurrentBook();
		tagManager = book.getTagManager();
		tagManager.sort();
		tags = Bookshelf.getCurrentBook().currentPage().tags;
		
		layout = getLayoutInflater().inflate(R.layout.tag_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagCloud = (TagCloudView) findViewById(R.id.tag_cloud_view);
		tagList.setOnItemClickListener(this);
		tagList.setOnKeyListener(this);
		tagCloud.setOnTouchListener(this);
		Assert.assertTrue("Tag list not created.", tagList != null);
		Assert.assertTrue("Tag cloud not created.", tagCloud != null);
		tagList.setTagSet(tags);
		tagCloud.setTagSet(tags);
		
		status = (TextView) findViewById(R.id.status);
		updateStatusBar();
		
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.title_tag);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
}



	
}
