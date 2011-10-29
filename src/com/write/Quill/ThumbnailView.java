package com.write.Quill;

import android.content.Context;
import android.graphics.Color;
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
	
	public void notifyTagsChanged() {
		adapter.notifyDataSetChanged();
	}
	
	public ThumbnailView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setColumnWidth(ThumbnailAdapter.THUMBNAIL_WIDTH + 2*PADDING);
		setChoiceMode(CHOICE_MODE_MULTIPLE_MODAL);
		setClickable(true);
		setFastScrollEnabled(true);
		setGravity(Gravity.CENTER);
		setVerticalSpacing(PADDING);
		setNumColumns(AUTO_FIT);
	//	setDrawingCacheEnabled(false);
		adapter = new ThumbnailAdapter(context);
        setAdapter(adapter);
	}

	
	
}
