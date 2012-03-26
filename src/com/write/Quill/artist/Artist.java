package com.write.Quill.artist;

/**
 * Artist is the base class for exporters to other file formats. Think PDF export, for example
 * @author vbraun
 */
abstract public class Artist {
	private final static String TAG = "Artist";
	
	protected float scale;
	
	protected void setScale(float scale) {
		this.scale = scale;
	}
	
	protected float getScale() {
		return scale;
	}
	
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
	
	// The artist is supposed to run in a background thread. This lets you interrupt it.
	protected boolean interrupt = false;
	
	public boolean getInterrupt() {
		return interrupt;
	}
	
	public void setInterrupt(boolean interruptFlag) {
		this.interrupt = interruptFlag;
	}
	
	///////////////////////////////////
	// The rest are helpers, forming the higher-level API 

	protected LineStyle currentLineStyle = null;
	public void setLineStyle(LineStyle lineStyle) {
		lineStyle.commitChanges(currentLineStyle, this);
		currentLineStyle = lineStyle;
	}

	abstract public void draw(name.vbraun.view.write.Page page);
	
}
