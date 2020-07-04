package com.packtpub;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

public class LiveCameraActivity extends Activity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    static {
        System.loadLibrary("livecamera");
    }

    private Camera mCamera;
    private TextureView mTextureView;
    private byte[] mVideoSource;
    private ImageView mImageViewR, mImageViewG, mImageViewB;
    private Bitmap mImageR, mImageG, mImageB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livecamera);
        mTextureView = (TextureView) findViewById(R.id.preview);
        mImageViewR = (ImageView) findViewById(R.id.imageViewR);
        mImageViewG = (ImageView) findViewById(R.id.imageViewG);
        mImageViewB = (ImageView) findViewById(R.id.imageViewB);

        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture pSurface, int pWidth, int pHeight)
    {}

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture pSurface){}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture pSurface, int pWidth, int pHeight){
        mCamera = Camera.open();
        try {
            mCamera.setPreviewTexture(pSurface);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setDisplayOrientation(0);
            Camera.Size size = findBestResolution(pWidth, pHeight);
            PixelFormat pixelFormat = new PixelFormat();
            PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pixelFormat);
            int sourceSize = size.width * size.height * pixelFormat.bitsPerPixel / 8;
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            mCamera.setParameters(parameters);
            mVideoSource = new byte[sourceSize];
            mImageR = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageG = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageB = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageViewR.setImageBitmap(mImageR);
            mImageViewG.setImageBitmap(mImageG);
            mImageViewB.setImageBitmap(mImageB);
            mCamera.addCallbackBuffer(mVideoSource);
            mCamera.startPreview();
        }
        catch (IOException ioe) {
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }

    private Camera.Size findBestResolution(int pWidth, int pHeight) {
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size selectedSize = mCamera.new Size(0,0);
        for (Camera.Size size : sizes) {
            if ((size.width <= pWidth)
                && (size.height <= pHeight)
                && (size.width >= selectedSize.width)
                && (size.height >= selectedSize.height)) {
                selectedSize = size;
            }
        }
        if ((selectedSize.width == 0) || (selectedSize.height == 0)) {
            selectedSize = sizes.get(0);
        }
        return selectedSize;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture pSurface){
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mVideoSource = null;
            mImageR.recycle(); mImageR = null;
            mImageG.recycle(); mImageG = null;
            mImageB.recycle(); mImageB = null;
        }
        return true;
    }

    @Override
    public void onPreviewFrame(byte[] pData, Camera pCamera){
        if (mCamera != null) {
            decode(mImageR, pData, 0xFFFF0000);
            decode(mImageG, pData, 0xFF00FF00);
            decode(mImageB, pData, 0xFF0000FF);

            mCamera.addCallbackBuffer(mVideoSource);
        }
    }

    public native void decode(Bitmap pTarget, byte[] pSource, int pFilter);
}
