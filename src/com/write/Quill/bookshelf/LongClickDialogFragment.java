package com.write.Quill.bookshelf;

import java.util.LinkedList;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.export.ExportActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


public class LongClickDialogFragment extends DialogFragment implements OnClickListener {
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
		Log.d(TAG, "onCreateDialog " + Bookshelf.getCount() + " " + position);
		notebook = notebooks.get(position);

		text = (EditText) dialog.findViewById(R.id.edit_notebook_title);
		text.setText(notebook.getTitle());
		text.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					EditText editText = (EditText) v;
					String text = editText.getText().toString();
					int editTextRowCount = text.split("\n").length;
					if (editTextRowCount >= 3)
						return true;
				}
				return false;
			}
		});

		okButton = (Button) dialog.findViewById(R.id.edit_notebook_button);
		cancelButton = (Button) dialog.findViewById(R.id.edit_notebook_cancel);
		exportButton = (Button) dialog.findViewById(R.id.edit_notebook_export);
		deleteButton = (Button) dialog.findViewById(R.id.edit_notebook_delete);
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		exportButton.setOnClickListener(this);
		deleteButton.setOnClickListener(this);

		if (notebooks.size() == 1)
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
		BookshelfActivity activity = ((BookshelfActivity) getActivity());
		switch (v.getId()) {
		case R.id.edit_notebook_button:
			BookPreview previous = Bookshelf.getCurrentBookPreview();
			String title = text.getText().toString();
			if (title.equals(Bookshelf.getCurrentBook().getTitle()))
				return;
			bookshelf.setCurrentBook(notebook);
			Book book = Bookshelf.getCurrentBook();
			book.setTitle(title);
			// book.save();
			bookshelf.setCurrentBook(previous);
			// notebook.reload();
			activity.adapter.notifyDataSetChanged();
			dismiss();
			break;
		case R.id.edit_notebook_cancel:
			if (is_new_notebook_dialog) {
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
			activity.showDeleteConfirmationDialog(position);
			break;
		}
	}
}
