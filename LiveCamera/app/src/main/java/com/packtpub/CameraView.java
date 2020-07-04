package com.packtpub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback {
    static {
        System.loadLibrary("livecamera");
    }

    public native void decode(Bitmap pTarget, byte[] pSource);

    private Camera mCamera;
    private byte[] mVideoSource;
    private Bitmap mBackBuffer;
    private Paint mPaint;

    public CameraView(Context context) {
        super(context);

        getHolder().addCallback(this);
        setWillNotDraw(false);
    }

    public void surfaceCreated(SurfaceHolder holder)  {
        try {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(this);
        }
        catch (IOException eIOException) {
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }

}
