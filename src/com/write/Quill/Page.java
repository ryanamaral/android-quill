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
	
	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;
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

	public void set_readonly(boolean ro) {
		is_readonly = ro;
		is_modified = true;
	}
	
	public void set_paper_type(PaperType type) {
		paper_type = type;
		is_modified = true;
		background.setPaperType(paper_type);
	}
	
	public void set_aspect_ratio(float aspect) {
		aspect_ratio = aspect;
		is_modified = true;
		background.setAspectRatio(aspect_ratio);
	}
	
	protected void set_transform(float dx, float dy, float s) {
		offset_x = dx;
		offset_y = dy;
		scale = s;
		transformation.offset_x = offset_x;
		transformation.offset_y = offset_y;
		transformation.scale = scale;
	    ListIterator<Stroke> siter = strokes.listIterator();
	    while (siter.hasNext())
	    	siter.next().set_transform(offset_x, offset_y, scale);
	}

	// set transform but clamp the offset such that the page stays visible
	protected void set_transform(float dx, float dy, float s, Canvas canvas) {
		float W = canvas.getWidth();
		float H = canvas.getHeight();
		dx = Math.min(dx, 2*W/3);
		dx = Math.max(dx,   W/3 - s*aspect_ratio);
		dy = Math.min(dy, 2*H/3);
		dy = Math.max(dy,   H/3 - s);
		set_transform(dx, dy, s);
	}
	
	public void add_stroke(Stroke s) {
		s.set_transform(offset_x, offset_y, scale);
		s.apply_inverse_transform();
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
		   	if (!canvas.quickReject(s.get_bounding_box(), Canvas.EdgeType.AA))
		   		s.render(canvas);
	    }
		canvas.restore();
	}
	
	public Stroke find_stroke_at(float x, float y, float radius) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
			if (!s.get_bounding_box().contains(x,y)) continue;
			if (s.distance(x,y) < radius)
				return s;
		}
		return null;
	}

	public boolean erase_strokes_in(RectF r, Canvas canvas) {
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
	
	
	public void write_to_stream(DataOutputStream out) throws IOException {
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
			siter.next().write_to_stream(out);
	}
	
	public Page() {
		tags = TagManager.newTagSet();
	}

	public Page(Page template) {
		tags = template.tags.copy();
		set_paper_type(template.paper_type);
		set_aspect_ratio(template.aspect_ratio);
		set_transform(template.offset_x, template.offset_y, template.scale);
	}
	

	public Page(DataInputStream in) throws IOException {
		// TODO
		tags = TagManager.newTagSet();
		
		int version = in.readInt();
		if (version == 1) {
			paper_type = PaperType.EMPTY;
		} else if (version == 2) {
			paper_type = PaperType.values()[in.readInt()];
			in.readInt();
			in.readInt();
		} else {
			tags.set(TagManager.loadTagSet(in));
			paper_type = PaperType.values()[in.readInt()];
			in.readInt();
			in.readInt();
		}
		if (version < 0 || version > 3)
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
}




//private class RenderStrokeTask extends AsyncTask<Stroke, Void, Path> {
//protected Path doInBackground(Stroke... s) {
//	for (int i=0; i<s.length; i++)
//		s[i].generate_path();
//	Log.v(TAG, "x=("+s[0].x_min+","+s[0].x_max+"), y=("+s[0].y_min+","+s[0].y_max+")");
//	Bitmap newBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.RGB_565);
//	return newBitmap;
//}
//
//protected void onPostExecute(Bitmap result) {
//	//mImageView.setImageBitmap(result);
//}
//}


