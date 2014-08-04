package com.opensource.imagecrop.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-08-04.
 */
public class CropImageView  extends ImageViewTouchBase {

//    public ArrayList<HighlightView> mHighlightViews = new ArrayList<HighlightView>();

    private HighlightView mMotionHighlightView = null;
    private float mLastX;
    private float mLastY;
    private int mMotionEdge;
    private boolean mSaving = false;
    private HighlightView mCropView;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDisplayed.getBitmap() != null) {
            mCropView.mMatrix.set(getImageMatrix());
            mCropView.invalidate();
            if (mCropView.mIsFocused) {
                centerBasedOnHighlightView(mCropView);
            }
        }
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void zoomTo(float scale, float centerX, float centerY) {
        super.zoomTo(scale, centerX, centerY);
        mCropView.mMatrix.set(getImageMatrix());
        mCropView.invalidate();
    }

    @Override
    protected void zoomIn() {
        super.zoomIn();
        mCropView.mMatrix.set(getImageMatrix());
        mCropView.invalidate();
    }

    @Override
    protected void zoomOut() {
        super.zoomOut();
        mCropView.mMatrix.set(getImageMatrix());
        mCropView.invalidate();
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY) {
        super.postTranslate(deltaX, deltaY);
        mCropView.mMatrix.postTranslate(deltaX, deltaY);
        mCropView.invalidate();
    }

    // According to the event's position, change the focus to the first
    // hitting cropping rectangle.
    private void recomputeFocus(MotionEvent event) {
        mCropView.setFocus(false);
        mCropView.invalidate();

        int edge = mCropView.getHit(event.getX(), event.getY());
        if (edge != HighlightView.GROW_NONE) {
            if (!mCropView.hasFocus()) {
                mCropView.setFocus(true);
                mCropView.invalidate();
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSaving) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // CR: inline case blocks.
                int edge = mCropView.getHit(event.getX(), event.getY());
                if (edge != HighlightView.GROW_NONE) {
                    mMotionEdge = edge;
                    mMotionHighlightView = mCropView;
                    mLastX = event.getX();
                    mLastY = event.getY();
                    // CR: get rid of the extraneous parens below.
                    mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move
                            : HighlightView.ModifyMode.Grow);
                    break;
                }
                break;
            // CR: vertical space before case blocks.
            case MotionEvent.ACTION_UP:
                centerBasedOnHighlightView(mMotionHighlightView);
                mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
                mMotionHighlightView = null;
                break;
            case MotionEvent.ACTION_MOVE:
                mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
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
                break;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                center(true, true);
                break;
            case MotionEvent.ACTION_MOVE:
                // if we're not zoomed then there's no point in even allowing
                // the user to move the image around. This call to center puts
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

        int panDeltaX1 = Math.max(0, getLeft() - r.left);
        int panDeltaX2 = Math.min(0, getRight() - r.right);

        int panDeltaY1 = Math.max(0, getTop() - r.top);
        int panDeltaY2 = Math.min(0, getBottom() - r.bottom);

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
            float[] coordinates = new float[] { hv.mCropRect.centerX(), hv.mCropRect.centerY() };
            getImageMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F); // CR: 300.0f.
        }

        ensureVisible(hv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCropView.draw(canvas);
    }

    public void setCropView(HighlightView hv) {
        this.mCropView = hv;
        invalidate();
    }

    public HighlightView getCropView() {
        return this.mCropView;
    }

    public Rect getCropRect() {
        return this.mCropView.getCropRect();
    }

    public void setSaving(boolean isSaving) {
        this.mSaving = isSaving;
    }
}
