package com.write.Quill.artist;

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
	
	///////////////////////////////////
	// First the low-level API 
	
	// construct a path
	abstract public void moveTo(float x, float y);
	abstract public void lineTo(float x, float y);
	
	// a path does not generate anything on the page until you call one of these methods
	abstract public void stroke();
	abstract public void fill();
	abstract public void fillStroke();

	// set the line style
	abstract public void setLineWidth(float width);
	abstract public void setLineColor(float colorRed, float colorGreen, float colorBlue);
	abstract public void setLineCap(LineStyle.Cap cap);
	abstract public void setLineJoin(LineStyle.Join join);
	
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
	
	public void drawLine(float x0, float y0, float x1, float y1, LineStyle lineStyle) {
		setLineStyle(lineStyle);
		moveTo(x0,y0);
		lineTo(x1,y1);
		stroke();
	}

	abstract public void addPage(name.vbraun.view.write.Page page);
	
}
