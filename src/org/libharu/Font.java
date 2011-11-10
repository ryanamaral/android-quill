package org.libharu;

public class Font {
	private final static String TAG = "Font";
	
	protected final Document parent;
	private int HPDF_Font_Pointer;

	public Font(Document document, BuiltinFont font, String encodingName) {
		parent = document;
		construct(document, font, encodingName);
	}
		
	public Font(Document document, BuiltinFont font) {
		parent = document;
		construct(document, font, "StandardEncoding");
	}
		
	// the standard PDF fonts (do not need to be embedded)
	public enum BuiltinFont {
		  COURIER, COURIER_BOLD, COURIER_OBLIQUE, COURIER_BOLD_OBLIQUE //,
//		  HELVETICA, HELVETICA_BOLD, HELVETICA_OBLIQUE, HELVETICA_BOLD_OBLIQUE,
//		  TIMES_ROMAN, TIMES_BOLD, TIMES_ITALIC, TIMES_BOLD_ITALIC,
//		  SYMBOL, ZAPFDINGBATS
	}
	
	///////////////////////////////////////////////////////
	/// private stuff 
	///////////////////////////////////////////////////////
	private native void construct(Document document, BuiltinFont font, String encodingName);
	private static native void initIDs();
	static {
        System.loadLibrary("hpdf");
		initIDs();
	}	
}
