package com.leo.cameramodule;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

public class CameraTextureView extends TextureView {


    public static final String TAG = "CameraSurfaceView";
    Context mContext;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;


    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = getContext();
        mRatioWidth = getWidth();
        mRatioHeight = getHeight();
        setKeepScreenOn(true);
        requestLayout();
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = getContext();
        mRatioWidth = getWidth();
        mRatioHeight = getHeight();
        setKeepScreenOn(true);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public void configureTransform(int viewWidth, int viewHeight, int mPreviewHeight, int mPreviewWidth) {
        if (mContext == null || !(mContext instanceof Activity)) {
            return;
        }
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewWidth, mPreviewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewHeight,
                    (float) viewWidth / mPreviewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        setTransform(matrix);
    }
}
