package name.vbraun.view.tag;

import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;





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
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.text.Layout.Alignment;

public class TagCloudView extends View {
	private static final String TAG = "TagCloudView";
	private static float MAX_TAG_WIDTH = 10f; 
	private static int CLOUD_PAD = 2;
	private static final int HIGHLIGHT = Color.BLUE;

	private TagSet tags;
	private ListIterator<Tag> tagIter = null;

	private final TextView text;
	private final Paint paint = new Paint();
	private final TextPaint styleNormal, styleHighlight;
	private final Rect rect = new Rect();
	private Handler handler = new Handler();
	
	

//	@Override
//	public void setOnTouchListener(View.OnTouchListener listener) {
//		
//	}

	public TagCloudView(Context context, AttributeSet attrs) {
		super(context, attrs);
		text = new TextView(context);
		styleNormal = new TextPaint();
		styleNormal.setTypeface(Typeface.SERIF);
		styleNormal.setColor(Color.DKGRAY);
		styleNormal.setAntiAlias(true);
		styleHighlight = new TextPaint();
		styleHighlight.setTypeface(Typeface.SERIF);
		styleHighlight.setShadowLayer(10, 0, 0, HIGHLIGHT);
		styleHighlight.setColor(Color.BLACK);
		styleHighlight.setAntiAlias(true);
		paint.setARGB(0x10, 0x10, 0, 0);
	}
		
	public void setTagSet(TagSet mTags) {
		tags = mTags;
		tagLayout.clear();
		notifyTagsChanged();
	}

	// the number of tags changed
	public void notifyTagsChanged() {
		tagLayout.clear();
		tagIter = tags.allTags().listIterator();
		handler.removeCallbacks(mIncrementalDraw);
        handler.post(mIncrementalDraw);		
		invalidate();
	}
	
	// the selection changed, but not the sizes of the cloud items
	public void notifyTagSelectionChanged() {
		if (tagIter != null) {
			notifyTagsChanged();
			return;
		}
		ListIterator<TagLayout> iter = tagLayout.listIterator();
		while (iter.hasNext()) {
			TagLayout tl = iter.next();
			tl.setHighlight();
		}		
		invalidate();
	}


	private LinkedList<TagLayout> tagLayout = new LinkedList<TagLayout>();
	private int cloudWidth = 0;
	private int cloudHeight = 0;
	private int centerX = 0;
	private int centerY = 0;
	
