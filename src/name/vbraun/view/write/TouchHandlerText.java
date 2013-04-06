package name.vbraun.view.write;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

public class TouchHandlerText extends TouchHandlerABC {
	private final static String TAG = "TouchHandlerText";
	
	private InputMethodManager inputMethodManager;
	private HandwriterInputConnection inputConnection;
	private EditText editText;
	
	protected TouchHandlerText(HandwriterView view) {
		super(view);
		
		view.setFocusable(false);
		view.setFocusableInTouchMode(false);  
		
		inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromInputMethod(view.getWindowToken(), 0);
		inputMethodManager.hideSoftInputFromInputMethod(view.getApplicationWindowToken(), 0);
        inputConnection = null;			

		editText = new EditText(getContext());
		editText.setImeOptions(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		editText.setTextSize(25f);
		editText.setText("Hello Computer");
		editText.setFocusable(true);
		editText.setFocusableInTouchMode(true);  
        
		boolean kbd = (view.getResources().getConfiguration().keyboardHidden == Configuration.KEYBOARDHIDDEN_YES);
		Log.d(TAG, "TouchHandlerText "+kbd);
	}
	
	@Override
	protected void destroy() {
		view.removeView(editText);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	@Override
	protected void draw(Canvas canvas, Bitmap bitmap) {
		Log.d(TAG, "painting text");
		canvas.drawBitmap(bitmap, 0, 0, null);
		editText.draw(canvas); 
	}
	
	@Override
	protected boolean onTouchEvent(MotionEvent event) {
        Log.e(TAG, "onTOUCH");
        if (event.getAction() == MotionEvent.ACTION_UP) {
        	if (editText == null) {
        		view.addView(editText);
        		editText.requestFocus();
        		inputMethodManager.showSoftInput(view, 0);
        	}
        }
        return true;
	}
	
	
	
//	
//	@Override
//	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//		Log.d(TAG, "onCreateInputConnection");
//	    outAttrs.actionLabel = null;
//	    outAttrs.label = "Test text";
//	    // outAttrs.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
//	    outAttrs.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE;
//	    //  outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE;
//		inputConnection = new HandwriterInputConnection(editText, true);
//		page.backgroundText.setEditable(inputConnection.getEditable());
//		return inputConnection;
//	}
//	
//    @Override
//    public boolean onCheckIsTextEditor() {
//        Log.d(TAG, "onCheckIsTextEditor "+(tool_type == Tool.TEXT));
//        return tool_type == Tool.TEXT;
//    }
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//		int action = event.getAction();
//		int keyCode = event.getKeyCode();
//		Log.v(TAG, "KeyEvent "+action+" "+keyCode);
//		if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
//			if (inputConnection != null) {
//				inputConnection.getEditable().insert(2, "\n");
//				return true;
//			}
//		}	
//		return super.dispatchKeyEvent(event);
//    }



}
