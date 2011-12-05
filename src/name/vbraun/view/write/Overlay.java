package name.vbraun.view.write;

import android.graphics.Canvas;

/**
 * An overlay is just something that is drawn on top of the HandwriterView 
 * without any scaling. 
 * 
 * In Quill, it is used to draw the tags on the bottom right corner.
 * 
 * @author vbraun
 *
 */
public interface Overlay {
	/**
	 * The overlay will have opportunity to draw on top of a bitmap-backed canvas.
	 * 
	 * @param canvas
	 */
	public void draw(Canvas canvas);
}
