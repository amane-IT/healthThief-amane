package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyCameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private final String TAG = "MyTag";

    private SurfaceHolder mHolder;

    private int mCameraID;

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;

    private int mDisplayOrientation;
    public static Long time = System.currentTimeMillis();

    boolean previewing;
    byte[] currentData;

    public MyCameraPreview(Context context) {
        super(context);
        }

    public MyCameraPreview(Context context, int cameraId) {
        super(context);
        Log.d(TAG, "MyCameraPreview cameraId : " + cameraId);

        // 0    ->     CAMERA_FACING_BACK
        // 1    ->     CAMERA_FACING_FRONT
        mCameraID = cameraId;

        try {
            // attempt to get a Camera instance
            mCamera = Camera.open(mCameraID);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Camera is not available");
        }


        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // get display orientation
        mDisplayOrientation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");

        // retrieve camera's info.
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);

        mCameraInfo = cameraInfo;

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        previewing = false;
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(previewing){
            mCamera.stopPreview();
            previewing = false;
        }

        if(mCamera != null)
        {
            try{
                mCamera.setPreviewDisplay(holder);
                int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
                mCamera.setDisplayOrientation(orientation);
                mCamera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 안드로이드 디바이스 방향에 맞는 카메라 프리뷰를 화면에 보여주기 위해 계산합니다.
     */
    public int calculatePreviewOrientation(Camera.CameraInfo info, int rotation) {
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    /**
     *  이미지 캡처
     */
    public void takePicture(){

        mCamera.takePicture(myShutterCallback, rawCallback, jpegCallback);

    }

    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {

        }
    };

    public void autoFocus(){
        mCamera.autoFocus(myAutoFocusCallback);
    }


    /**
     * 이미지 저장을 위한 콜백 함수
     */
    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };

    private Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };


    private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //이미지의 너비와 높이 결정
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;
            int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);

            //Log.d("MyTag","이미지 캡처 시 -> orientation : " + orientation);

            //byte array 를 bitmap 으로 변환
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length, options);

            //이미지를 디바이스 방향으로 회전
            Matrix matrix = new Matrix();

            /**
             * 셀카 모드에는 저장 시 좌우 반전을 해줘야 한다.
             */
            if(mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

                matrix.setScale(-1,1);
            }

            matrix.postRotate(orientation);
            bitmap =  Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

            //bitmap 을  byte array 로 변환
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

            currentData = stream.toByteArray();

            new MyCameraPreview.SaveImageTask().execute(currentData);
        }
    };

    /**
     * 이미지 저장을 위한 콜백 클레스
     */
    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            try {

                File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Foodiary");
                Log.d("경로", Environment.getExternalStorageDirectory().getAbsolutePath() + "/Foodiary");
                if (!path.exists()) {
                    path.mkdirs();
                }



                String fileName = Long.toString(time) + ".jpg";
                File outputFile = new File(path, fileName);

                outStream = new FileOutputStream(outputFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to "
                        + outputFile.getAbsolutePath());


                mCamera.startPreview();


                // 갤러리에 반영
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(outputFile));
                getContext().sendBroadcast(mediaScanIntent);


                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                    Log.d(TAG, "Camera preview started.");
                } catch (Exception e) {
                    Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

    }

    public String returnTime(){
        String picName = Long.toString(time);
        return picName;
    }

}