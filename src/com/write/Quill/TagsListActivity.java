package com.write.Quill;
import name.vbraun.view.tag.TagCloudView;
import name.vbraun.view.tag.TagEditDialog;
import name.vbraun.view.tag.TagListView;
import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.StorageAndroid;
import com.write.Quill.data.TagManager;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;
import com.write.Quill.thumbnail.ThumbnailActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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


public class TagsListActivity 
	extends 
		ActivityBase 
	implements 
		AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener,
		View.OnKeyListener, 
		View.OnTouchListener,
		DialogInterface.OnDismissListener {
	
	private static final String TAG = "TagsListActivity";
	private Button tagButton;
	private View layout;
	protected TagListView tagList;
	protected TagCloudView tagCloud;
	protected TextView status;
	protected Menu menu;
	
	protected TagManager tagManager;
	protected TagSet tags;
	protected Tag tag;
	
	protected void tagsChanged(boolean onlySelection) {
       	tagList.notifyTagsChanged();
       	if (onlySelection)
       		tagCloud.notifyTagSelectionChanged();
       	else
       		tagCloud.notifyTagsChanged();
   		updateStatusBar();
    	Bookshelf.getCurrentBook().currentPage().touch();
	}
	
	private final static int DIALOG_EDIT_TAG = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EDIT_TAG:
			return new TagEditDialog(this);
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_EDIT_TAG:
			TagEditDialog dlg = (TagEditDialog)dialog;
			dlg.setTag(tag);
			dlg.setOnDismissListener(this);
		default:
			super.onPrepareDialog(id, dialog);
		}
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
       	tagList.notifyTagsChanged();
       	tagCloud.notifyTagsChanged();
   		updateStatusBar();
	}


	@Override
	protected void onResume() {
		super.onResume();
      	Book book = Bookshelf.getCurrentBook();
		tagManager = book.getTagManager();
		tagManager.sort();
		tags = Bookshelf.getCurrentBook().currentPage().tags;
		tagList.setTagSet(tags);
		tagCloud.setTagSet(tags);
		updateStatusBar();		       	
	}
	
	
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If the event is a key-down event on the "enter" button
		Log.v(TAG, "onKey "+keyCode);
    	EditText text = (EditText)v;
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
            (keyCode == KeyEvent.KEYCODE_ENTER)) {
        	Tag t = tagManager.newTag(text.getText().toString());
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
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == tagList.getAdapterView()) {
			Log.d(TAG, "Long click: "+tags.size());
			tag = tagManager.get(position);
			showDialog(DIALOG_EDIT_TAG);
			return true;
		}
		return false;
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
		String msg = getString(R.string.tag_list_status, 
				tags.size(), tags.allTags().size());
		status.setText(msg);
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
	        case R.id.menu_tag_thumbnails:
	        	finish();
	    		i = new Intent(getApplicationContext(), ThumbnailActivity.class);    
	        	startActivity(i);
	    		return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
						
		layout = getLayoutInflater().inflate(R.layout.tag_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagCloud = (TagCloudView) findViewById(R.id.tag_cloud_view);
		Assert.assertTrue("Tag list not created.", tagList != null);
		Assert.assertTrue("Tag cloud not created.", tagCloud != null);
		tagList.setOnItemClickListener(this);
		tagList.setOnItemLongClickListener(this);
		tagList.setOnKeyListener(this);
		tagCloud.setOnTouchListener(this);

		status = (TextView) findViewById(R.id.status);
		
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.tag_list_title);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
	}
	
}
