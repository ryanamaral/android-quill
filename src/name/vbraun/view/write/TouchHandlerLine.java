package name.vbraun.view.write;

import java.util.LinkedList;

import android.util.Log;

public class TouchHandlerLine extends TouchHandlerControlpointABC {
	private static final String TAG = "TouchHandlerLine";

	protected TouchHandlerLine(HandwriterView view) {
		super(view, view.getOnlyPenInput());
		Log.e(TAG, "ctor");

	}

	@Override
	protected LinkedList<GraphicsControlpoint> getGraphicsObjects() {
		return (LinkedList<GraphicsControlpoint>) getPage().lineArt;
	}

	@Override
	protected GraphicsControlpoint newGraphics(float x, float y, float pressure) {
		Log.e(TAG, "newGraphics");
		GraphicsLine line = new GraphicsLine(
				getPage().getTransform(), x, y, 
				view.getPenThickness(), view.getPenColor());
		return line;
	}

	@Override
	protected void destroy() {
	}

}
