package com.write.Quill;

public class Transformation {

	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;
	
	public float applyX(float x) {
		return x*scale + offset_x;
	}
	
	public float applyY(float y) {
		return y*scale + offset_y;
	}
	
	public float inverseX(float x) {
		return (x-offset_x)/scale;
	}

	public float inverseY(float y) {
		return (y-offset_y)/scale;
	}

	public Transformation offset(float dx, float dy) {
		Transformation result = new Transformation();
		result.offset_x = offset_x + dx;
		result.offset_y = offset_y + dy;
		result.scale = scale;
		return result;
	}	
}
