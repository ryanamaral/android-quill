package name.vbraun.view.write;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class HandwriterInputConnection extends android.view.inputmethod.BaseInputConnection {
	private static final String TAG = "HandwriterInputConnection"; 

	private SpannableStringBuilder editable;
	private View textView;

	public HandwriterInputConnection(View targetView, boolean fullEditor) {
		super(targetView, fullEditor);
		textView = targetView;
	}

	public Editable getEditable() {
    	Log.d(TAG, "getEditable");
		if (editable == null) {
			editable = (SpannableStringBuilder) Editable.Factory.getInstance()
					.newEditable("Placeholder");
	    }
		return editable;
	}

    public boolean commitText(CharSequence text, int newCursorPosition) {
    	Log.d(TAG, "commitText "+(String)text+" @ "+newCursorPosition);
        editable.append(text);
        textView.invalidate();
        // textView.setText(text);
        return true;
    }
}
