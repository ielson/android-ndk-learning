package com.packtpub.livecamera;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.packtpub.livecamera.R;

import java.io.IOException;
import java.util.List;

public class LiveCameraActivity extends Activity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    static {
        System.loadLibrary("livecamera");
    }

    private static final int PERMISSION_REQUEST_CODE = 200;

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

        if (checkPermission()) {
            mTextureView.setSurfaceTextureListener(this);
        } else {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(LiveCameraActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture pSurface, int pWidth, int pHeight)
    {}

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture pSurface){}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture pSurface, int pWidth, int pHeight){
        try {
            releaseCameraAndPreview();
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
        try {
            mCamera.setPreviewTexture(pSurface);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setDisplayOrientation(0);
            Log.d(getString(R.string.app_name), "Surface width: " + pWidth + " Height: " + pHeight);
            Camera.Size size = findBestResolution(pWidth, pHeight);
            Log.d(getString(R.string.app_name), "Camera width: " + size.width + " camera height: " + size.height);
            PixelFormat pixelFormat = new PixelFormat();
            PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pixelFormat);
            Log.d(getString(R.string.app_name), "bits/pixel: " + pixelFormat.bitsPerPixel);
            int sourceSize = size.width * size.height * pixelFormat.bitsPerPixel / 8;
            Log.d(getString(R.string.app_name), "Source Size: "+sourceSize);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setParameters(parameters);
            mVideoSource = new byte[sourceSize];
            mImageR = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageG = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageB = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageViewR.setImageBitmap(mImageR);
            mImageViewG.setImageBitmap(mImageG);
            mImageViewB.setImageBitmap(mImageB);
            mCamera.addCallbackBuffer(mVideoSource);
            Log.d("Live Camera", "before start preview");
            mCamera.startPreview();
            Log.d("Live Camera", "after start preview");
        }
        catch (IOException ioe) {
            Log.d("Live Camera", "inside io exception");
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }

    private Camera.Size findBestResolution(int pWidth, int pHeight) {
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size selectedSize = mCamera.new Size(0,0);
        for (Camera.Size size : sizes) {
            Log.d(getString(R.string.app_name), "Max size: " + pWidth + "x" + pHeight);
            Log.d(getString(R.string.app_name), "Camera possible size: " + size.width + "x" + size.height);
            if ((size.width <= pWidth)
                    && (size.height <= pHeight)
                    && (size.width >= selectedSize.width)
                    && (size.height >= selectedSize.height)) {
                selectedSize = size;
                Log.d(getString(R.string.app_name), "Selected size: " + selectedSize.width + "x" + selectedSize.height);
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
            Log.d(getString(R.string.app_name), "antes do primeiro decode");
            decode(mImageR, pData, 0xFFFF0000);
            Log.d(getString(R.string.app_name), "depois do primeiro decode");
            decode(mImageG, pData, 0xFF00FF00);
            decode(mImageB, pData, 0xFF0000FF);

            mCamera.addCallbackBuffer(mVideoSource);
        }
    }

    public native void decode(Bitmap pTarget, byte[] pSource, int pFilter);

    private void releaseCameraAndPreview() {
        //myCameraPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}
