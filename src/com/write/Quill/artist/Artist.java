package com.write.Quill.artist;

import java.io.File;

import junit.framework.Assert;

/**
 * Artist is the base class for exporters to other file formats. Think PDF export, for example
 * @author vbraun
 */
abstract public class Artist {
	private final static String TAG = "Artist";
	
	protected boolean interrupt = false;	
	protected float scale = 1f;
	protected boolean backgroundVisible = true;
	
	// The current point (undefined until you call one of moveTo/lineTo/quadTo/cubicTo first
	protected float current_x;
	protected float current_y;
	
	///////////////////////////////////
	// First the low-level API 
	
	// construct a path
	abstract public void moveTo(float x, float y);
	abstract public void lineTo(float x, float y);
	
	// quadratic Bezier to (x2,y2) with control point (x1,y1)
	abstract public void quadTo(float x1, float y1, float x2, float y2);
	
	// cubic Bezier to (x3,y3) with control points (x1,y1) and (x2,y2)
	abstract public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3);
	
	// a path does not generate anything on the page until you call one of these methods
	abstract public void stroke();
	abstract public void fill();
	abstract public void fillStroke();

	// set the line style
	abstract public void setLineWidth(float width);
	abstract public void setLineColor(float colorRed, float colorGreen, float colorBlue);
	abstract public void setLineCap(LineStyle.Cap cap);
	abstract public void setLineJoin(LineStyle.Join join);
	
	// set the fill style
	abstract public void setFillColor(float colorRed, float colorGreen, float colorBlue);
	
	// write result and/or close output file
	abstract public void destroy();
	
	// draw larger/smaller
	protected void setScale(float scale) {
		this.scale = scale;
	}
	
	protected float getScale() {
		return scale;
	}
	
	// The artist is supposed to run in a background thread. This lets you interrupt it.
	public boolean getInterrupt() {
		return interrupt;
	}
	
	public void setInterrupt(boolean interruptFlag) {
		this.interrupt = interruptFlag;
	}
	
	// Whether the background is visible
	public void setBackgroundVisible(boolean visible) {
		backgroundVisible = visible;
	}
	
	public boolean getBackgroundVisible() {
		return backgroundVisible;
	}
	
	///////////////////////////////////
	// The rest are helpers, forming the higher-level API 

	protected LineStyle currentLineStyle = null;
	public void setLineStyle(LineStyle lineStyle) {
		Assert.assertNotNull(lineStyle);
		if (currentLineStyle == null) {
			setLineWidth(lineStyle.getWidth());
			setLineColor(lineStyle.getRed(), lineStyle.getGreen(), lineStyle.getBlue());
			setLineCap(lineStyle.getCap());
			setLineJoin(lineStyle.getJoin());
		} else
			lineStyle.commitChanges(currentLineStyle, this);
		currentLineStyle = lineStyle;
	}
	
	protected FillStyle currentFillStyle = null;
	public void setFillStyle(FillStyle fillStyle) {
		Assert.assertNotNull(fillStyle);
		if (currentFillStyle == null) {
			setFillColor(fillStyle.getRed(), fillStyle.getGreen(), fillStyle.getBlue());
		}
	}
	
	public void drawLine(float x0, float y0, float x1, float y1, LineStyle lineStyle) {
		setLineStyle(lineStyle);
		moveTo(x0,y0);
		lineTo(x1,y1);
		stroke();
	}

	abstract public void imageJpeg(File jpgFile, float left, float right, float top, float bottom);
	
	abstract public void addPage(name.vbraun.view.write.Page page);
	
}
