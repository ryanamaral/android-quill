package name.vbraun.view.write;

import java.util.LinkedList;

import name.vbraun.view.write.GraphicsControlpoint.Controlpoint;

import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class TouchHandlerImage extends TouchHandlerControlpointABC {
	private static final String TAG = "TouchHandlerImage";

	protected TouchHandlerImage(HandwriterView view) {
		super(view, view.getOnlyPenInput());
	}

	/**
	 * Called when the user touches with the pen
	 */
	@Override
	protected void onPenDown(Controlpoint controlpoint, boolean isNew) {
		view.getToolBox().startControlpointMove(!isNew, true);		
	}

	@Override
	protected LinkedList<? extends GraphicsControlpoint> getGraphicsObjects() {
		return getPage().images;
	}

	protected float maxDistanceControlpointScreen() {
		return 25f;
	}
	
	@Override
	protected void saveGraphics(GraphicsControlpoint graphics) {
		view.saveGraphics(graphics);
		GraphicsImage image = (GraphicsImage) graphics;
		view.callOnEditImageListener(image);
	}

	@Override
	protected void editGraphics(GraphicsControlpoint graphics) {
		graphics.restore();
		GraphicsImage image = (GraphicsImage) graphics;
		view.callOnEditImageListener(image);
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