	private class TagLayout {
		private StaticLayout layout;
		protected final Tag tag;
		protected int x, y;
		protected int width, height;
		protected Rect rect = new Rect();
		protected TextPaint style;
		protected void setHighlight() {
			if (tags.contains(tag)) 
				style.setShadowLayer(10, 0, 0, HIGHLIGHT);
			else
				style.setShadowLayer(0, 0, 0, HIGHLIGHT);	
		}
		protected void initTextStyle() {
			style = new TextPaint();
			style.setTypeface(Typeface.SERIF);
			style.setColor(Color.DKGRAY);
			style.setAntiAlias(true);
			float s = 120 * 
				20f/(20+tags.allTags().size()) * 
				3f/(3+tagLayout.size());
			s = Math.min(s, 90);
			style.setTextSize(s);
		}
		protected TagLayout(Tag mTag) {
			tag = mTag;
			initTextStyle();
			setHighlight();
			layout = new StaticLayout(
					tag.toString(), style, 
					(int)(MAX_TAG_WIDTH*style.getTextSize()), 
					Alignment.ALIGN_NORMAL, 1, 0, false);
			height = layout.getHeight();
			width = 0;
			for (int l=0; l<layout.getLineCount(); l++) {
				float l_width = layout.getLineWidth(l);
				width = Math.max(width, (int)l_width);
			}
			moveToOrigin();
		}
		public void moveToOrigin() {
			x = y = 0;
			rect.set(0, 0, width, height);
			rect.offset(-width/2, -height/2);
			rect.inset(-CLOUD_PAD, -CLOUD_PAD);			
		}
		public void draw(Canvas canvas) {
			canvas.save();
			canvas.translate(centerX, centerY);
			// canvas.drawRect(rect, paint);
			canvas.translate(rect.left+CLOUD_PAD, rect.top+CLOUD_PAD);
			layout.draw(canvas);
			canvas.restore();
		}	
		public void offset(int dx, int dy) {
			rect.offset(dx, dy);
			x += dx;
			y += dy;			
		}
		public void positionSlideInFromInfinty(float vx, float vy) {
			// x += s*vx
			// y += s*vy
			boolean first = true;
			Translation delta = new Translation(vx, vy);
			ListIterator<TagLayout> iter = tagLayout.listIterator();
			while (iter.hasNext()) {
				TagLayout tl = iter.next();
				Translation intercept = delta.findCollision(this, tl);
				if (intercept == null)
					continue;
				if (first) {
					delta.s = intercept.s;
					first = false;
				}
				delta.s = Math.min(delta.s, intercept.s);
			}
			if (!first) {
				delta.apply(this);
			}
		}
		public class Translation {
			protected float vx;
			protected float vy;
			protected float s = 0;
			public Translation(float v_x, float v_y) {
				vx = v_x;
				vy = v_y;
			}
			public void apply(TagLayout tl) {
				int dx = (int)Math.rint(vx*s);
				int dy = (int)Math.rint(vy*s);
				tl.offset(dx, dy);
			}
			public void apply(Rect rect) {
				int dx = (int)Math.rint(vx*s);
				int dy = (int)Math.rint(vy*s);
				rect.offset(dx, dy);
			}
			public Translation findCollision(TagLayout moving, TagLayout obstacle) {
				Translation delta = new Translation(vx, vy);
				Rect r = new Rect();
				boolean collision_x, collision_y;
				if (vx == 0) {  
					collision_x = false;
				} else { 
					if (vx < 0) 
						delta.s = -(moving.rect.left-obstacle.rect.right) / vx;
					else
						delta.s = -(moving.rect.right-obstacle.rect.left) / vx;
					r.set(moving.rect);	
					delta.apply(r);
					collision_x =  (Math.min(r.bottom, obstacle.rect.bottom) >=
							        Math.max(r.top, obstacle.rect.top) - 1);
				}
				float sx = delta.s;
				if (vy == 0) {  
					collision_y = false;
				} else { 
					if (vy < 0) 
						delta.s = -(moving.rect.top-obstacle.rect.bottom) / vy;
					else
						delta.s = -(moving.rect.bottom-obstacle.rect.top) / vy;
					r.set(moving.rect);	
					delta.apply(r);					
					collision_y = (Math.min(r.right, obstacle.rect.right) >=
								   Math.max(r.left, obstacle.rect.left) - 1);
				}
				float sy = delta.s;

				if (collision_x && collision_y)
					delta.s = Math.min(sx, sy);
				else if (collision_x) delta.s = sx;
				else if (collision_y) delta.s = sy;
				else return null;
				return delta;
			}
		}
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		Log.d(TAG, "onSizeChanged "+w+" "+h+" "+oldw+" "+oldh);
		super.onSizeChanged(w, h, oldw, oldh);
		cloudWidth = w;
		cloudHeight = h;
		centerX = w/2;
		centerY = h/2;
		if ((w!=oldw && h!=oldh) && tags != null) {
			handler.removeCallbacks(mIncrementalDraw);
			tagLayout.clear();
			tagIter = tags.allTags().listIterator();
			handler.post(mIncrementalDraw);
		} else 
			invalidate();
	}


	// add a tag to the tag cloud
	// returns whether there was space to add tag
	public boolean addTagToCloud(Tag tag) {
		TagLayout t = new TagLayout(tag);
		float phi_min = 0;
		float r_min = 0;
		int N = 36;
		for (int i=0; i<N; i++) {
			float phi = (float)(2*Math.PI*i)/N;
			t.moveToOrigin();
			t.positionSlideInFromInfinty(FloatMath.cos(phi), FloatMath.sin(phi));
			float r = FloatMath.sqrt(t.x*t.x+t.y*t.y);
			if (i==0) 
				r_min = r;
			else if (r<r_min) {
				r_min = r;
				phi_min = phi;
			}
		}
		t.moveToOrigin();
		t.positionSlideInFromInfinty(FloatMath.cos(phi_min), FloatMath.sin(phi_min));
		tagLayout.add(t);
		return true;
	}

	public Tag findTagAt(float xEvent, float yEvent) {
		int x = (int)(xEvent - centerX);
		int y = (int)(yEvent - centerY);
		ListIterator<TagLayout> iter = tagLayout.listIterator();
		while (iter.hasNext()) {
			TagLayout tl = iter.next();
			if (tl.rect.contains(x,y)) {
				return tl.tag;
			}
		}		
		return null;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		ListIterator<TagLayout> iter = tagLayout.listIterator();
		while (iter.hasNext()) {
			TagLayout tl = iter.next();
			tl.draw(canvas);
		}		
	}
	
	
    private Runnable mIncrementalDraw = new Runnable() {
 	   public void run() {
 		   if (tags == null) return;
 		   //  Log.d(TAG, "mIncrementalDraw "+tags.allTags().size());
 		   if (tagIter != null) {
 				if (!tagIter.hasNext()) {
 					tagIter = null;
 					handler.removeCallbacks(mIncrementalDraw);
 				} else {
 					Tag t = tagIter.next();
 					addTagToCloud(t);
 					invalidate();
 					handler.post(mIncrementalDraw);
 				}
 			}
 	   }
 	};

}
