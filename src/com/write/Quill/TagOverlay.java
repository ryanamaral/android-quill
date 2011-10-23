package com.write.Quill;

import java.util.ListIterator;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

public class TagOverlay {
	private static final String TAG = "TagOverlay";
	protected static final float TEXT_SIZE = 10f;
	protected static final int MARGIN = 10; 
	
	private final TagSet tagSet;
	
	private final TextPaint style = new TextPaint();
	private final Rect rect = new Rect();
	private final StaticLayout layout;
	
	public TagOverlay(TagSet ts) {
		TagSet filter = Book.getBook().getFilter();
		tagSet = ts;
		style.setTextAlign(Align.RIGHT);
		style.setAntiAlias(true);
		style.setColor(Color.DKGRAY);
		String s = "";
		if (ts.tags.size() > 0) {
			s = "Tags:";
			ListIterator<Tag> iter = ts.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				s += "\n" + t.name;
				if (filter.contains(t))
					s += " (required)";
			}
			iter = filter.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				if (!ts.contains(t)) 
					s += "\n" + t.name + " (missing)";
			}
		}
		layout = new StaticLayout(
				s, style, 
				300, Alignment.ALIGN_NORMAL, 1, 0, false);
	}
	
	public void draw(Canvas canvas) {
		canvas.save();
		canvas.translate(canvas.getWidth()-MARGIN, canvas.getHeight()-MARGIN-layout.getHeight());
		layout.draw(canvas);
		canvas.restore();
	}
	
}
