package name.vbraun.view.write;

import java.io.DataOutputStream;
import java.io.IOException;

import com.write.Quill.artist.Artist;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Abstract base class for all graphics objects
 * 
 * @author vbraun
 * 
 */
public abstract class Graphics {
	private static final String TAG = "Graphics";

	public enum Tool {
		FOUNTAINPEN, PENCIL, MOVE, ERASER, TEXT, LINE, ARROW, IMAGE
	}

	protected Tool tool;

	/**
	 * Copy constructor All derived classes must implement a copy constructor
	 * 
	 * @param graphics
	 */
	protected Graphics(Graphics graphics) {
		tool = graphics.tool;
		setTransform(graphics.transform);
	}

	protected Graphics(Tool tool) {
		this.tool = tool;
	}

	public Tool getTool() {
		return tool;
	}

	protected Transformation transform = new Transformation();
	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;

	protected RectF bBoxFloat = new RectF();
	protected Rect bBoxInt = new Rect();
	protected boolean recompute_bounding_box = true;

	public RectF getBoundingBox() {
		if (recompute_bounding_box)
			computeBoundingBox();
		return bBoxFloat;
	}

	public Rect getBoundingBoxRoundOut() {
		if (recompute_bounding_box)
			computeBoundingBox();
		return bBoxInt;
	}

	/**
	 * An implementation of computeBoundingBox must set bBoxFloat and bBoxInt
	 */
	abstract protected void computeBoundingBox();

	protected void setTransform(Transformation transform) {
		if (this.transform.equals(transform))
			return;
		this.transform.set(transform);
		offset_x = transform.offset_x;
		offset_y = transform.offset_y;
		scale = transform.scale;
		recompute_bounding_box = true;
	}

	abstract public float distance(float x_screen, float y_screen);

	abstract public boolean intersects(RectF r_screen);

	abstract public void draw(Canvas c, RectF bounding_box);

	abstract public void render(Artist artist);

	abstract public void writeToStream(DataOutputStream out) throws IOException;
}
