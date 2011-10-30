package com.write.Quill;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;

public class ThumbnailView extends GridView {
	private static final String TAG = "ThumbnailView";
	
	protected static final int PADDING = 10;
	protected ThumbnailAdapter adapter = null;
	private Handler handler = new Handler();

	public void notifyTagsChanged() {
		handler.removeCallbacks(incrementalDraw);
		adapter.notifyTagsChanged();
		postIncrementalDraw();
	}
	
	public ThumbnailView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setChoiceMode(CHOICE_MODE_MULTIPLE_MODAL);
		setClickable(true);
		setFastScrollEnabled(true);
		setGravity(Gravity.CENTER);
		setVerticalSpacing(PADDING);
		adapter = new ThumbnailAdapter(context);
		adapter.thumbnail_width = ThumbnailAdapter.MIN_THUMBNAIL_WIDTH;
        setAdapter(adapter);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 
		int width = getMeasuredWidth();
		int columns = width / (ThumbnailAdapter.MIN_THUMBNAIL_WIDTH+PADDING);
		adapter.thumbnail_width = width / columns - PADDING;
		Log.d(TAG, "onMeasure "+width+ " " + adapter.thumbnail_width);
		setColumnWidth(adapter.thumbnail_width + PADDING);
		setNumColumns(columns);
		adapter.notifyTagsChanged();
		
	    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		adapter.setNumColumns(getNumColumns());
	}

	protected void postIncrementalDraw() {
		handler.removeCallbacks(incrementalDraw);
        handler.postDelayed(incrementalDraw,10);		
	}
	
    private Runnable incrementalDraw = new Runnable() {
  	   public void run() {
  		   boolean rc = adapter.renderThumbnail();
  		   Log.d(TAG, "incrementalDraw "+rc);
  		   if (rc) {
  			   postIncrementalDraw();
  	  		   invalidateViews();
  		   }
  	   }
  	};


}
