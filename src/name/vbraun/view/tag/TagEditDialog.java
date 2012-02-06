package name.vbraun.view.tag;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.R.id;
import com.write.Quill.R.layout;
import com.write.Quill.R.string;
import com.write.Quill.data.TagManager;
import com.write.Quill.data.TagManager.Tag;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.EditText;
import android.view.View.OnClickListener;

public class TagEditDialog 
	extends Dialog
	implements OnClickListener {
	private static final String TAG = "TagEditDialog";
	
	private Tag tag;
	
	private EditText editText;
	
	public TagEditDialog(Context context) {
		super(context);
		setContentView(R.layout.edit_tag_dialog);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		editText = (EditText)findViewById(R.id.edit_tag_text);
		setTitle(R.string.edit_tag_title);
		findViewById(R.id.edit_tag_button).setOnClickListener((android.view.View.OnClickListener) this);
		findViewById(R.id.edit_tag_cancel).setOnClickListener(this);
	}

	@Override
	public void onClick(View button) {
		switch (button.getId()) {
		case R.id.edit_tag_button:
			String newTag = editText.getText().toString();
			if (newTag.equals(tag.toString())) {
				Log.d(TAG, "No change");
				dismiss();
			}
			changeTag(newTag);
			dismiss();
		case R.id.edit_tag_cancel:
			dismiss();
		}
	}

	public void setTag(Tag tag) {
		this.tag = tag;
		Log.d(TAG, "setTag "+tag+" "+editText);
		Assert.assertNotNull(tag);
		editText.setText(tag.toString());
	}
	
	
	private void changeTag(String newTag) {
		tag.rename(editText.getText().toString());
	}

}