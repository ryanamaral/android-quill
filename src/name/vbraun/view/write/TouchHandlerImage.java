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

	final LinearLayout layout;
	final ImageButton edit;
	
	protected TouchHandlerImage(HandwriterView view) {
		super(view, view.getOnlyPenInput());
		
		layout = new LinearLayout(view.getContext());
		view.addView(layout);
		layout.setOrientation(LinearLayout.VERTICAL);
	    layout.setGravity(Gravity.CENTER);

		layout.setLayoutParams(new LinearLayout.LayoutParams( 
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		TextView txt = new TextView(view.getContext());
		txt.setText("test");
		layout.addView(txt);

		edit = new ImageButton(view.getContext());
		edit.setImageResource(android.R.drawable.ic_menu_manage);
		
//		LayoutParams params = 
//				new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		edit.setLayoutParams(params);
		
//		edit.setX(100);
//		edit.setY(100);
		edit.setId(100);
		layout.setX(200);
		layout.setY(200);
		layout.layout(200,	200, 400, 400);
		layout.addView(edit,0);
		
		
		Log.e(TAG, "layout " + layout.getX() + ":" + layout.getY() + " " + layout.getHeight() + "x" + layout.getWidth());
		
		view.invalidate();
//		edit.setX(100);
//		edit.setY(100);
//		edit.setMaxWidth(50);
//		edit.setMaxHeight(50);
//		edit.setMinimumHeight(50);
//		edit.setMinimumWidth(50);
		
	}

	/**
	 * Called when the user touches with the pen
	 */
	@Override
	protected void onPenDown(Controlpoint controlpoint, boolean isNew) {
		view.getToolBox().startControlpointMove(!isNew, true);		
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
		view.removeView(edit);
	}

}
