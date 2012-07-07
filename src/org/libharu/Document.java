package org.libharu;

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

public class Document {
	private static final String TAG = "HPDF_Document";
	
	private int HPDF_Doc_Pointer;
	protected LinkedList<Page> pages = new LinkedList<Page>();
	
	public Document() {
		construct();
	}
	
	public native void setCompressionMode(CompressionMode mode);
	public enum CompressionMode {
		COMP_NONE, COMP_TEXT, COMP_IMAGE, COMP_METADATA, COMP_ALL
	}
	 
	public native void setPassword(String ownerPassword, String userPassword);

	public Page addPage() {
		return new Page(this);
	}
	
	// Save the PDF to file
	public native void saveToFile(String filename) throws IOException;
	
	// It is your responsibility to call Document.destructAll() when you are finished.
	public void destructAll() {
		ListIterator<Page> iter = pages.listIterator();
		while (iter.hasNext())
			iter.next().destruct();
		destruct();
	}
	
	/**
	 * Construct a new Font object. Use then Page.setFontAndSize() to change the font.
	 * @param font
	 * @param encodingName
	 * @return
	 */
	public Font getFont(Font.BuiltinFont font, String encodingName) {
		return new Font(this, font, encodingName);
	}

	public Font getFont(Font.BuiltinFont font) {
		return new Font(this, font);
	}

	
	/**
	 * Construct a new Image object. Use then Page.image() to draw it on the desired location.
	 * @param fileName
	 * @return
	 */
	public Image getImage(String fileName) {
		return new Image(this, fileName);
	}
	
	// private stuff follows
	private static native void initIDs();
	private native void construct();
	private native void destruct();	
	static {
        System.loadLibrary("hpdf");
		initIDs();
	}
}
