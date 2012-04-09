package com.write.Quill.thumbnail;

import com.write.Quill.thumbnail.ThumbnailAdapter.Thumbnail;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.GridView;

public class ThumbnailView extends GridView {
	private static final String TAG = "ThumbnailView";
	
	protected Context context;
	protected static final int PADDING = 10;
	protected ThumbnailAdapter adapter = null;
	private Handler handler = new Handler();

	public void notifyTagsChanged() {
    	Log.d(TAG, "notifyTagsChanged");
		handler.removeCallbacks(incrementalDraw);
		int width = adapter.thumbnail_width;
		adapter = new ThumbnailAdapter(context);
		adapter.thumbnail_width = width;
		adapter.setNumColumns(getNumColumns());
		setAdapter(adapter);
	}
	
	public ThumbnailView(Context c, AttributeSet attrs) {
		super(c, attrs);
		context = c;
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
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Log.d(TAG, "onLayout "+l+" "+t+" "+r+" "+b);
		super.onLayout(changed, l, t, r, b);

		int width = r-l;
		int columns = width / (ThumbnailAdapter.MIN_THUMBNAIL_WIDTH+PADDING);
		if (columns == 0) columns = 1;
		adapter.thumbnail_width = width / columns - PADDING;
		adapter.computeItemHeights();
		setColumnWidth(adapter.thumbnail_width + PADDING);
		setNumColumns(columns);
		adapter.setNumColumns(columns);
	}

	protected void postIncrementalDraw() {
		handler.removeCallbacks(incrementalDraw);
        handler.post(incrementalDraw);		
	}
	
    private Runnable incrementalDraw = new Runnable() {
  	   public void run() {
  		   boolean rc = adapter.renderThumbnail();
  		   // Log.d(TAG, "incrementalDraw "+rc);
  		   if (rc) {
  			   postIncrementalDraw();
  		   }
  	   }
  	};

    public void checkedStateChanged(int position, boolean checked) {
    	adapter.checkedStateChanged(position, checked);
    	invalidateViews();
    }

    public void uncheckAll() {
    	adapter.uncheckAll();
    	invalidateViews();
    }

    @Override
    protected void onDetachedFromWindow() {
    	// Log.e(TAG, "onDetachedFromWindow");
    	handler.removeCallbacks(incrementalDraw);
    	super.onDetachedFromWindow();	
    }
    
    @Override
    protected void onAttachedToWindow() {
    	// Log.e(TAG, "onAttachedToWindow");
    	super.onAttachedToWindow();
        handler.post(incrementalDraw);		
   }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	super.onSizeChanged(w, h, oldw, oldh);
    	// Log.e(TAG, "onSizeChanged");
        handler.post(runInvalidateViews);

    }
    
    private Runnable runInvalidateViews = new Runnable() {
    	public void run() {
    		invalidateViews();
    	};
    };
}
