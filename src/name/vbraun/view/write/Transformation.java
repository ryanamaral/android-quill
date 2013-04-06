package name.vbraun.view.write;

public class Transformation {

	protected float offset_x = 0f;
	protected float offset_y = 0f;
	protected float scale = 1.0f;

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
		Transformation result = new Transformation();
		result.offset_x = offset_x + dx;
		result.offset_y = offset_y + dy;
		result.scale = scale;
		return result;
	}

	public Transformation copy() {
		Transformation t = new Transformation();
		t.offset_x = offset_x;
		t.offset_y = offset_y;
		t.scale = scale;
		return t;
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
