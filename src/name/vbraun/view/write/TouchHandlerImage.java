package name.vbraun.view.write;

import java.util.LinkedList;

import android.util.Log;

public class TouchHandlerImage extends TouchHandlerControlpointABC {
	private static final String TAG = "TouchHandlerImage";

	protected TouchHandlerImage(HandwriterView view) {
		super(view, view.getOnlyPenInput());
	}

	@Override
	protected LinkedList<GraphicsControlpoint> getGraphicsObjects() {
		return (LinkedList<GraphicsControlpoint>) getPage().images;
	}

	protected float maxDistanceControlpointScreen() {
		return 25f;
	}

	@Override
	protected GraphicsControlpoint newGraphics(float x, float y, float pressure) {
		GraphicsImage image = new GraphicsImage(
				getPage().getTransform(), x, y);
		return image;
	}

	@Override
	protected void destroy() {
	}

}
