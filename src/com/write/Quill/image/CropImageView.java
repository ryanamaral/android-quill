package com.write.Quill.image;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


class CropImageView extends ImageViewTouchBase {
	float mLastX, mLastY;
	int mMotionEdge;
	boolean stop = false;
	private Context mContext;
	private HighlightView mCrop = null;
	private HighlightView mMotionHighlightView = null;

	@Override
	protected void onLayout(boolean changed, int left, int top,
			int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mBitmapDisplayed.getBitmap() != null) {
			if (mCrop != null) {
				mCrop.mMatrix.set(getImageMatrix());
				mCrop.invalidate();
				if (mCrop.mIsFocused) {
					centerBasedOnHighlightView(mCrop);
				}
			}
		}
	}

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}
	
	protected Rect getCropRect() {
		if (mCrop == null)
			return null;
		else
			return mCrop.getCropRect();
	}
	
	@Override
	protected void zoomTo(float scale, float centerX, float centerY) {
		super.zoomTo(scale, centerX, centerY);
		if (mCrop != null) {
			mCrop.mMatrix.set(getImageMatrix());
			mCrop.invalidate();
		}
	}

	@Override
	protected void zoomIn() {
		super.zoomIn();
		if (mCrop != null) {
			mCrop.mMatrix.set(getImageMatrix());
			mCrop.invalidate();
		}
	}

	@Override
	protected void zoomOut() {
		super.zoomOut();
		if (mCrop != null) {
			mCrop.mMatrix.set(getImageMatrix());
			mCrop.invalidate();
		}
	}

	@Override
	protected void postTranslate(float deltaX, float deltaY) {
		super.postTranslate(deltaX, deltaY);
		if (mCrop != null) {
			mCrop.mMatrix.postTranslate(deltaX, deltaY);
			mCrop.invalidate();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (stop || mCrop == null)
			return false;
		
		ImageActivity imageActivity = (ImageActivity) mContext;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			int edge = mCrop.getHit(event.getX(), event.getY());
			if (edge != HighlightView.GROW_NONE) {
				mMotionEdge = edge;
				mMotionHighlightView = mCrop;
				mLastX = event.getX();
				mLastY = event.getY();
				mMotionHighlightView.setMode(
								(edge == HighlightView.MOVE)
								? HighlightView.ModifyMode.Move
										: HighlightView.ModifyMode.Grow);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mMotionHighlightView != null) {
				centerBasedOnHighlightView(mMotionHighlightView);
				mMotionHighlightView.setMode(
						HighlightView.ModifyMode.None);
			}
			mMotionHighlightView = null;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mMotionHighlightView != null) {
				mMotionHighlightView.handleMotion(mMotionEdge,
						event.getX() - mLastX,
						event.getY() - mLastY);
				mLastX = event.getX();
				mLastY = event.getY();

				if (true) {
					// This section of code is optional. It has some user
					// benefit in that moving the crop rectangle against
					// the edge of the screen causes scrolling but it means
					// that the crop rectangle is no longer fixed under
					// the user's finger.
					ensureVisible(mMotionHighlightView);
				}
			}
			break;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			center(true, true);
			break;
		case MotionEvent.ACTION_MOVE:
			// if we're not zoomed then there's no point in even allowing
			// the user to move the image around.  This call to center puts
			// it back to the normalized location (with false meaning don't
			// animate).
			if (getScale() == 1F) {
				center(true, true);
			}
			break;
		}

		return true;
	}

	// Pan the displayed image to make sure the cropping rectangle is visible.
	private void ensureVisible(HighlightView hv) {
		Rect r = hv.mDrawRect;

		int panDeltaX1 = Math.max(0, mLeft - r.left);
		int panDeltaX2 = Math.min(0, mRight - r.right);

		int panDeltaY1 = Math.max(0, mTop - r.top);
		int panDeltaY2 = Math.min(0, mBottom - r.bottom);

		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if (panDeltaX != 0 || panDeltaY != 0) {
			panBy(panDeltaX, panDeltaY);
		}
	}

	// If the cropping rectangle's size changed significantly, change the
	// view's center and scale according to the cropping rectangle.
	private void centerBasedOnHighlightView(HighlightView hv) {
		Rect drawRect = hv.mDrawRect;

		float width = drawRect.width();
		float height = drawRect.height();

		float thisWidth = getWidth();
		float thisHeight = getHeight();

		float z1 = thisWidth / width * .6F;
		float z2 = thisHeight / height * .6F;

		float zoom = Math.min(z1, z2);
		zoom = zoom * this.getScale();
		zoom = Math.max(1F, zoom);
		if ((Math.abs(zoom - getScale()) / zoom) > .1) {
			float [] coordinates = new float[] {hv.mCropRect.centerX(),
					hv.mCropRect.centerY()};
			getImageMatrix().mapPoints(coordinates);
			zoomTo(zoom, coordinates[0], coordinates[1], 300F);
		}

		ensureVisible(hv);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mCrop != null)
			mCrop.draw(canvas);
	}

	public void setHighlight(HighlightView hv) {
		mCrop = hv;
		if (hv != null) {
			centerBasedOnHighlightView(hv);
			invalidate();
		} else {
			zoomTo(1f);
			invalidate();
		}
	}

}