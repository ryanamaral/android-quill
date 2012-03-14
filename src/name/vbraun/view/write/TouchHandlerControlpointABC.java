package name.vbraun.view.write;

import java.util.LinkedList;

import name.vbraun.view.write.GraphicsControlpoint.Controlpoint;

import junit.framework.Assert;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;

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
	
	protected TouchHandlerControlpointABC(HandwriterView view, boolean activePen) {
		super(view);
		this.activePen = activePen;
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
				saveGraphics();
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
				return true;
			}
			// Log.v(TAG, "ACTION_DOWN");
			if (!useForWriting(event)) 
				return true;   // eat non-pen events
			penID = event.getPointerId(0);
			return true;
		}
		else if (action == MotionEvent.ACTION_UP) {
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				// Log.v(TAG, "ACTION_UP: Got "+N+" points.");
				view.saveStroke(position_x, position_y, pressure, N);
				N = 0;
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
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
	}

	/**
	 * @return all graphics objects of the given type (e.g. all images)
	 */
	abstract LinkedList<GraphicsControlpoint> getGraphicsObjects();
	
	/**
	 * Create a new graphics object
	 * @param x initial x position
	 * @param y initial y position
	 * @param pressure initial pressure
	 * @return a new object derived from GraphicsControlpoint
	 */
	abstract GraphicsControlpoint newGraphicsObject(float x, float y, float pressure);
	abstract LinkedList<Controlpoint> getControlpoints();
	
	
}
