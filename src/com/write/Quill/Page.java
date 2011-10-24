package com.write.Quill;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import com.write.Quill.Stroke.PenType;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;


public class Page {
	private static final String TAG = "Page";
	private final Background background = new Background();
	
	public enum PaperType {
		EMPTY, RULED, QUAD, HEX
	}
	
	public static class PaperTypeName {
		PaperTypeName(CharSequence n, PaperType t) { name = n; type = t; }
		protected CharSequence name;
		protected PaperType type;
	}
	
	public static final PaperTypeName[] PaperTypes = {
		new PaperTypeName("Blank",  PaperType.EMPTY),
		new PaperTypeName("Legal ruled",  PaperType.RULED),
		new PaperTypeName("Quad paper",  PaperType.QUAD),
	};
	
	// persistent data
	public final LinkedList<Stroke> strokes = new LinkedList<Stroke>();
	public final TagManager.TagSet tags;
	public float aspect_ratio = AspectRatio.Table[0].ratio;
	protected boolean is_readonly = false;
	protected PaperType paper_type = PaperType.RULED;
	
	// coordinate transformation Stroke -> screen
	protected Transformation transformation = new Transformation();
	
	protected boolean is_modified = false;

	private final RectF mRectF = new RectF();
	private final Paint paint = new Paint();
	
	
	public boolean is_empty() {
		return strokes.isEmpty();
	}
	
	protected void touch() {
		is_modified = true;
	}

	public void setReadonly(boolean ro) {
		is_readonly = ro;
		is_modified = true;
	}
	
	public void setPaperType(PaperType type) {
		paper_type = type;
		is_modified = true;
		background.setPaperType(paper_type);
	}
	
	public void setAspectRatio(float aspect) {
		aspect_ratio = aspect;
		is_modified = true;
		background.setAspectRatio(aspect_ratio);
	}
	
	protected void setTransform(float dx, float dy, float s) {
		transformation.offset_x = dx;
		transformation.offset_y = dy;
		transformation.scale = s;
		ListIterator<Stroke> siter = strokes.listIterator();
	    while (siter.hasNext())
	    	siter.next().setTransform(transformation);
	}
	
	protected void setTransform(Transformation newTrans) {
		transformation.offset_x = newTrans.offset_x;
		transformation.offset_y = newTrans.offset_y;
		transformation.scale = newTrans.scale;
	    ListIterator<Stroke> siter = strokes.listIterator();
	    while (siter.hasNext())
	    	siter.next().setTransform(transformation);
	}

	// set transform but clamp the offset such that the page stays visible
	protected void setTransform(float dx, float dy, float s, Canvas canvas) {
		float W = canvas.getWidth();
		float H = canvas.getHeight();
		dx = Math.min(dx, 2*W/3);
		dx = Math.max(dx,   W/3 - s*aspect_ratio);
		dy = Math.min(dy, 2*H/3);
		dy = Math.max(dy,   H/3 - s);
		setTransform(dx, dy, s);
	}
	
	protected void setTransform(Transformation newTrans, Canvas canvas) {
		setTransform(newTrans.offset_x, newTrans.offset_y, newTrans.scale, canvas);
	}

	
	protected Transformation getTransform() {
		return transformation;
	}
	
	public void addStroke(Stroke s) {
		s.setTransform(transformation);
		s.applyInverseTransform();
		s.computeBoundingBox(); // simplify might make the box smaller
		s.simplify();
		strokes.add(s);
		is_modified = true;
	}

	public void draw(Canvas canvas, RectF bounding_box) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		canvas.save();
		canvas.clipRect(bounding_box);
		background.draw(canvas, bounding_box, transformation);
		while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
		   	if (!canvas.quickReject(s.get_bounding_box(), 
		   				 			Canvas.EdgeType.AA))
		   		s.render(canvas);
	    }
		canvas.restore();
	}
	
	public Stroke findStrokeAt(float x, float y, float radius) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
			if (!s.get_bounding_box().contains(x,y)) continue;
			if (s.distance(x,y) < radius)
				return s;
		}
		return null;
	}

	public boolean eraseStrokesIn(RectF r, Canvas canvas) {
		mRectF.set(r);
	    ListIterator<Stroke> siter = strokes.listIterator();
	    boolean need_redraw = false;
	    while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
			if (!RectF.intersects(r, s.get_bounding_box())) continue;
			if (s.intersects(r)) {
				mRectF.union(s.get_bounding_box());
				siter.remove();
				need_redraw = true;
			}
		}
	    if (need_redraw) {
	    	draw(canvas, mRectF);
			is_modified = true;
	    }
	    return need_redraw;
	}
	
	public void draw(Canvas canvas) {
		mRectF.set(0,0,canvas.getWidth(), canvas.getHeight());
		draw(canvas, mRectF);
	}
	
	
	public void writeToStream(DataOutputStream out) throws IOException {
		out.writeInt(3);  // protocol #1
		tags.write_to_stream(out);
		out.writeInt(paper_type.ordinal());
		out.writeInt(0); // reserved1
		out.writeInt(0); // reserved2
		out.writeBoolean(is_readonly);
		out.writeFloat(aspect_ratio);
		out.writeInt(strokes.size());
		ListIterator<Stroke> siter = strokes.listIterator(); 
		while (siter.hasNext())
			siter.next().writeToStream(out);
	}
	
	public Page() {
		tags = TagManager.newTagSet();
	}

	public Page(Page template) {
		tags = template.tags.copy();
		setPaperType(template.paper_type);
		setAspectRatio(template.aspect_ratio);
		setTransform(template.transformation);
	}
	

	public Page(DataInputStream in) throws IOException {
		tags = TagManager.newTagSet();
		
		int version = in.readInt();
		if (version == 1) {
			paper_type = PaperType.EMPTY;
		} else if (version == 2) {
			paper_type = PaperType.values()[in.readInt()];
			in.readInt();
			in.readInt();
		} else if (version == 3) {
			tags.set(TagManager.loadTagSet(in));
			paper_type = PaperType.values()[in.readInt()];
			in.readInt();
			in.readInt();
		} else 
			throw new IOException("Unknown version!");
		is_readonly = in.readBoolean();
		aspect_ratio = in.readFloat();
		int N = in.readInt();
		for (int i=0; i<N; i++) {
			strokes.add(new Stroke(in));
		}
		background.setAspectRatio(aspect_ratio);
		background.setPaperType(paper_type);
	}
	
	public Bitmap renderBitmap(int width, int height) {
		Transformation backup = transformation;
		Transformation newTrans = new Transformation();
		newTrans.offset_x = 0;
		newTrans.offset_y = 0;
		newTrans.scale = Math.min(height, width/aspect_ratio);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		setTransform(newTrans);
		draw(c);
		setTransform(backup);
		return bitmap;
	}
}


