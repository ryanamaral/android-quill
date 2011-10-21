package com.write.Quill;

import com.write.Quill.TagManager.TagSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.text.Layout.Alignment;

public class TagCloudView extends View {
	private static final String TAG = "TagCloudView";

	private TagSet tags;
	private final TextView text;
	private final TextPaint normal, highlight;
	
	public TagCloudView(Context context, AttributeSet attrs) {
		super(context, attrs);
		text = new TextView(context);
		normal = new TextPaint();
		normal.setTextSize(18f);
		normal.setTypeface(Typeface.SERIF);
		normal.setAntiAlias(true);
		highlight = new TextPaint();
		highlight.setTextSize(24f);
		highlight.setTypeface(Typeface.SERIF);
		highlight.setShadowLayer(10, 0, 0, Color.BLUE);
		highlight.setAntiAlias(true);
	}
		
	public void setTagSet(TagSet mTags) {
		tags = mTags;
	}

	
	@Override
	public void onDraw(Canvas canvas) {
		StaticLayout layout;
		layout = new StaticLayout(
				"First Tag", normal, 200, 
				Alignment.ALIGN_NORMAL, 1, 0, false);
		layout.draw(canvas);
		String s = "Defines the length of the fading edges";
		canvas.drawText(s, 100, 200, highlight);		
	}
}
