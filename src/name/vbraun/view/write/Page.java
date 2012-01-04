package name.vbraun.view.write;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import com.write.Quill.data.TagManager;
import com.write.Quill.data.TagManager.TagSet;

import junit.framework.Assert;

import name.vbraun.view.write.Graphics.Tool;

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
	
	private TagManager tagManager;
	
	// persistent data
	public final LinkedList<Stroke> strokes = new LinkedList<Stroke>();
	public final TagManager.TagSet tags;
	protected float aspect_ratio = AspectRatio.Table[0].ratio;
	protected boolean is_readonly = false;
	protected Paper.Type paper_type = Paper.Type.RULED;
	protected TextBox backgroundText = new TextBox(Tool.TEXT);
	
	// coordinate transformation Stroke -> screen
	protected Transformation transformation = new Transformation();
	
	protected boolean is_modified = false;

	private final RectF mRectF = new RectF();
	private final Paint paint = new Paint();
	
	public TagSet getTags() {
		return tags;
	}
	
	public boolean is_empty() {
		return strokes.isEmpty();
	}
	
	public void touch() {
		is_modified = true;
	}
	
	public boolean isModified() {
		return is_modified;
	}

	public float getAspectRatio() {
		return aspect_ratio;
	}
	
	public Paper.Type getPaperType() {
		return paper_type;
	}
	
	public boolean isReadonly() {
		return is_readonly;
	}
	
	public void setReadonly(boolean ro) {
		is_readonly = ro;
		is_modified = true;
	}
	
	public void setPaperType(Paper.Type type) {
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
		strokes.add(s);
		s.setTransform(getTransform());
		is_modified = true;
	}
	
	public void removeStroke(Stroke s) {
		strokes.remove(s);
		is_modified = true;
	}

	public void draw(Canvas canvas, RectF bounding_box) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		canvas.save();
		canvas.clipRect(bounding_box);
		background.draw(canvas, bounding_box, transformation);
		backgroundText.draw(canvas, bounding_box);
		while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
		   	if (!canvas.quickReject(s.getBoundingBox(), 
		   				 			Canvas.EdgeType.AA))
		   		s.draw(canvas, bounding_box);
	    }
		canvas.restore();
	}
	
	public Stroke findStrokeAt(float x, float y, float radius) {
	    ListIterator<Stroke> siter = strokes.listIterator();
		while(siter.hasNext()) {	
			Stroke s = siter.next();	    	
			if (!s.getBoundingBox().contains(x,y)) continue;
			if (s.distance(x,y) < radius)
				return s;
		}
		return null;
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
	
	public Page(TagManager tagMgr) {
		tagManager = tagMgr;
		tags = tagManager.newTagSet();
		setPaperType(paper_type);
		setAspectRatio(aspect_ratio);
		setTransform(transformation);
		is_modified = true;
	}

	public Page(Page template) {
		tagManager = template.tagManager;
		tags = template.tags.copy();
		setPaperType(template.paper_type);
		setAspectRatio(template.aspect_ratio);
		setTransform(template.transformation);
		is_modified = true;
	}
	

	public Page(DataInputStream in, TagManager tagMgr) throws IOException {
		tagManager = tagMgr;
		int version = in.readInt();
		if (version == 1) {
			tags = tagManager.newTagSet();
			paper_type = Paper.Type.EMPTY;
		} else if (version == 2) {
			tags = tagManager.newTagSet();
			paper_type = Paper.Type.values()[in.readInt()];
			in.readInt();
			in.readInt();
		} else if (version == 3) {
			tags = tagManager.loadTagSet(in);
			paper_type = Paper.Type.values()[in.readInt()];
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
		Transformation backup = getTransform().copy();
		float scale = Math.min(height, width/aspect_ratio);
		setTransform(0, 0, scale);
		int actual_width  = (int)Math.rint(scale*aspect_ratio);
		int actual_height = (int)Math.rint(scale);
		Bitmap bitmap = Bitmap.createBitmap
			(actual_width, actual_height, Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		draw(c);
		setTransform(backup);
		return bitmap;
	}
}


