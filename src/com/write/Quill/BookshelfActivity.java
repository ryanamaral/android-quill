package com.write.Quill;

import java.util.LinkedList;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.data.StorageAndroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;

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
					.setPositiveButton(R.string.delete_notebook_yes, dialogClickListener)
				    .setNegativeButton(R.string.delete_notebook_no,  dialogClickListener);
				return builder.create();
	    }
	}
 
	public static class LongClickDialogFragment 
		extends DialogFragment 
		implements OnClickListener {
		private static final String TAG = "LongClickDialogFragment";
	
		private int position;
		private boolean is_new_notebook_dialog;
		
		private BookPreview notebook;
		private Button okButton, cancelButton, exportButton, deleteButton;
		private EditText text;
		
	    public static LongClickDialogFragment newInstance(int title, int position) {
	        LongClickDialogFragment frag = new LongClickDialogFragment();
	        Bundle args = new Bundle();
	        args.putInt("title", title);
	        args.putInt("position", position);
	        frag.setArguments(args);
	        return frag;
	    }

	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        int title = getArguments().getInt("title");
	        position = getArguments().getInt("position");
	    	Dialog dialog = new Dialog(getActivity());
	    	dialog.setContentView(R.layout.edit_notebook_dialog);
	    	dialog.setTitle(title);	        
	    	
	    	LinkedList<BookPreview> notebooks = Bookshelf.getBookPreviewList();
	    	Log.d(TAG, "onCreateDialog "+ Bookshelf.getCount() + " "+position);
			notebook = notebooks.get(position);
			
			text = (EditText)dialog.findViewById(R.id.edit_notebook_title);
			text.setText(notebook.getTitle());
	    	text.setOnKeyListener(new View.OnKeyListener() {
	            @Override
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if (keyCode == KeyEvent.KEYCODE_ENTER) {
	                	EditText editText = (EditText)v;
	                	String text = editText.getText().toString();
	                    int editTextRowCount = text.split("\n").length;
	                    if (editTextRowCount >= 3) return true;
	                }
	                return false;
	            }});
			
			okButton     = (Button)dialog.findViewById(R.id.edit_notebook_button);
			cancelButton = (Button)dialog.findViewById(R.id.edit_notebook_cancel);
			exportButton = (Button)dialog.findViewById(R.id.edit_notebook_export);
			deleteButton = (Button)dialog.findViewById(R.id.edit_notebook_delete);
			okButton.setOnClickListener(this);
			cancelButton.setOnClickListener(this);
			exportButton.setOnClickListener(this);
			deleteButton.setOnClickListener(this);
		
			if (notebooks.size()==1)
				deleteButton.setEnabled(false);
			is_new_notebook_dialog = (title == R.string.edit_notebook_title_new);
			if (is_new_notebook_dialog) {
				deleteButton.setVisibility(View.INVISIBLE);
				exportButton.setVisibility(View.INVISIBLE);
			}
	    	return dialog;
	    }
	    
	    @Override
	    public void onClick(View v) {
    		Bookshelf bookshelf = Bookshelf.getBookshelf();
	    	switch (v.getId()) {
	    	case R.id.edit_notebook_button:
	    		BookPreview previous = Bookshelf.getCurrentBookPreview();
	    		String title = text.getText().toString();
	    		if (title.equals(Bookshelf.getCurrentBook().getTitle())) return;
	    		bookshelf.setCurrentBook(notebook);
	    		Bookshelf.getCurrentBook().setTitle(title); 
	    		bookshelf.setCurrentBook(previous);
	    		notebook.reload();
	    		((BookshelfActivity)getActivity()).adapter.notifyDataSetChanged();
	    		dismiss();
	    		break;
	    	case R.id.edit_notebook_cancel:
	    		if (is_new_notebook_dialog) {
	    			BookshelfActivity activity = ((BookshelfActivity)getActivity());
	    	    	Bookshelf.getBookshelf().deleteBook(notebook.getUUID());	    			
	    			activity.adapter.notifyDataSetChanged();
	    		}
	    		dismiss();
	    		break;
	    	case R.id.edit_notebook_export:
	    		bookshelf.setCurrentBook(notebook);
	    		Intent exportIntent = new Intent(getActivity(), ExportActivity.class);
	    		exportIntent.putExtra("filename", text.getText().toString());
	    		startActivity(exportIntent);
	    		dismiss();
	    		break;
	    	case R.id.edit_notebook_delete:
	    		dismiss();
	    		((BookshelfActivity)getActivity()).showDeleteConfirmationDialog(position);
	    		break;
	    	}
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

