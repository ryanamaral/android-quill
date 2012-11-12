package com.write.Quill.bookshelf;

import java.util.LinkedList;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.data.StorageAndroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * Show the list of available notebooks. Single click loads the 
 * notebook, long click brings up options menu. 
 * 
 * @author vbraun
 *
 */
public class BookshelfActivity 
	extends ListActivity
	implements OnItemClickListener, OnItemLongClickListener{
	
	private final static String TAG = "BookshelfActivity";
	
	protected BookshelfAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
      	StorageAndroid.initialize(getApplicationContext());

		adapter = new BookshelfAdapter(getApplicationContext());
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnItemLongClickListener(this);
	}
	
	@Override
	protected void onResume() {
        ActivityBase.quillIncRefcount();
		super.onResume();
		Bookshelf.sortBookPreviewList();
		adapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        ActivityBase.quillDecRefcount();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == adapter.getCount()-1) {
			showNewNotebookDialog();
			return;
		}
		BookPreview nb = Bookshelf.getBookPreviewList().get(position);
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		bookshelf.setCurrentBook(nb);
		finish();
	}
	
	private void deleteIsConfirmed(UUID uuidToDelete) {
    	Bookshelf.getBookshelf().deleteBook(uuidToDelete);
    	adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == adapter.getCount()-1) 
			return false;  
		showLongClickDialog(position);
		return true;
	}
	
	public static class DeleteConfirmationFragment extends DialogFragment {
		private BookPreview notebook;

		public static DeleteConfirmationFragment newInstance(int position) {
	    	DeleteConfirmationFragment frag = new DeleteConfirmationFragment();
	        Bundle args = new Bundle();
	        args.putInt("position", position);
	        frag.setArguments(args);
	        return frag;
	    }
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        int position = getArguments().getInt("position");
	    	LinkedList<BookPreview> notebooks = Bookshelf.getBookPreviewList();
			notebook = notebooks.get(position);
	        
			DialogInterface.OnClickListener dialogClickListener = 
					new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int button) {
				        switch (button){
				        case DialogInterface.BUTTON_NEGATIVE:  break;
				        case DialogInterface.BUTTON_POSITIVE:
				        	((BookshelfActivity)getActivity()).deleteIsConfirmed(notebook.getUUID());
				            break;
				        }
				    }};
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.delete_notebook_message)
					.setPositiveButton(android.R.string.yes, dialogClickListener)
				    .setNegativeButton(android.R.string.no,  dialogClickListener);
				return builder.create();
	    }
	}
 

	void showLongClickDialog(int position) {
	    DialogFragment newFragment = LongClickDialogFragment.newInstance(
	            R.string.edit_notebook_title, position);
	    newFragment.show(getFragmentManager(), "longClickDialog");
	}

	void showNewNotebookDialog() {
		Bookshelf bookshelf = Bookshelf.getBookshelf();
		bookshelf.newBook(getString(R.string.new_notebook_default_title));
		int position = Bookshelf.getBookPreviewList().indexOf(Bookshelf.getCurrentBookPreview());
		Assert.assertTrue(position >= 0);
	    DialogFragment newFragment = LongClickDialogFragment.newInstance(
	            R.string.edit_notebook_title_new, position);
	    newFragment.show(getFragmentManager(), "newNotebookDialog");
	}
	
	void showDeleteConfirmationDialog(int position) {
	    DialogFragment newFragment = DeleteConfirmationFragment.newInstance(position);
	    newFragment.show(getFragmentManager(), "deleteConfirmationDialog");
	}

}

