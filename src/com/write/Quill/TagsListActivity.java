package com.write.Quill;
import android.app.Activity;
import android.os.Bundle;


public class TagsListActivity extends Activity {
	TagsListView view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = new TagsListView(this);
		setContentView(view);
	}
	
	
	
}
