package name.vbraun.view.write;

import java.util.LinkedList;
import java.util.ListIterator;

import junit.framework.Assert;

import android.graphics.Canvas;
import android.graphics.RectF;



/**
 * Base class for graphics objects that have control points 
 * (everything except pen strokes, really).
 * @author vbraun
 *
 */
public abstract class GraphicsControlpoint extends Graphics {

	public class Controlpoint {
		protected float x,y;
		public Controlpoint(float x, float y) {
			this.x = x;
			this.y = y;
		}
		public void move(float x, float y) {
			this.x = x;
			this.y = y;
			GraphicsControlpoint.this.controlpointMoved(this);
		}
	}
	
	protected abstract LinkedList<Controlpoint> getControlpoints();
	
	void controlpointMoved(Controlpoint point) {
		recompute_bounding_box = true;
	}
	
	protected GraphicsControlpoint(Tool mTool) {
		super(mTool);
	}

	/**
	 * By default, the bounding box is the box containing the control points
	 * inset by this much (which you can override in a derived class).
	 * @return
	 */
	protected float boundingBoxInset() { 
		return 1;
	}
	
	@Override
	protected void computeBoundingBox() {
		ListIterator<Controlpoint> iter = getControlpoints().listIterator();
		Assert.assertTrue(iter.hasNext()); // must have at least one control point
		Controlpoint p = iter.next();
		float xmin, xmax, ymin, ymax;
		xmin = xmax = p.x;
		ymin = ymax = p.y;
		while (iter.hasNext()) {
			p = iter.next();
			if (p.x < xmin) xmin = p.x;
			if (p.x > xmax) xmax = p.x;
			if (p.y < ymin) xmin = p.y;
			if (p.y > ymax) xmax = p.y;
		}
		bBox.set(xmin, ymin, xmax, ymax);
		float extra = boundingBoxInset();
		bBox.inset(extra, extra);		
		recompute_bounding_box = false;
	}

	@Override
	public float distance(float x_screen, float y_screen) {
		// TODO Auto-generated method stub
		return 0;
	}


}
