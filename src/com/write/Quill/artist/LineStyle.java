package com.write.Quill.artist;

public class LineStyle {
	private final static String TAG = "LineStyle";
		
	public enum Cap {
		BUTT_END, ROUND_END, PROJECTING_SQUARE_END
	}

	public enum Join {
	    MITER_JOIN, ROUND_JOIN, BEVEL_JOIN
	}

	private float width = 0;
	private float colorRed = 0f, colorGreen=0f, colorBlue=0f;
	private Cap cap = Cap.BUTT_END;
	private Join join = Join.MITER_JOIN;
	
	public void setWidth(float width) {
		this.width = width;
	}
	
	public float getWidth() {
		return width;
	}
	
	
	/**
	 * Colors are floats in the interval [0,1]
	 * @param colorRed
	 * @param colorGreen
	 * @param colorBlue
	 */
	public void setColor(float colorRed, float colorGreen, float colorBlue) {
		this.colorRed = colorRed;
		this.colorGreen = colorGreen;
		this.colorBlue = colorBlue;
	}
	
	public float getRed() {
		return colorRed;
	}
	
	public float getGreen() {
		return colorGreen;
	}
	
	public float getBlue() {
		return colorBlue;
	}
	
	public void setCap(Cap cap) {
		this.cap = cap;
	}

	public Cap getCap() {
		return cap;
	}
	
	public void setJoin(Join join) {
		this.join = join;
	}
	
	public Join getJoin() {
		return join;
	}
	
	protected void commitChanges(LineStyle prev, Artist artist) {
		if (width != prev.width) artist.setLineWidth(width);
		if ((colorRed != prev.colorRed) || 
			(colorGreen != prev.colorGreen) || 
			(colorBlue != prev.colorBlue))
			artist.setLineColor(colorRed, colorGreen, colorBlue);
		if (cap != prev.cap)
			artist.setLineCap(cap);
		if (join != prev.join)
			artist.setLineJoin(join);
	}
}
