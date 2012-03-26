package name.vbraun.view.write;

public class AspectRatio {
	public static final String TAG = "AspectRatio";
	private static final float INCH_in_MM = 25.4f;

	protected float ratio;
	protected String name = null;
	protected float heightMm;
	
//	public static class AspectRatioName {
//		AspectRatioName(CharSequence n, float a) { name = n; ratio = a; }
//		private CharSequence name;
//		private float ratio;
//	}
	
	private static final float heightA4 = 297;
	private static final float widthA4 = 210;
	
	public static final AspectRatio[] Table = {
		new AspectRatio("Portrait Screen",      800f/1232f, heightA4),
		new AspectRatio("Landscape Screen",     1280f/752f, widthA4),
		new AspectRatio("A4 PaperType", 1f/(float)Math.sqrt(2), heightA4),
		new AspectRatio("US Letter",                8f/11f, 11*INCH_in_MM),
		new AspectRatio("US Legal",                 8f/14f, 14*INCH_in_MM),
		new AspectRatio("Projector (4:3)",           4f/3f, widthA4),
		new AspectRatio("HDTV (16:9)",              16f/9f, widthA4)
	};
	
	
	protected AspectRatio(String aspectName, float aspectRatio, float height_in_mm) {
		ratio = aspectRatio;
		name = aspectName;
		heightMm = height_in_mm;
	}
	
	
	public AspectRatio(float aspectRatio) {
		ratio = aspectRatio;
		for (int i=0; i<Table.length; i++)
			if (ratio == Table[i].ratio) {
				name = Table[i].name;
				heightMm = Table[i].heightMm;
			}
		if (name == null) {
			name = "1:"+ratio;
			if (ratio<1)
				heightMm = heightA4;
			else
				heightMm = widthA4;
		}
	}
	
	public String getName() {
		return name;
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

}
