/*
        References: http://www.piwai.info/chatheads-basics (MAIN SOURCE)
                     https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
 */

package com.microsoft.sdksample;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenTextService extends Service {

    private WindowManager windowManager;
    private MediaProjectionManager mMediaProjectionManager;
    private View service_layout;
    private GestureDetector gestureDetector;

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private String TAG = this.getClass().getSimpleName();

    private int resultCode;
    private Intent permissionIntent;

    private MediaProjection mediaProjection;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private DrawView snipView;

    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        service_layout= LayoutInflater.from(this).inflate(R.layout.service_processtext, null);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        final ImageView bubble = (ImageView) service_layout.findViewById(R.id.image_bubble);
        final ImageView close = (ImageView) service_layout.findViewById(R.id.closeService_image);
        final ImageView takeSS = (ImageView) service_layout.findViewById(R.id.takeScreenShot_image);
        snipView = (DrawView) service_layout.findViewById(R.id.snip_view);
        bubble.setImageResource(R.mipmap.ic_launcher);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            //-------https://stackoverflow.com/questions/19538747/how-to-use-both-ontouch-and-onclick-for-an-imagebutton
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    // single tap
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(ScreenTextService.this.getApplicationContext(),"My Awesome service toast...",Toast.LENGTH_SHORT).show();
                        }
                    });
                    if(isSnipViewVisible()){
                        snipView.setVisibility(View.GONE);
                        close.setVisibility(View.VISIBLE);
                        takeSS.setVisibility(View.GONE);
                        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        windowManager.updateViewLayout(service_layout, params);
                    }else {
                        snipView.setVisibility(View.VISIBLE);
                        takeSS.setVisibility(View.VISIBLE);
                        close.setVisibility(View.GONE);
                        params.x = 0;
                        params.y =100;
                        params.width = WindowManager.LayoutParams.MATCH_PARENT;
                        params.height = WindowManager.LayoutParams.MATCH_PARENT;
                        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                        windowManager.updateViewLayout(service_layout, params);
                    }
                    return true;
                } else {
                    // your code for move and drag
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(service_layout, params);
                            return true;
                    }
                }

                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });

        takeSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, permissionIntent);

                if(mediaProjection!=null) {
                    File externalFilesDir = getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        STORE_DIRECTORY = externalFilesDir.getAbsolutePath() + "/screenshotService/";
                        File storeDirectory = new File(STORE_DIRECTORY);
                        if (!storeDirectory.exists()) {
                            boolean success = storeDirectory.mkdirs();
                            if (!success) {
                                Log.e(TAG, "failed to create file storage directory.");
                                return;
                            }
                        }
                    } else {
                        Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
                        return;
                    }

                    DisplayMetrics metrics = getResources().getDisplayMetrics();
                    mDensity = metrics.densityDpi;
                    mDisplay = windowManager.getDefaultDisplay();
                    createVirtualDisplay();

                    mediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
                }

            }
        });

        windowManager.addView(service_layout, params);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();

    }

    private boolean isSnipViewVisible(){
        return service_layout.findViewById(R.id.snip_view).getVisibility()==View.VISIBLE;
    }

    //----Based on https://github.com/mtsahakis/MediaProjectionDemo/blob/master/src/com/mtsahakis/mediaprojectiondemo/ScreenCaptureImageActivity.java

    private void createVirtualDisplay(){
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mediaProjection.createVirtualDisplay("SCREENCAP", mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;
            Bitmap croppedBitmap = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    String mPath =  STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".png";
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;
                    int statusBarHeight = 0;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    croppedBitmap = Bitmap.createBitmap(bitmap,snipView.getPosx(),snipView.getPosy() +statusBarHeight,snipView.getRectWidth(),snipView.getRectHeight()+statusBarHeight);

                    File imageFile = new File(mPath);

                    // write bitmap to a file
                    fos = new FileOutputStream(imageFile);
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                    stopProjection();
                    openScreenshot(imageFile);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if(croppedBitmap != null){
                    croppedBitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
                reader.close();
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    mediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaProjection != null) {
                    mediaProjection.stop();
                }
            }
        });
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        permissionIntent=intent;
        resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (service_layout != null) windowManager.removeView(service_layout);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }
}
