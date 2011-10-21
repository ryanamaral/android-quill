package com.write.Quill;
import com.write.Quill.TagManager.TagSet;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class TagsListActivity extends Activity {
	private static final String TAG = "TagsListActivity";
	Button tagButton;
	View layout;
	TagListView tagList;
	TagCloudView tagCloud;
	
	TagSet tags = TagManager.newTagSet();
	
	
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

//		view = new TagsListView(this, tags);
//		setContentView(view);
		
		layout = getLayoutInflater().inflate(R.layout.tag_activity, null);
		setContentView(layout);
		tagList = (TagListView) findViewById(R.id.tag_list_view);
		tagCloud = (TagCloudView) findViewById(R.id.tag_cloud_view);
		assert tagList != null: "Tag list not created.";
		assert tagCloud != null: "Tag cloud not created.";
		tagList.setTagSet(tags);
		
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
