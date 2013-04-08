package name.vbraun.view.write;

public class Transformation {

	protected float offset_x;
	protected float offset_y;
	protected float scale;

	public Transformation() {
		offset_x = 0.0f;
		offset_y = 0.0f;
		scale = 1.0f;
	}
	
	public Transformation(float offset_x, float offset_y, float scale) {
		this.offset_x = offset_x;
		this.offset_y = offset_y;
		this.scale = scale;
	}
	
	public Transformation(final Transformation t) {
		this.offset_x = t.offset_x;
		this.offset_y = t.offset_y;
		this.scale = t.scale;
	}
	
	public float applyX(float x) {
		return x * scale + offset_x;
	}

	public float scaleText(float fontSize) {
		// Based on ThinkPad Tablet
		return scale / 1232f * fontSize;
	}

	public float applyY(float y) {
		return y * scale + offset_y;
	}

	public float inverseX(float x) {
		return (x - offset_x) / scale;
	}

	public float inverseY(float y) {
		return (y - offset_y) / scale;
	}

	public Transformation offset(float dx, float dy) {
		return new Transformation(offset_x + dx, offset_y + dy, scale);
	}

	protected void set(Transformation t) {
		offset_x = t.offset_x;
		offset_y = t.offset_y;
		scale = t.scale;
	}

	public boolean equals(Transformation t) {
		return (offset_x == t.offset_x) && (offset_y == t.offset_y) && (scale == t.scale);
	}
}
