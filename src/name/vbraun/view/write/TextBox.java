package name.vbraun.view.write;

import java.io.DataOutputStream;
import java.io.IOException;

import com.write.Quill.artist.Artist;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextPaint;

public class TextBox extends Graphics {
	private final static String TAG = "TextBox";
	
	private TextPaint paint = new TextPaint();
	private DynamicLayout textLayout;
	
	protected void setEditable(Editable edit) {
		textLayout = new DynamicLayout(edit, paint, 100, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
	}
	
	protected TextBox(Tool mTool) {
		super(mTool);
	}

	@Override
	protected void computeBoundingBox() {
		// TODO Auto-generated method stub

	}

	@Override
	public float distance(float x_screen, float y_screen) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean intersects(RectF r_screen) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void draw(Canvas c, RectF bounding_box) {
		if (textLayout == null) return;
		c.save();
		c.translate(10, 60);
		textLayout.draw(c);
		c.restore();
	}

	@Override
	public void writeToStream(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(Artist artist) {
		// TODO Auto-generated method stub
		
	}

	
	
}
