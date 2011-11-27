package com.write.Quill;

import com.write.Quill.Bookshelf.Notebook;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class BookshelfActivity 
	extends ListActivity
	implements OnItemClickListener, OnItemLongClickListener{
	
	private final static String TAG = "BookshelfActivity";
	
	protected BookshelfAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bookshelf.onCreate(getApplicationContext());
		
		adapter = new BookshelfAdapter(getApplicationContext());
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnItemLongClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		Notebook nb = bookshelf.getNotebookList().get(position);
		bookshelf.setCurrentBook(nb);
		Log.d(TAG, "Click: "+nb.getTitle());
		finish();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		Notebook nb = bookshelf.getNotebookList().get(position);

		Log.d(TAG, "Long click: "+nb.getTitle());
		return true;
	}


}
