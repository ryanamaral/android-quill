package com.write.Quill;
import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;


public class TagsListActivity extends Activity 
	implements AdapterView.OnItemClickListener, View.OnKeyListener {
	
	private static final String TAG = "TagsListActivity";
	private Button tagButton;
	private View layout;
	protected TagListView tagList;
	protected TagCloudView tagCloud;
	
	protected TagManager tagManager = TagManager.getTagManager();
	protected TagSet tags = TagManager.newTagSet();
	
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If the event is a key-down event on the "enter" button
		Log.v(TAG, "onKey "+keyCode);
    	EditText text = (EditText)v;
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
            (keyCode == KeyEvent.KEYCODE_ENTER)) {
        	TagManager tm = TagManager.getTagManager();
        	Tag t = tm.makeTag(text.getText().toString());
        	tags.add(t);
        	tagList.notifyTagsChanged();
        	tagCloud.notifyTagsChanged();
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
		tagList.notifyTagsChanged();
		tagCloud.notifyTagSelectionChanged();
	}
	
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate");
		TagManager tm = TagManager.getTagManager();
		tags.add(tm.makeTag("Selection 1"));
		tags.add(tm.makeTag("Selection 2"));
		tags.add(tm.makeTag("Selection 3"));
		tm.makeTag("Tag 4");
		tm.makeTag("Tag 5");
		tm.makeTag("Tag 6");
		tm.makeTag("Tag 7");
		tm.makeTag("Tag 8");
		tm.makeTag("Tag 9");
		tm.makeTag("Tag 10");
		tm.makeTag("Tag 11");

//		view = new TagsListView(this, tags);
//		setContentView(view);
		
		layout = getLayoutInflater().inflate(R.layout.tag_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagCloud = (TagCloudView) findViewById(R.id.tag_cloud_view);
		tagList.setOnItemClickListener(this);
		tagList.setOnKeyListener(this);
		assert tagList != null: "Tag list not created.";
		assert tagCloud != null: "Tag cloud not created.";
		tagList.setTagSet(tags);
		tagCloud.setTagSet(tags);
		
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
