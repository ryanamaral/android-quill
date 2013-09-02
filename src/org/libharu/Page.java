package org.libharu;

import java.io.File;
import java.util.LinkedList;

public class Page {
	private static final String TAG = "HPDF_Page";
	
	protected final Document parent;
	private int HPDF_Page_Pointer;
	
	public Page(Document document) {
		parent = document;
		document.pages.add(this);
		construct(document);
	}
		
	///////////////////////////////////////////////////////
	/// Page dimensions
	///////////////////////////////////////////////////////
	public native void setSize(PageSize size, PageDirection direction);
	public enum PageSize {
		 LETTER, LEGAL, A3, A4, A5, B4, B5, EXECUTIVE, US4x6, US4x8, US5x7, COMM10
	}
	public enum PageDirection {
		PORTRAIT, LANDSCAPE
	}

	public native float getHeight();
	public native float getWidth();
	
	///////////////////////////////////////////////////////
	/// Paths 
	///////////////////////////////////////////////////////
	public native void setLineWidth(float line_width);
	
	public native void setLineCap(LineCap line_cap);
	public enum LineCap {
		BUTT_END, ROUND_END, PROJECTING_SQUARE_END
	}

	public native void setLineJoin(LineJoin  line_join);
	public enum LineJoin {
	    MITER_JOIN, ROUND_JOIN, BEVEL_JOIN
	}
	
	public native void setMiterLimit(float miter_limit);
	public native void setRGBStroke(float red, float green, float blue);
	public native void setRGBFill(float red, float green, float blue);
	public native void moveTo(float x, float y);
	public native void lineTo(float x, float y);
	public native void curveTo(float x1, float y1, float x2, float y2, float x3, float y3);

	// a path does not generate anything on the page until you call one of these methods
	public native void stroke();
	public native void fill();
	public native void fillStroke();
	
	///////////////////////////////////////////////////////
	/// Images
	///////////////////////////////////////////////////////

	public native void image(Image img, float x, float y, float width, float height);
	
	///////////////////////////////////////////////////////
	/// Text
	///////////////////////////////////////////////////////

	// Text output needs to be wrapped into begin/end blocks
	public native void beginText();
	public native void endText();
	
	public native float getTextWidth(String text);
	public native void setFontAndSize(Font font, float size);
	public native void textOut(float x, float y, String text);
	public native void moveTextPos(float x, float y);
		
	///////////////////////////////////////////////////////
	/// private stuff 
	///////////////////////////////////////////////////////
	private static native void initIDs();
	private native void construct(Document document);
	protected native void destruct();	
	static {
        System.loadLibrary("hpdf");
		initIDs();
	}

}
