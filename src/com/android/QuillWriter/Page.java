package com.android.QuillWriter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import com.android.QuillWriter.Stroke.PenType;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;


public class Page {
	private static final String TAG = "Page";

	public enum PaperType {
		EMPTY, LINES, SQUARE, HEX
	}
	
	public static class AspectRatio {
		AspectRatio(CharSequence n, float a) { name = n; aspect = a; }
		protected CharSequence name;
		protected float aspect;
	}
	
	public static final AspectRatio[] AspectRatios = {
		new AspectRatio("Portrait Screen",  800f/1232f),
		new AspectRatio("Landscape Screen",  1280f/752f),
		new AspectRatio("A4 Paper", 1f/(float)Math.sqrt(2)),
		new AspectRatio("US Letter",  8f/11f),
		new AspectRatio("Projector (4:3)",  4f/3f),
		new AspectRatio("HDTV (16:9)", 16f/9f)
	};
	
	// persistent data
	protected final LinkedList<Stroke> strokes = new LinkedList();
	protected boolean is_readonly = false;
	protected float aspect_ratio = AspectRatios[0].aspect;
	protected PaperType paper_type = PaperType.EMPTY;
	
	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;
	
	protected boolean is_modified = false;

	private final RectF mRectF = new RectF();
	private final RectF paper = new RectF();
	private final Paint paint = new Paint();
	
	public boolean is_empty() {
		return strokes.isEmpty();
	}
	
	private void draw_paper(Canvas canvas, RectF bBox) {
		//Log.v(TAG, "draw_paper at scale "+scale);
		// the paper is 1 high and aspect_ratio wide
		paper.set(offset_x, offset_y, offset_x+aspect_ratio*scale, offset_y+scale);
		if (!paper.contains(bBox))
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
		paint.setARGB(0xff, 0xff, 0xff, 0xff);
		canvas.drawRect(paper, paint);
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
	}
	
	public void set_aspect_ratio(float aspect) {
		aspect_ratio = aspect;
		is_modified = true;
	}
	
	protected void set_transform(float dx, float dy, float s) {
		offset_x = dx;
		offset_y = dy;
		scale = s;
	    ListIterator<Stroke> siter = strokes.listIterator();
	    while (siter.hasNext())
	    	siter.next().set_transform(offset_x, offset_y, scale);
	}
	
	public void add_stroke(Stroke s) {
		s.set_transform(offset_x, offset_y, scale);
		s.apply_inverse_transform();
		strokes.add(s);
		is_modified = true;
	}

	public void draw(Canvas canvas, RectF bounding_box) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		canvas.save();
		canvas.clipRect(bounding_box);
		draw_paper(canvas, bounding_box);
		while(siter.hasNext())
	    {	
			Stroke s = siter.next();	    	
		   	if (!canvas.quickReject(s.get_bounding_box(), Canvas.EdgeType.AA))
		   		s.render(canvas);
	    }
		canvas.restore();
	}
	
	public void draw(Canvas canvas) {
		mRectF.set(0,0,canvas.getWidth(), canvas.getHeight());
		draw(canvas, mRectF);
	}
	
	
	public void write_to_stream(DataOutputStream out) throws IOException {
		out.writeInt(1);  // protocol #1
		out.writeBoolean(is_readonly);
		out.writeFloat(aspect_ratio);
		out.writeInt(strokes.size());
		ListIterator<Stroke> siter = strokes.listIterator(); 
		while (siter.hasNext())
			siter.next().write_to_stream(out);
	}
	
	public Page() {}

	public Page(Page template) {
		set_paper_type(template.paper_type);
		set_aspect_ratio(template.aspect_ratio);
		set_transform(template.offset_x, template.offset_y, template.scale);
	}
	

	public Page(DataInputStream in) throws IOException {
	int version = in.readInt();
		if (version != 1)
			throw new IOException("Unknown version!");
		is_readonly = in.readBoolean();
		aspect_ratio = in.readFloat();
		int N = in.readInt();
		for (int i=0; i<N; i++) {
			strokes.add(new Stroke(in));
		}
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


