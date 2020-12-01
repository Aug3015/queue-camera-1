package com.leo.cameramodule;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.view.Display;

import java.io.IOException;
import java.security.Policy;
import java.util.Collections;
import java.util.List;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

public enum CameraInstance implements Camera.PreviewCallback {
    INSTANCE;
    private Camera mCamera;
    private byte[] mPreviewBuffer;
    private Camera.PreviewCallback mPreviewCallBack;
    private int mWidth = 0;
    private int mHeight = 0;

    public void startPreview(Activity activity, int index, int width, int height, SurfaceTexture surfaceTexture, int previewFormat) throws IOException {
        mWidth = width;
        mHeight = height;
        stop();
        try {
            mCamera = Camera.open(index);
            mCamera.setPreviewTexture(surfaceTexture);
            settingParameter(activity, index, width, height, previewFormat);
            createCallbackBuffer(width, height, previewFormat);
            mCamera.addCallbackBuffer(mPreviewBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        } catch (Exception exception) {
            throw exception;
        }

    }

    private void createCallbackBuffer(int width, int height, int previewFormat) {
        if (previewFormat == ImageFormat.NV21) {
            mPreviewBuffer = new byte[width * height * 3 / 2];
        } else if (previewFormat == ImageFormat.YV12) {
            double strike = Math.ceil(width / 16f * 16);
            double cStride = Math.ceil(width / 32f * 16);
            double frameSize = strike * height;
            double qFrameSize = cStride * height / 2;
            mPreviewBuffer = new byte[(int) (frameSize + qFrameSize * 2)];
        }
    }

    private void settingParameter(Activity activity, int index, int width, int height, int previewFormat) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(previewFormat);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
        parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
        Camera.Size size = mCamera.new Size(width, height);
        selectCameraPreview(parameters, size);
        parameters.setPreviewSize(mWidth, mHeight);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(index, cameraInfo);
        int mDisplayRotation = getDisplayRotation(activity);
        int orientation = getDisplayOrientation(mDisplayRotation, 0);
        mCamera.setDisplayOrientation(orientation);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(parameters);
    }

    private int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case ROTATION_0:
                return 0;
            case ROTATION_90:
                return 90;
            case ROTATION_180:
                return 180;
            case ROTATION_270:
                return 270;

        }
        return 0;
    }

    private int getDisplayOrientation(int degrees, int cameraID) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) * 360;
            result = (360 - result) % 360;

        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private void selectCameraPreview(Camera.Parameters parameters, Camera.Size targetSize) {
        List<Camera.Size> previewsSizes = parameters.getSupportedPreviewSizes();
        Collections.sort(previewsSizes, (lhs, rhs) -> {
            if ((lhs.width * lhs.height) >= (rhs.width * rhs.height)) {
                return 1;
            } else {
                return -1;
            }
        });
        for (Camera.Size size : previewsSizes) {
            if (size.width >= targetSize.width && size.height >= targetSize.height) {
                mWidth = (int) size.width;
                mHeight = (int) size.height;
                return;
            }
        }
    }

    public void resume() {
        if (mCamera != null)
            mCamera.startPreview();
    }

    public void stop() {
        if (mCamera == null)
            return;
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera = null;

    }

    public void pause() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public boolean toggleFlash() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            String flashMode;
            if (params.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH) {
                flashMode = Camera.Parameters.FLASH_MODE_OFF;
            } else {
                flashMode = Camera.Parameters.FLASH_MODE_TORCH;
            }
            params.setFlashMode(flashMode);
            mCamera.setParameters(params);
            return params.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH;
        }
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mPreviewCallBack != null) {
            mPreviewCallBack.onPreviewFrame(data, camera);
        }
        mCamera.addCallbackBuffer(mPreviewBuffer);
    }
}
