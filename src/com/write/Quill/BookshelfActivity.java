package com.write.Quill;

import java.util.UUID;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.Notebook;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

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
	
	private UUID uuidToDelete = null;
	
	private void deleteAfterConfirmation(UUID uuid) {
		uuidToDelete = uuid;
	}

	private void deleteConfirmation() {
    	Bookshelf.getBookshelf().deleteBook(uuidToDelete);
    	uuidToDelete = null;
    	adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		if (bookshelf.getNotebookList().size() <= 1) {
			Toast.makeText(getApplicationContext(), 
					"Cannot delete last notebook", Toast.LENGTH_LONG);
			return true;
		}
		Notebook nb = bookshelf.getNotebookList().get(position);
		deleteAfterConfirmation(nb.getUUID());
		showDialog(DIALOG_DELETE_NOTEBOOK);
		Log.d(TAG, "Long click: "+nb.getTitle());
		return true;
	}

	
    private static final int DIALOG_DELETE_NOTEBOOK = 0;

    
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE_NOTEBOOK:
			return (Dialog)create_dialog_delete();
		}
		return null;
	}
	
	private AlertDialog create_dialog_delete() {
		DialogInterface.OnClickListener dialogClickListener = 
			new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int button) {
		        switch (button){
		        case DialogInterface.BUTTON_NEGATIVE:  break;
		        case DialogInterface.BUTTON_POSITIVE:
		        	deleteConfirmation();
		            break;
		        }
		    }};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Deleting notebook, are you sure?")
			.setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener);
		return builder.create();
	}
	
 
}
