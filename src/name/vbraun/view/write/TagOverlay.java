package name.vbraun.view.write;

import java.util.ListIterator;

import com.write.Quill.Bookshelf;

import name.vbraun.view.write.TagManager.Tag;
import name.vbraun.view.write.TagManager.TagSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class TagOverlay {
	private static final String TAG = "TagOverlay";
	protected static final float TEXT_SIZE = 10f;
	protected static final int MARGIN = 10; 
	
	private final TagSet tagSet;
	
	private final TextPaint style = new TextPaint();
	private final Rect rect = new Rect();
	private final StaticLayout layout;
	
	public TagOverlay(TagSet ts) {
		TagSet filter = Bookshelf.getCurrentBook().getFilter();
		tagSet = ts;
		style.setTextAlign(Align.RIGHT);
		style.setAntiAlias(true);
		style.setColor(Color.DKGRAY);
		String s = "";
		if (ts.size() > 0 || filter.size() > 0) {
			s = "Tags:";
			ListIterator<Tag> iter = ts.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				s += "\n" + t.toString();
				if (filter.contains(t))
					s += " (required)";
			}
			iter = filter.tagIterator();
			while (iter.hasNext()) {
				Tag t = iter.next();
				if (!ts.contains(t)) 
					s += "\n" + t.toString() + " (missing)";
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
