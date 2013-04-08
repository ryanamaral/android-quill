package name.vbraun.view.write;

import com.write.Quill.R;

import junit.framework.Assert;
import android.content.Context;
import android.content.res.Resources;

public class AspectRatio {
	public static final String TAG = "AspectRatio";
	private static final float INCH_in_MM = 25.4f;

	protected float ratio;
	protected String resourceName;
	protected float heightMm;
	
	private static final float heightA4 = 297;
	private static final float widthA4 = 210;
	
	private final static String ASPECT_PORTRAIT  = "ASPECT_PORTRAIT";
	private final static String ASPECT_LANDSCAPE = "ASPECT_LANDSCAPE";
	private final static String ASPECT_A4_PAPER  = "ASPECT_A4_PAPER";
	private final static String ASPECT_US_LETTER = "ASPECT_US_LETTER";
	private final static String ASPECT_US_LEGAL  = "ASPECT_US_LEGAL";
	private final static String ASPECT_4_TO_3    = "ASPECT_4_TO_3";
	private final static String ASPECT_16_TO_9   = "ASPECT_16_TO_9";
	
	public static final AspectRatio[] Table = {
		new AspectRatio(ASPECT_PORTRAIT,      800f/1232f, heightA4),
		new AspectRatio(ASPECT_LANDSCAPE,     1280f/752f, widthA4),
		new AspectRatio(ASPECT_A4_PAPER,  1f/(float)Math.sqrt(2), heightA4),
		new AspectRatio(ASPECT_US_LETTER,         8f/11f, 11*INCH_in_MM),
		new AspectRatio(ASPECT_US_LEGAL,          8f/14f, 14*INCH_in_MM),
		new AspectRatio(ASPECT_4_TO_3,             4f/3f, widthA4),
		new AspectRatio(ASPECT_16_TO_9,           16f/9f, widthA4)
	};
	
	
	protected AspectRatio(String resourceName, float aspectRatio, float height_in_mm) {
		this.resourceName = resourceName;
		ratio = aspectRatio;
		heightMm = height_in_mm;
	}
	
	private float comparisonValue(float aspectRatio) {
		return Math.abs(1 - getValue()/aspectRatio);
	}

	static protected AspectRatio closestMatch(float aspectRatio) {
		AspectRatio best = AspectRatio.Table[0];
		for (int i=1; i<AspectRatio.Table.length; i++) {
			AspectRatio aspect = AspectRatio.Table[i];
			if (aspect.comparisonValue(aspectRatio) < best.comparisonValue(aspectRatio))
				best = aspect;
		}
		return best;
	}
		
	public CharSequence getName(Context context) {
		Resources res = context.getResources();
		String[] values  = res.getStringArray(R.array.dlg_aspect_values);
		String[] entries = res.getStringArray(R.array.dlg_aspect_entries);
		Assert.assertTrue(values.length == entries.length);
		for (int i=0; i<entries.length; i++) 
			if (resourceName.equals(values[i]))
				return entries[i];
		return null;
	}

	public float getValue() {
		return ratio;
	}
	
	public float guessHeightMm() {
		return heightMm;
	}
	
	public float guessWidthMm() {
		return ratio * guessHeightMm();
	}

	/**
	 * Return whether the width is larger than the height
	 */
	public boolean isLandscape() {
		return ratio > 1.0f;
	}
	
	/**
	 * Return whether the width is smaller than the height
	 */
	public boolean isPortrait() {
		return ratio < 1.0f;
	}

}
