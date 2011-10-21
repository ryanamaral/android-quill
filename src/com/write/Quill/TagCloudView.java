package com.write.Quill;

import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.text.Layout.Alignment;

public class TagCloudView extends View {
	private static final String TAG = "TagCloudView";
	private static int MAX_TAG_WIDTH = 300; 

	private TagSet tags;
	private ListIterator<Tag> tagIter = null;

	private final TextView text;
	private final Paint paint = new Paint();
	private final TextPaint styleNormal, styleHighlight;
	private final Rect rect = new Rect();
	private Handler handler = new Handler();

	public TagCloudView(Context context, AttributeSet attrs) {
		super(context, attrs);
		text = new TextView(context);
		styleNormal = new TextPaint();
		styleNormal.setTextSize(18f);
		styleNormal.setTypeface(Typeface.SERIF);
		styleNormal.setColor(Color.DKGRAY);
		styleNormal.setAntiAlias(true);
		styleHighlight = new TextPaint();
		styleHighlight.setTextSize(24f);
		styleHighlight.setTypeface(Typeface.SERIF);
		styleHighlight.setShadowLayer(10, 0, 0, Color.BLUE);
		styleHighlight.setColor(Color.BLACK);
		styleHighlight.setAntiAlias(true);
		paint.setARGB(0x10, 0x10, 0, 0);
	}
		
	public void setTagSet(TagSet mTags) {
		tags = mTags;
		tagLayout.clear();
		tagIter = tags.tags.listIterator();
        handler.post(mIncrementalDraw);		
	}

	private LinkedList<TagLayout> tagLayout = new LinkedList<TagLayout>();
	private int cloud_width = 0;
	private int cloud_height = 0;
	private int centerX = 0;
	private int centerY = 0;
	
	private class TagLayout {
		private StaticLayout layout;
		protected final Tag tag;
		protected int width, height;
		protected int x,y;
		protected Rect rect = new Rect();
		protected boolean highlight;
		protected TagLayout(Tag mTag) {
			tag = mTag;
			highlight = tags.contains(tag);
			TextPaint style;
			if (highlight)
				style = styleHighlight;
			else
				style = styleNormal;
			layout = new StaticLayout(
					tag.name, style, MAX_TAG_WIDTH, 
					Alignment.ALIGN_NORMAL, 1, 0, false);
			height = layout.getHeight();
			width = 0;
			for (int l=0; l<layout.getLineCount(); l++) {
				float l_width = layout.getLineWidth(l);
				width = Math.max(width, (int)l_width);
			}
			rect.set(x, y, x+width, y+height);
			rect.inset(-2, -2);
			x = centerX;
			y = centerY;
		}
		public void draw(Canvas canvas) {
			canvas.save();
			canvas.translate(x,y);
			layout.draw(canvas);
			canvas.restore();
			canvas.drawRect(rect, paint);
		}	
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		handler.removeCallbacks(mIncrementalDraw);
		super.onSizeChanged(w, h, oldw, oldh);
		cloud_width = w;
		cloud_height = h;
		centerX = w/2;
		centerY = h/2;
		tagLayout.clear();
		if (tags != null) {
			tagIter = tags.allTags().listIterator();
			handler.post(mIncrementalDraw);
		}
	}


	// add a tag to the tag cloud
	// returns whether there was space to add tag
	public boolean addTagToCloud(Tag tag) {
		TagLayout t = new TagLayout(tag);
		t.y = 30*tagLayout.size();
		tagLayout.add(t);
		return true;
	}

	
	@Override
	public void onDraw(Canvas canvas) {
		ListIterator<TagLayout> iter = tagLayout.listIterator();
		while (iter.hasNext()) {
			TagLayout tl = iter.next();
			tl.draw(canvas);
		}		
		Log.d(TAG, "TagLayout "+tagLayout.size());
	}
	
	
    private Runnable mIncrementalDraw = new Runnable() {
 	   public void run() {
 		   if (tags == null) return;
 		   Log.d(TAG, "mIncrementalDraw "+tags.allTags().size());
 		   if (tagIter != null) {
 				if (!tagIter.hasNext()) {
 					tagIter = null;
 					handler.removeCallbacks(mIncrementalDraw);
 				} else {
 					Tag t = tagIter.next();
 					addTagToCloud(t);
 					invalidate();
 					handler.postDelayed(mIncrementalDraw, 500);
 				}
 			}
 	   }
 	};

}
