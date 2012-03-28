package com.write.Quill;

import java.util.ListIterator;

import name.vbraun.view.write.Overlay;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class TagOverlay implements Overlay {
	private static final String TAG = "TagOverlay";
	protected static final float TEXT_SIZE = 10f;
	protected static final int MARGIN = 10; 
	
	private final TagSet tagSet;
	
	private final TextPaint style = new TextPaint();
	private final Rect rect = new Rect();
	private final StaticLayout layout;
	
	private final boolean right;
	
	public TagOverlay(Context context, TagSet ts, boolean right) {
		this.right = right;
		tagSet = ts;
		String s = constructTagString(context);
		layout = new StaticLayout(
				s, style, 
				300, Alignment.ALIGN_NORMAL, 1, 0, false);		
	}
	
	public TagOverlay(Context context, TagSet ts, int pageNumber, boolean right) {
		this.right = right;
		tagSet = ts;
		String s = constructTagString(context);
		s += "\n"; 
		s += context.getString(R.string.tag_overlay_page_number, pageNumber);
		layout = new StaticLayout(s, style, 
				300, Alignment.ALIGN_NORMAL, 1, 0, false);		
	}
	
	private String constructTagString(Context context) {
		TagSet filter = Bookshelf.getCurrentBook().getFilter();
		style.setTextAlign(right ? Align.RIGHT : Align.LEFT);
		style.setAntiAlias(true);
		style.setColor(Color.DKGRAY);
		String s = "";
		if (tagSet.size() > 0 || filter.size() > 0) {
			s = context.getString(R.string.tag_overlay_tags);
			ListIterator<Tag> iter = tagSet.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				s += "\n" + t.toString();
				if (filter.contains(t))
					s += " " + context.getString(R.string.tag_overlay_required);
			}
			iter = filter.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				if (!tagSet.contains(t)) 
					s += "\n" + t.toString() + 
						 " " + context.getString(R.string.tag_overlay_missing);
			}
		}
		return s;
	}
	
	public void draw(Canvas canvas) {
		canvas.save();
		if (right)
			canvas.translate(canvas.getWidth()-MARGIN, canvas.getHeight()-MARGIN-layout.getHeight());
		else
			canvas.translate(MARGIN, canvas.getHeight()-MARGIN-layout.getHeight());
		layout.draw(canvas);
		canvas.restore();
	}
	
}
