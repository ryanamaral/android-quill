package name.vbraun.view.write;

import java.util.Currency;
import java.util.LinkedList;

import name.vbraun.view.write.GraphicsControlpoint.Controlpoint;

import junit.framework.Assert;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.BounceInterpolator;

/**
 * Base class for touch handles than manipulate control points
 * @author vbraun
 *
 */
public abstract class TouchHandlerControlpointABC 
	extends TouchHandlerABC {
	private final static String TAG = "TouchHandlerControlpointABC";

	private final boolean activePen;
	
	private int penID = -1;
	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldPressure, newPressure; 
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private float oldX1, oldY1, newX1, newY1;  // for 1st finger
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;
	
	private Controlpoint activeControlpoint = null;
	
	protected TouchHandlerControlpointABC(HandwriterView view, boolean activePen) {
		super(view);
		this.activePen = activePen;
		view.invalidate(); // make control points appear
	}
	
	@Override
	protected void interrupt() {
		super.interrupt();
		penID = fingerId1 = fingerId2 = -1;
	}

	@Override
	protected boolean onTouchEvent(MotionEvent event) {
		if (activePen) 
			return onTouchEventActivePen(event);
		else 
			return onTouchEventPassivePen(event);
	}

	protected boolean onTouchEventPassivePen(MotionEvent event) {
		// TODO
		return onTouchEventActivePen(event);
	}
	
	protected GraphicsControlpoint newGraphicsObject = null;
	
	protected boolean onTouchEventActivePen(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			if (getMoveGestureWhileWriting() && fingerId1 != -1 && fingerId2 == -1) {
				int idx1 = event.findPointerIndex(fingerId1);
				if (idx1 != -1) {
					oldX1 = newX1 = event.getX(idx1);
					oldY1 = newY1 = event.getY(idx1);
				}
			}
			if (getMoveGestureWhileWriting() && fingerId2 != -1) {
				Assert.assertTrue(fingerId1 != -1);
				int idx1 = event.findPointerIndex(fingerId1);
				int idx2 = event.findPointerIndex(fingerId2);
				if (idx1 == -1 || idx2 == -1) return true;
				newX1 = event.getX(idx1);
				newY1 = event.getY(idx1);
				newX2 = event.getX(idx2);
				newY2 = event.getY(idx2);		
				view.invalidate();
				return true;
			}
			if (penID == -1) return true;
			int penIdx = event.findPointerIndex(penID);
			if (penIdx == -1) return true;
			
			oldT = newT;
			newT = System.currentTimeMillis();
			// Log.v(TAG, "ACTION_MOVE index="+pen+" pointerID="+penID);
			oldX = newX;
			oldY = newY;
			oldPressure = newPressure;
			newX = event.getX(penIdx);
			newY = event.getY(penIdx);
			newPressure = event.getPressure(penIdx);
			if (newT-oldT > 300) { // sometimes ACTION_UP is lost, why?
				Log.v(TAG, "Timeout in ACTION_MOVE, "+(newT-oldT));
				oldX = newX; oldY = newY;
				saveGraphics(activeControlpoint.getGraphics());
			}
			drawOutline(oldX, oldY, newX, newY, oldPressure, newPressure);
			return true;
		}		
		else if (action == MotionEvent.ACTION_DOWN) {
			Assert.assertTrue(event.getPointerCount() == 1);
			newT = System.currentTimeMillis();
			if (useForTouch(event) && getDoubleTapWhileWriting() && Math.abs(newT-oldT) < 250) {
				// double-tap
				view.centerAndFillScreen(event.getX(), event.getY());
				penID = fingerId1 = fingerId2 = -1;
				return true;
			}
			oldT = newT;
			if (useForTouch(event) && getMoveGestureWhileWriting() && event.getPointerCount()==1) {
				fingerId1 = event.getPointerId(0); 
				fingerId2 = -1;
				newX1 = oldX1 = event.getX(); 
				newY1 = oldY1 = event.getY();
			}
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				activeControlpoint = null;
				return true;
			}
			// Log.v(TAG, "ACTION_DOWN");
			if (!useForWriting(event)) 
				return true;   // eat non-pen events
			penID = event.getPointerId(0);
			bBox.setEmpty();
			activeControlpoint = findControlpoint(event.getX(), event.getY());
			if (activeControlpoint == null) { 
				// none within range, create new graphics
				newGraphicsObject = newGraphics(event.getX(), event.getY(), event.getPressure());
				activeControlpoint = newGraphicsObject.initialControlpoint();
			}
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				// Log.v(TAG, "ACTION_UP: line finished "+activeControlpoint);
				if (newGraphicsObject != null) {
					saveGraphics(newGraphicsObject);
					newGraphicsObject = null;
				}
				view.callOnStrokeFinishedListener();
			} else if (getMoveGestureWhileWriting() && 
						(id == fingerId1 || id == fingerId2) &&
						fingerId1 != -1 && fingerId2 != -1) {
				Page page = getPage();
				float dx = page.transformation.offset_x + (newX1-oldX1+newX2-oldX2)/2;
				float dy = page.transformation.offset_y + (newY1-oldY1+newY2-oldY2)/2; 
				page.setTransform(dx, dy, page.transformation.scale, view.canvas);
				page.draw(view.canvas);
				view.invalidate();				
			}
			penID = fingerId1 = fingerId2 = -1;
			return true;
		}
		else if (action == MotionEvent.ACTION_CANCEL) {
			// e.g. you start with finger and use pen
			// if (event.getPointerId(0) != penID) return true;
			Log.v(TAG, "ACTION_CANCEL");
			penID = fingerId1 = fingerId2 = -1;
			newGraphicsObject = null;
			activeControlpoint = null;
			getPage().draw(view.canvas);
			view.invalidate();
			return true;
		}
		else if (action == MotionEvent.ACTION_POINTER_DOWN) {  // start move gesture
			if (fingerId1 == -1) return true; // ignore after move finished
			if (fingerId2 != -1) return true; // ignore more than 2 fingers
			int idx2 = event.getActionIndex();
			oldX2 = newX2 = event.getX(idx2);
			oldY2 = newY2 = event.getY(idx2);
			float dx = newX2-newX1;
			float dy = newY2-newY1;
			float distance = FloatMath.sqrt(dx*dx+dy*dy);
			if (distance >= getMoveGestureMinDistance()) {
				fingerId2 = event.getPointerId(idx2);
			}
			// Log.v(TAG, "ACTION_POINTER_DOWN "+fingerId2+" + "+fingerId1+" "+oldX1+" "+oldY1+" "+oldX2+" "+oldY2);
		}
		return false;
	}

	@Override
	protected void onDraw(Canvas canvas, Bitmap bitmap) {
		if (fingerId2 != -1) {
			// move preview by translating bitmap
			canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
			float x = (newX1-oldX1+newX2-oldX2)/2;
			float y = (newY1-oldY1+newY2-oldY2)/2; 
			canvas.drawBitmap(bitmap, x, y, null);
		} else {
			canvas.drawBitmap(bitmap, 0, 0, null);
			drawControlpoints(canvas);
		}
	}
	
	protected void drawControlpoints(Canvas canvas) {
		for (GraphicsControlpoint line : getPage().lineArt) {
			line.drawControlpoints(canvas);
		}
		view.invalidate();
	}

	/**
	 * @return all graphics objects of the given type (e.g. all images)
	 */
	protected abstract LinkedList<GraphicsControlpoint> getGraphicsObjects();
	
	/**
	 * Create a new graphics object
	 * @param x initial x position
	 * @param y initial y position
	 * @param pressure initial pressure
	 * @return a new object derived from GraphicsControlpoint
	 */
	protected abstract GraphicsControlpoint newGraphics(float x, float y, float pressure);
	
	/**
	 * Save the graphics object to the current page
	 * @param graphics
	 */
	protected void saveGraphics(GraphicsControlpoint graphics) {
		view.saveGraphics(graphics);
	}
	
	private final RectF bBox = new RectF();
	private final Rect  rect = new Rect();

	protected void drawOutline(float oldX, float oldY, float newX, float newY, float oldPressure, float newPressure) {
		Assert.assertNotNull(activeControlpoint);
		activeControlpoint.move(newX, newY);
		GraphicsControlpoint graphics = activeControlpoint.getGraphics();
		// Log.v(TAG, "drawOutline "+graphics.getBoundingBoxRoundOut());
		RectF newBoundingBox = graphics.getBoundingBox();
		final float dr = graphics.controlpointRadius();
		newBoundingBox.inset(-dr, -dr);
		bBox.union(newBoundingBox);
		getPage().draw(view.canvas, bBox);
		graphics.draw(view.canvas, graphics.getBoundingBox());
		bBox.roundOut(rect);
		view.invalidate(rect);
		bBox.set(newBoundingBox);
	}

	
	/**
	 * Maximal distance to select control point (measured in dp)
	 */
	private static final float MAX_DISTANCE = 15; 
	
	/**
	 * Find the closest control point to a given screen position
	 * @param xScreen X screen coordinate
	 * @param yScreen Y screen coordinate
	 * @return The closest Controlpoint or null if there is none within MAX_DISTANCE
	 */
	protected Controlpoint findControlpoint(float xScreen, float yScreen) {
		final Transformation transform = getPage().getTransform();
		final float x = transform.inverseX(xScreen);
		final float y = transform.inverseY(yScreen);
		final float rMax = MAX_DISTANCE * view.screenDensity / transform.scale;
		
		float rMin2 = rMax * rMax;
		Controlpoint closest = null;
		for (GraphicsControlpoint graphics : getGraphicsObjects())
			for (Controlpoint p : graphics.controlpoints) {
				final float dx = x-p.x;
				final float dy = y-p.y;
				final float r2 = dx*dx+dy*dy;
				if (r2 < rMin2) {
					rMin2 = r2;
					closest = p;
				}
			}
		return closest;
	}
	
}
