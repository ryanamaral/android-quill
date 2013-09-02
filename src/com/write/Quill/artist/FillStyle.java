package com.write.Quill.artist;

public class FillStyle {
	public final static String TAG = "FillStyle";
	
	private float colorRed = 0f, colorGreen=0f, colorBlue=0f;
	
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
	
	protected void commitChanges(FillStyle prev, Artist artist) {
		if ((colorRed != prev.colorRed) || 
			(colorGreen != prev.colorGreen) || 
			(colorBlue != prev.colorBlue))
			artist.setLineColor(colorRed, colorGreen, colorBlue);
	}
	
}
