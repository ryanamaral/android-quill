package com.write.Quill;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class TagsListView extends RelativeLayout {
	private static final String TAG = "TagsListView";

	protected View layout;
	protected ListAdapter adapter;
	protected ListView list;
	
	TagsListView(Context context) {
		super(context);
		
		LayoutInflater layoutInflater = (LayoutInflater)
			context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = layoutInflater.inflate(R.layout.tag_list, this);

        adapter = ArrayAdapter.createFromResource(context, 
        		R.array.someArrayForDisplayInList,
        		android.R.layout.simple_list_item_1);
        list = (ListView) findViewById(R.id.list);
        
        Log.d("adapter size", String.format("%d",adapter.getCount()));
        list.setAdapter(adapter);
	}
	
	
	
//	
//	
//	
//	protected void onCreate(Bundle savedInstanceState) {
//		
//        layout = getLayoutInflater().inflate(R.layout.tag_action_menu, null, false);
//        setContentView(layout);
//        
//        adapter = ArrayAdapter.createFromResource(this, 
//        		R.array.someArrayForDisplayInList,
//        		android.R.layout.simple_list_item_1);
//        // mListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
//
//        Log.d("adapter size", String.format("%d",adapter.getCount()));
//        setListAdapter(adapter);
//        	
//        
//        list = (ListView) findViewById(R.id.list);

        //setContentView(lv);

////        ListView lv = new ListView(this);
//        assert mListAdapter != null;
//        assert lv != null;
////        setContentView(tag_action_menu);
//        lv.setAdapter(mListAdapter);
    
//    
//        
////      bar.setDisplayOptions(
////		ActionBar.DISPLAY_SHOW_CUSTOM | 
////		ActionBar.DISPLAY_SHOW_HOME
////		);
//
//View tag_action_menu = getLayoutInflater().inflate(R.layout.tag_action_menu, null, false);
//
////LinkedList<String> sizes_values = new LinkedList<String>();
////ArrayAdapter adapt = new ArrayAdapter(this,
////		android.R.layout.list_item, sizes_values);
////String [] strings = getResources().getStringArray(R.array.export_size_vector);
////adapt.addAll(strings);
//////adapt.setDropDownViewResource(android.R.layout.simple_list_item_1);
//
//
//
//
//
//ListView lv = (ListView) findViewById(R.id.tag_list);
//setContentView(lv);
//
////ListView lv = new ListView(this);
//assert mListAdapter != null;
//assert lv != null;
////setContentView(tag_action_menu);
//lv.setAdapter(mListAdapter);
//setContentView(lv);
//////lv.setTextFilterEnabled(true);
// 
//        
//        
//        
        
       
	
}
