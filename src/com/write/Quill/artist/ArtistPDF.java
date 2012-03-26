package com.write.Quill.artist;

import java.io.File;
import java.io.IOException;
import java.util.Currency;
import java.util.ListIterator;

import name.vbraun.view.write.Stroke;

import org.libharu.Document;
import org.libharu.Document.CompressionMode;
import org.libharu.Font;
import org.libharu.Font.BuiltinFont;
import org.libharu.Page;
import org.libharu.Page.LineCap;
import org.libharu.Page.LineJoin;
import org.libharu.Page.PageDirection;
import org.libharu.Page.PageSize;

import junit.framework.Assert;

import android.util.Log;

import com.write.Quill.artist.LineStyle.Cap;
import com.write.Quill.artist.LineStyle.Join;

public class ArtistPDF
	extends Artist {
	
	private final static String TAG = "ArtistPDF";
	
	private File file;
	private Document doc;
	private Page pdf;
	
	protected float height;
	protected float width;

	private Font pageNumberFont;
	protected final int pageNumberSize = 14;
	protected int pageNumber = 1;
	private PageSize pageSize;

	private boolean rotate = false;

	
	public ArtistPDF(File file) {
		this.file = file;
		doc = new Document();
		doc.setCompressionMode(CompressionMode.COMP_ALL);
		pageNumberFont = doc.getFont(BuiltinFont.COURIER_BOLD);
	}

	@Override
	public void destroy() {
		try {
			String path = file.getAbsolutePath();
			doc.saveToFile(path);
		} catch (IOException e) {
			Log.e(TAG, "Error saving PDF file: "+e.toString());
		}	
	}
	
	public float scaledX(float x, float y) {
		if (rotate)
			return y * scale;
		else
			return x * scale;
	}
	
	public float scaledY(float x, float y) {
		if (rotate)
			return x * scale;
		else
			return (1-y) * scale;
	}
	
	@Override
	public void moveTo(float x, float y) {
		pdf.moveTo(scaledX(x), scaledY(y));
	}

	@Override
	public void lineTo(float x, float y) {
		pdf.lineTo(x, y);
	}

	@Override
	public void stroke() {
		pdf.stroke();
		currentLineStyle = null;
	}

	@Override
	public void fill() {
		pdf.fill();
		currentLineStyle = null;
	}

	@Override
	public void fillStroke() {
		pdf.fillStroke();
		currentLineStyle = null;
	}

	@Override
	public void setLineWidth(float width) {
        pdf.setLineWidth(scale * width);
	}

	@Override
	public void setLineColor(float red, float green, float blue) {
        pdf.setRGBStroke(red, green, blue);
	}

	@Override
	public void setLineCap(Cap cap) {
		switch (cap) {
		case BUTT_END:
			pdf.setLineCap(LineCap.BUTT_END);
			break;
		case PROJECTING_SQUARE_END:
			pdf.setLineCap(LineCap.PROJECTING_SQUARE_END);
			break;
		case ROUND_END:
			pdf.setLineCap(LineCap.ROUND_END);
			break;
		default:
			Assert.fail();
		}
	}

	@Override
	public void setLineJoin(Join join) {
		switch (join) {
		case BEVEL_JOIN:
			pdf.setLineJoin(LineJoin.BEVEL_JOIN);
			break;
		case MITER_JOIN:
			pdf.setLineJoin(LineJoin.MITER_JOIN);
			break;
		case ROUND_JOIN:
			pdf.setLineJoin(LineJoin.ROUND_JOIN);
			break;
		default:
			Assert.fail();
		}
	}
	
	public void addPageNumber() {
        int margin = 20;
        pdf.setFontAndSize(pageNumberFont, pageNumberSize);
        pdf.beginText();
        pdf.textOut(width-pageNumberSize-margin, height-pageNumberSize-margin,
        			""+pageNumber);
        pdf.endText();
        pageNumber++;
	}
		

	public void draw(name.vbraun.view.write.Page page) {
		boolean page_is_portrait = (page.getAspectRatio() < 1);
		pdf = doc.addPage();
		if (page_is_portrait)	{
			pdf.setSize(pageSize, PageDirection.PORTRAIT);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.getAspectRatio());
		} else {
			pdf.setSize(pageSize, PageDirection.LANDSCAPE);
			width = pdf.getWidth();
			height = pdf.getHeight();
			scale = Math.min(height, width/page.getAspectRatio());
		}
		page.renderArtist(this);
		addPageNumber();
	}

}
