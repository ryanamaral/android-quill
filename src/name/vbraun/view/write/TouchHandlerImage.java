package name.vbraun.view.write;

import java.util.LinkedList;

import android.util.Log;

public class TouchHandlerImage extends TouchHandlerControlpointABC {
	private static final String TAG = "TouchHandlerImage";

	protected TouchHandlerImage(HandwriterView view) {
		super(view, view.getOnlyPenInput());
	}

	private LinkedList<GraphicsControlpoint> graphicsObjectsCache = new LinkedList<GraphicsControlpoint>();
	
	@Override
	protected LinkedList<GraphicsControlpoint> getGraphicsObjects() {
		LinkedList<GraphicsImage> images = getPage().images;
		if (images.equals(graphicsObjectsCache))
			return graphicsObjectsCache;
		graphicsObjectsCache.clear();
		for (GraphicsControlpoint img : images)
			graphicsObjectsCache.add(img);
		return graphicsObjectsCache;
	}

	protected float maxDistanceControlpointScreen() {
		return 25f;
	}
	
	@Override
	protected void onPenUp(GraphicsControlpoint graphics) {
		GraphicsImage image = (GraphicsImage) graphics;
		view.callOnPickImageListener(image.getUuid());
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
