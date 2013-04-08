package name.vbraun.view.write;

import java.util.LinkedList;
import java.util.ListIterator;

import junit.framework.Assert;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;



/**
 * Base class for graphics objects that have control points 
 * (everything except pen strokes, really).
 * @author vbraun
 *
 */
/**
 * @author vbraun
 *
 */
public abstract class GraphicsControlpoint extends Graphics {
	private static final String TAG = "GraphicsControlpoint";

	public class Controlpoint {
		protected float x,y;   // page coordinates
		public Controlpoint(float x, float y) {
			this.x = x;
			this.y = y;
		}
		public Controlpoint(Transformation transform, float x, float y) {
			this.x = transform.inverseX(x);
			this.y = transform.inverseY(y);
		}
		public Controlpoint(final Controlpoint controlpoint) {
			this.x = controlpoint.x;
			this.y = controlpoint.y;
		}
		public void move(float x, float y) {
			this.x = transform.inverseX(x);
			this.y = transform.inverseY(y);
			GraphicsControlpoint.this.controlpointMoved(this);
		}
		public GraphicsControlpoint getGraphics() {
			return GraphicsControlpoint.this;
		}
		public String toString() {
			return "("+x+","+y+")";
		}
		public float screenX() { return transform.applyX(x); };
		public float screenY() { return transform.applyY(y); };
		public Controlpoint copy() { return new Controlpoint(x, y); };
		public void set(final Controlpoint p) { x = p.x; y = p.y; };
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param graphics
	 */
	protected GraphicsControlpoint(final GraphicsControlpoint graphics) {
		super(graphics);
		fillPaint = new Paint(graphics.fillPaint);
		outlinePaint = new Paint(graphics.outlinePaint);
	}
	
	/**
	 * Derived classes must add their control points to this list
	 */
	protected LinkedList<Controlpoint> controlpoints = new LinkedList<Controlpoint>();
	
	protected LinkedList<Controlpoint> backupControlpoints = null;
	
	/**
	 * Backup the controlpoints so that you can restore them later (e.g. user aborted move)
	 */
	protected void backup() {
		if (backupControlpoints == null) {
			backupControlpoints = new LinkedList<Controlpoint>();
			for (Controlpoint p : controlpoints) 
				backupControlpoints.add(p.copy());
		} else {
			ListIterator<Controlpoint> point_iter = controlpoints.listIterator();
			ListIterator<Controlpoint> backup_iter = backupControlpoints.listIterator();
			while (point_iter.hasNext())
				backup_iter.next().set(point_iter.next());				
		}
	}
	
	
	/**
	 * Restore the control points having calling backup() earlier
	 */
	protected void restore() {
		if (backupControlpoints == null) {
			Log.e(TAG, "restore() called without backup()");
			return;
		}
		ListIterator<Controlpoint> point_iter = controlpoints.listIterator();
		ListIterator<Controlpoint> backup_iter = backupControlpoints.listIterator();
		while (point_iter.hasNext())
			point_iter.next().set(backup_iter.next());				
	}
	
	/**
	 * The control point that is active after object creation. 
	 * @return A Controlpoint or null (indicating that there is none active)
	 */
	protected Controlpoint initialControlpoint() {
		return null;
	}
	
	void controlpointMoved(Controlpoint point) {
		recompute_bounding_box = true;
	}
	
	protected GraphicsControlpoint(Tool mTool) {
		super(mTool);
		fillPaint = new Paint();
		fillPaint.setARGB(0x20, 0xff, 0x0, 0x0);
		fillPaint.setStyle(Style.FILL);
		fillPaint.setAntiAlias(true);
		outlinePaint = new Paint();
		outlinePaint.setARGB(0x80, 0x0, 0x0, 0x0);
		outlinePaint.setStyle(Style.STROKE);
		outlinePaint.setStrokeWidth(2.5f);
		outlinePaint.setAntiAlias(true);
	}
	
	/**
	 * By default, the bounding box is the box containing the control points
	 * inset by this much (which you can override in a derived class).
	 * @return
	 */
	protected float boundingBoxInset() { 
		return -1;
	}
	
	protected final Paint fillPaint, outlinePaint;
	
	/**
	 * The (maximal) size of a control point
	 * @return The size in pixels
	 */
	protected float controlpointRadius() {
		return 15;
	}
	
	protected void drawControlpoints(Canvas canvas) {
		for (Controlpoint p : controlpoints) {
			float x = p.screenX();
			float y = p.screenY();
			canvas.drawCircle(x, y, controlpointRadius(), fillPaint);
			canvas.drawCircle(x, y, controlpointRadius(), outlinePaint);
		}
	}
	
	@Override
	protected void computeBoundingBox() {
		ListIterator<Controlpoint> iter = controlpoints.listIterator();
		Assert.assertTrue(iter.hasNext()); // must have at least one control point
		Controlpoint p = iter.next();
		float xmin, xmax, ymin, ymax;
		xmin = xmax = transform.applyX(p.x);
		ymin = ymax = transform.applyY(p.y);
		while (iter.hasNext()) {
			p = iter.next();
			float x = p.screenX();
			xmin = Math.min(xmin, x);
			xmax = Math.max(xmax, x);
			float y = p.screenY();
			ymin = Math.min(ymin, y);
			ymax = Math.max(ymax, y);
		}
		bBoxFloat.set(xmin, ymin, xmax, ymax);
		float extra = boundingBoxInset();
		bBoxFloat.inset(extra, extra);		
		bBoxFloat.roundOut(bBoxInt);
		recompute_bounding_box = false;
	}

	@Override
	public float distance(float x_screen, float y_screen) {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
