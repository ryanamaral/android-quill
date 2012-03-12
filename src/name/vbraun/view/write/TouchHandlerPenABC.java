package name.vbraun.view.write;

import name.vbraun.view.write.Graphics.Tool;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;

public abstract class TouchHandlerPenABC extends TouchHandlerABC {

	protected int N = 0;
	protected static final int Nmax = 1024;
	protected float[] position_x = new float[Nmax];
	protected float[] position_y = new float[Nmax];
	protected float[] pressure = new float[Nmax];

	protected final Paint pen;

	protected TouchHandlerPenABC(HandwriterView view) {
		super(view);
		pen = new Paint();
		pen.setAntiAlias(true);
		pen.setARGB(0xff, 0, 0, 0);	
		pen.setStrokeCap(Paint.Cap.ROUND);
	}
	
	@Override
	protected void interrupt() {
		super.interrupt();
		N = 0;
	}
	
	private final Rect mRect = new Rect();
	
	/**
	 * Set the pen style. Subsequent calls to drawOutline() will use this pen.
	 */
	protected void initPenStyle() {
		int penColor = view.getPenColor();
		pen.setARGB(Color.alpha(penColor), Color.red(penColor), Color.green(penColor), Color.blue(penColor));
		
		float scaledPenThickness = getScaledPenThickness();
		pen.setStrokeWidth(scaledPenThickness);		
	}
	
	protected void drawOutline(float oldX, float oldY, float newX, float newY, float oldPressure, float newPressure) {
		if (view.getToolType() == Tool.FOUNTAINPEN) {
			float scaledPenThickness = getScaledPenThickness() * (oldPressure+newPressure)/2f; 
			pen.setStrokeWidth(scaledPenThickness);
		}

		view.canvas.drawLine(oldX, oldY, newX, newY, pen);
		mRect.set((int)oldX, (int)oldY, (int)newX, (int)newY);
		mRect.sort();
		int extra = -(int)(pen.getStrokeWidth()/2) - 1;
		mRect.inset(extra, extra);
		view.invalidate(mRect);
	}

	protected void saveStroke() {
		view.saveStroke(position_x, position_y, pressure, N);
		N = 0;
	}

}