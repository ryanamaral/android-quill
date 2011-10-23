package com.write.Quill;
import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
	
	protected TagManager tagManager = TagManager.getTagManager();
	protected TagSet tags = TagManager.newTagSet();
	
	protected void tagsChanged(boolean onlySelection) {
       	tagList.notifyTagsChanged();
       	if (onlySelection)
       		tagCloud.notifyTagSelectionChanged();
       	else
       		tagCloud.notifyTagsChanged();
   		updateStatusBar();
    	QuillWriterActivity.getBook().current_page().is_modified = true;
	}
	
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If the event is a key-down event on the "enter" button
		Log.v(TAG, "onKey "+keyCode);
    	EditText text = (EditText)v;
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
            (keyCode == KeyEvent.KEYCODE_ENTER)) {
        	TagManager tm = TagManager.getTagManager();
        	Tag t = tm.makeTag(text.getText().toString());
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate");
		TagManager tm = TagManager.getTagManager();
		tm.sort();
		tags = QuillWriterActivity.getBook().current_page().tags;
		
		layout = getLayoutInflater().inflate(R.layout.tag_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagCloud = (TagCloudView) findViewById(R.id.tag_cloud_view);
		tagList.setOnItemClickListener(this);
		tagList.setOnKeyListener(this);
		tagCloud.setOnTouchListener(this);
		assert tagList != null: "Tag list not created.";
		assert tagCloud != null: "Tag cloud not created.";
		tagList.setTagSet(tags);
		tagCloud.setTagSet(tags);
		
		status = (TextView) findViewById(R.id.status);
		updateStatusBar();
		
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        
        tagButton = new Button(this);
        tagButton.setText(R.string.tag_button);
        bar.setCustomView(tagButton);
        bar.setDisplayShowCustomEnabled(true);
      	tagButton.setOnClickListener(
      	        new OnClickListener() {
      	            public void onClick(View v) {
      	            	finish();
      	            }});
      	
	}



	
}
