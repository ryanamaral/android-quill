package name.vbraun.view.write;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.UUID;

import com.write.Quill.artist.Artist;

import junit.framework.Assert;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class GraphicsImage extends GraphicsControlpoint {
	private static final String TAG = "GraphicsImage";

	private Controlpoint bottom_left, bottom_right, top_left, top_right,
			center;
	private final Paint paint = new Paint();
	private final Paint outline = new Paint();
	private final Rect rect = new Rect();
	private final RectF rectF = new RectF();

	private Bitmap bitmap = null;
	private File file = null;

	private int height, width;

	public enum FileType {
		FILETYPE_NONE, FILETYPE_PNG, FILETYPE_JPG
	}
	
	public static String getImageFileExt(FileType fileType) {
		if (fileType == FileType.FILETYPE_JPG) {
			return ".jpg";
		} else if (fileType == FileType.FILETYPE_PNG) {
			return ".png";
		} else {
			Assert.fail();
			return null;
		}
	}
	
	public static FileType getImageFileType(String fileName) {
		for (FileType t : FileType.values()) {
			if (t == FileType.FILETYPE_NONE)
				continue;
			String ext = getImageFileExt(t);
			if (fileName.endsWith(ext))
				return t;
		}
		return FileType.FILETYPE_NONE;
	}

	public static String getImageFileName(UUID uuid, FileType fileType) {
		return uuid.toString() + getImageFileExt(fileType);
	}

	public String getImageFileExt() {
		return getImageFileExt(fileType);
	}
	
	public String getImageFileName() {
		return getImageFileName(getUuid(), fileType);
	}

	// persistent data
	protected URI sourceUri = null;
	protected UUID uuid = null;
	protected boolean constrainAspect = false;
	protected FileType fileType = FileType.FILETYPE_NONE;
	protected Rect cropRect = new Rect();

	/**
	 * @param transform
	 *            The current transformation
	 * @param x
	 *            Screen x coordinate
	 * @param y
	 *            Screen y coordinate
	 * @param penThickness
	 * @param penColor
	 */
	protected GraphicsImage(Transformation transform, float x, float y) {
		super(Tool.IMAGE);
		setTransform(transform);
		bottom_left = new Controlpoint(transform, x, y);
		bottom_right = new Controlpoint(transform, x, y);
		top_left = new Controlpoint(transform, x, y);
		top_right = new Controlpoint(transform, x, y);
		center = new Controlpoint(transform, x, y);
		controlpoints.add(bottom_left);
		controlpoints.add(bottom_right);
		controlpoints.add(top_left);
		controlpoints.add(top_right);
		controlpoints.add(center);
		init();
	}

	private void init() {
		paint.setARGB(0xff, 0x5f, 0xff, 0x5f);
		paint.setStyle(Style.FILL);
		paint.setStrokeWidth(0);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
		outline.setARGB(0xff, 0x0, 0xaa, 0x0);
		outline.setStyle(Style.STROKE);
		outline.setStrokeWidth(4);
		outline.setAntiAlias(true);
		outline.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	protected Controlpoint initialControlpoint() {
		return bottom_right;
	}

	@Override
	public boolean intersects(RectF screenRect) {
		return false;
	}

	@Override
	public void draw(Canvas c, RectF bounding_box) {
		computeScreenRect();
		c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);

		if (bitmap == null) {
			c.drawRect(rect, paint);
			c.drawRect(rect, outline);
		} else {
			c.drawBitmap(bitmap, null, rect, null);
		}
	}

	private Controlpoint oppositeControlpoint(Controlpoint point) {
		if (point == bottom_right)
			return top_left;
		if (point == bottom_left)
			return top_right;
		if (point == top_right)
			return bottom_left;
		if (point == top_left)
			return bottom_right;
		if (point == center)
			return center;
		Assert.fail("Unreachable");
		return null;
	}

	private final static float minDistancePixel = 30;

	@Override
	void controlpointMoved(Controlpoint point) {
		super.controlpointMoved(point);
		if (point == center) {
			float width2 = (bottom_right.x - bottom_left.x) / 2;
			float height2 = (top_right.y - bottom_right.y) / 2;
			bottom_right.y = bottom_left.y = center.y - height2;
			top_right.y = top_left.y = center.y + height2;
			bottom_right.x = top_right.x = center.x + width2;
			bottom_left.x = top_left.x = center.x - width2;
		} else {
			Controlpoint opposite = oppositeControlpoint(point);
			float dx = opposite.x - point.x;
			float dy = opposite.y - point.y;
			float minDistance = minDistancePixel / scale;
			if (0 <= dx && dx <= minDistance)
				dx = minDistance;
			if (-minDistance <= dx && dx <= 0)
				dx = -minDistance;
			if (0 <= dy && dy <= minDistance)
				dy = minDistance;
			if (-minDistance <= dy && dy <= 0)
				dy = -minDistance;
			rectF.bottom = point.y;
			rectF.top = point.y + dy;
			rectF.left = point.x;
			rectF.right = point.x + dx;
			rectF.sort();
			bottom_right.y = bottom_left.y = rectF.bottom;
			top_right.y = top_left.y = rectF.top;
			bottom_right.x = top_right.x = rectF.right;
			bottom_left.x = top_left.x = rectF.left;
			center.x = rectF.left + (rectF.right - rectF.left) / 2;
			center.y = rectF.bottom + (rectF.top - rectF.bottom) / 2;
		}
	}

	private void computeScreenRect() {
		rectF.bottom = bottom_left.screenY();
		rectF.top = top_left.screenY();
		rectF.left = bottom_left.screenX();
		rectF.right = bottom_right.screenX();
		rectF.sort();
		rectF.round(rect);
	}

	public void writeToStream(DataOutputStream out) throws IOException {
	}

	public GraphicsImage(DataInputStream in) throws IOException {
		super(Tool.IMAGE);
		int version = in.readInt();
		if (version > 1)
			throw new IOException("Unknown version!");
		tool = Tool.values()[in.readInt()];
		if (tool != Tool.LINE)
			throw new IOException("Unknown tool type!");

		init();
		loadBitmap();
	}

	@Override
	public void render(Artist artist) {
		// TODO Auto-generated method stub
	}

	public void setBitmap(UUID newUuid, String fileName) {
		// file = new File("/mnt/sdcard/d5efe912-4b03-4ed7-a124-bff4984691d6.jpg");
		file = new File(fileName);
		uuid = newUuid;
		fileType = getImageFileType(fileName);
		try {
			loadBitmap();
		} catch (IOException e) {
			Log.e(TAG, "Unable to load file " + file.toString() + " (missing?");
		}
	}

	public UUID getUuid() {
		if (uuid == null)
			uuid = UUID.randomUUID();
		return uuid;
	}

	public File getFile() {
		return file;
	}

	private final int IMAGE_MAX_SIZE = 1024;

	private void loadBitmap() throws IOException {
		Assert.assertNotNull(file);
		InputStream fis;

		BitmapFactory.Options o1 = new BitmapFactory.Options();
		o1.inJustDecodeBounds = true;
		fis = new FileInputStream(file);
		BitmapFactory.decodeStream(fis, null, o1);
		fis.close();

		int h = o1.outHeight;
		int w = o1.outWidth;
		int scale = 1;
		if (h > IMAGE_MAX_SIZE || w > IMAGE_MAX_SIZE) {
			scale = (int) Math.pow(
					2,
					(int) Math.round(Math.log(IMAGE_MAX_SIZE
							/ (double) Math.max(h, w))
							/ Math.log(0.5)));
		}

		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		fis = new FileInputStream(file);
		bitmap = BitmapFactory.decodeStream(fis, null, o2);
		fis.close();

		height = o2.outHeight;
		width = o2.outWidth;
	}

}
