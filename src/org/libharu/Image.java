package org.libharu;

/**
 * An image is an object in a PDF file. The same image can be drawn on multiple pages. 
 * This object just encapsulates the image data, and you should use Document.getImage() 
 * to construct it. You then have to use Page.image() to draw it on any pages you want. 
 * @author vbraun
 */
public class Image {
	private final static String TAG = "Image";
	
	protected final Document parent;
	private int HPDF_Image_Pointer;

	public Image(Document document, String fileName) {
		parent = document;
		construct(document, fileName);
	}
		
	// private stuff follows
	private static native void initIDs();
	private native void construct(Document document, String fileName);
	static {
        System.loadLibrary("hpdf");
		initIDs();
	}
}
