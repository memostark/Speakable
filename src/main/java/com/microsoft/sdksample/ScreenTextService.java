/*
        References: http://www.piwai.info/chatheads-basics (MAIN SOURCE)
                     https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
                     Tip for trying to improve Google TTS initialization time:
                     https://stackoverflow.com/questions/42417606/bypassing-google-tts-engine-initialization-lag-in-android/42588475
 */

package com.microsoft.sdksample;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class ScreenTextService extends Service {

    static final int ANIMATION_NONE = 0;
    static final int ANIMATION_OPEN = 1;
    static final int ANIMATION_CLOSE = 2;
    static final int ANIMATION_FORCE_CLOSE = 3;

    private WindowManager windowManager;
    private MediaProjectionManager mMediaProjectionManager;
    private View service_layout;
    private FrameLayout trash_layout;
    private GestureDetector gestureDetector;
    private TextRecognizer textRecognizer;
    private CustomTTS tts;

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private static boolean hasPermission;
    private String TAG = this.getClass().getSimpleName();

    private static int resultCode;
    private static Intent permissionIntent = null;

    private MediaProjection mediaProjection;
    private ImageReader mImageReader;
    private AnimationHandler mAnimationHandler;
    private Handler mHandler;
    private Display mDisplay;
    private static DisplayMetrics mMetrics;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private DrawView snipView;
    private ImageView close;
    private ImageView takeSS;
    private LinearLayout container;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams mParamsTrash;

    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();


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
        trash_layout= (FrameLayout) LayoutInflater.from(this).inflate(R.layout.trash_layout, null);
        trash_layout.setClipChildren(false);
        mAnimationHandler = new AnimationHandler(trash_layout);
        hasPermission=false;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMetrics = getResources().getDisplayMetrics();
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        tts = new CustomTTS(ScreenTextService.this);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        final ImageView bubble = (ImageView) service_layout.findViewById(R.id.image_bubble);
        close = (ImageView) service_layout.findViewById(R.id.closeService_image);
        takeSS = (ImageView) service_layout.findViewById(R.id.takeScreenShot_image);
        snipView = (DrawView) service_layout.findViewById(R.id.snip_view);
        container = (LinearLayout) service_layout.findViewById(R.id.icon_container);
        bubble.setImageResource(R.mipmap.ic_launcher);

        final View trashIconContainer = trash_layout.findViewById(R.id.trash_icon_container);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        mParamsTrash = new WindowManager.LayoutParams();
        mParamsTrash.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.type = WindowManager.LayoutParams.TYPE_PHONE;
        mParamsTrash.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParamsTrash.format = PixelFormat.TRANSLUCENT;
        // INFO:Windowの原点のみ左下に設定
        mParamsTrash.gravity = Gravity.START | Gravity.BOTTOM;

        // Thanks! https://stackoverflow.com/questions/3779173/determining-the-size-of-an-android-view-at-runtime#6569243
        ViewTreeObserver viewTreeObserver = trash_layout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    trash_layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int heightTrashIcon = trashIconContainer.getMeasuredHeight();
                    trash_layout.setTranslationY(heightTrashIcon);
                    mAnimationHandler.mTargetHeight = 48;
                }
            });
        }

        trash_layout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
                                       int oldBottom) {
                mAnimationHandler.onUpdateViewPosition();

            }
        });


        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private float touchX;
            private float touchY;

            //-------https://stackoverflow.com/questions/19538747/how-to-use-both-ontouch-and-onclick-for-an-imagebutton
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {

                    if(isSnipViewVisible()){
                        setSnippingView(false);
                    }else {
                        setSnippingView(true);
                    }
                    return true;
                } else {
                    // your code for move and drag
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mAnimationHandler.updateTargetPosition(params.x, params.y);
                            mAnimationHandler.sendAnimationMessageDelayed(ANIMATION_OPEN, LONG_PRESS_TIMEOUT);
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            touchX = event.getX();
                            touchY = event.getY();
                            //Log.i(TAG,"X: "+touchX+" Y: "+ touchY+" RawX: "+initialTouchX+" RawY: "+initialTouchY);
                            return true;
                        case MotionEvent.ACTION_UP:
                            mAnimationHandler.removeMessages(ANIMATION_OPEN);
                            mAnimationHandler.sendAnimationMessage(ANIMATION_CLOSE);
                            animateToEdge();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            //Log.i(TAG,"X: "+params.x+" Y: "+ params.y);
                            windowManager.updateViewLayout(service_layout, params);
                            return true;
                    }
                }
                return false;
            }
            //-----------https://stackoverflow.com/questions/18503050/how-to-create-draggabble-system-alert-in-android
            private void animateToEdge() {
                int currentX = params.x;
                int bubbleWidth =  bubble.getMeasuredWidth();
                Log.i(TAG,"Width: "+mMetrics.widthPixels);
                ValueAnimator ani;
                if (currentX > (mMetrics.widthPixels - bubbleWidth) / 2) {
                    ani = ValueAnimator.ofInt(currentX, mMetrics.widthPixels - bubbleWidth);
                } else {
                    ani = ValueAnimator.ofInt(currentX, -bubbleWidth/3);

                }
                //params.y = Math.min(Math.max(0, initialY),mMetrics.heightPixels - bubble.getMeasuredHeight());

                ani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        params.x = (Integer) animation.getAnimatedValue();
                        windowManager.updateViewLayout(service_layout, params);
                    }
                });
                ani.setDuration(350L);
                ani.setInterpolator(new AccelerateDecelerateInterpolator());
                ani.start();

            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (service_layout != null)
                    windowManager.removeView(service_layout);
                if(trash_layout!=null)
                    windowManager.removeView(trash_layout);
            }
        });

        takeSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSnippingView(false);
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

    protected static void setScreenshotPermission(final Intent intent, int result) {
        permissionIntent = intent;
        resultCode= result;
        if(permissionIntent!=null) hasPermission=true;
    }

    public CustomTTS getTTS(){
        return tts;
    }

    private void setSnippingView(Boolean visible) {
        FrameLayout.LayoutParams rlparams = (FrameLayout.LayoutParams)container.getLayoutParams();
        if(visible){
            rlparams.setMargins(0,35,0,0);
            snipView.setVisibility(View.VISIBLE);
            takeSS.setVisibility(View.VISIBLE);
            close.setVisibility(View.GONE);
            params.x=0;
            params.y=100;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            windowManager.updateViewLayout(service_layout, params);
        }else{
            rlparams.setMargins(0,0,0,0);
            container.setLayoutParams(rlparams);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x=0;
            params.y=100;
            snipView.setVisibility(View.GONE);
            close.setVisibility(View.VISIBLE);
            takeSS.setVisibility(View.GONE);
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.updateViewLayout(service_layout, params);
        }
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
                    //openScreenshot(imageFile);
                    // https://stackoverflow.com/questions/37287910/how-to-extract-text-from-image-android-app
                    Frame imageFrame = new Frame.Builder()

                            .setBitmap(croppedBitmap)                 // your image bitmap
                            .build();

                    String imageText;


                    SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                    for (int i = 0; i < textBlocks.size(); i++) {
                        TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                        imageText = textBlock.getValue();                   // return string
                        tts.determineLanguage(imageText);
                        Log.i(TAG, i+".- "+imageText);
                    }
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
        if(intent!=null) {
            String action = intent.getAction();
            if(MainActivity.NORMAL_SERVICE.equals(action) ){
                windowManager.addView(trash_layout, mParamsTrash);
                windowManager.addView(service_layout, params);
                if(!hasPermission){
                    permissionIntent = intent;
                    resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                }
                hasPermission=true;
            }else if(MainActivity.NORMAL_NO_PERM_SERVICE.equals(action)) {
                if(!hasPermission){
                    final Intent intentGetPermission = new Intent(this, AcquireScreenshotPermission.class);
                    intentGetPermission.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentGetPermission);
                }
                windowManager.addView(trash_layout, mParamsTrash);
                windowManager.addView(service_layout, params);
            }
        }else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (service_layout != null) {
            if(service_layout.getWindowToken() != null) windowManager.removeView(service_layout);
        }
        if (trash_layout != null) {
            if(trash_layout.getWindowToken() != null) windowManager.removeView(trash_layout);
        }
        if(tts!=null) tts.finishTTS();
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    static class AnimationHandler extends Handler{

        private static final int TYPE_FIRST = 1;
        private static final int TYPE_UPDATE = 2;
        private static final float OVERSHOOT_TENSION = 1.0f;
        private static final long TRASH_OPEN_DURATION_MILLIS = 400L;
        private static final long TRASH_OPEN_START_DELAY_MILLIS = 200L;
        private static final long TRASH_CLOSE_DURATION_MILLIS = 200L;
        private static final long ANIMATION_REFRESH_TIME_MILLIS = 10L;

        private final WeakReference<FrameLayout> mTrashView;

        private int mStartedCode;
        private long mStartTime;
        private float mStartTransitionY;
        private float mTargetPositionX;
        private float mTargetPositionY;
        private float mTargetHeight;
        private float mMoveStickyYRange;


        private final Rect mTrashIconLimitPosition;
        private final OvershootInterpolator mOvershootInterpolator;


        AnimationHandler(FrameLayout trashView){
            mTrashView = new WeakReference<>(trashView);
            mStartedCode = ANIMATION_NONE;
            mTrashIconLimitPosition = new Rect();
            mOvershootInterpolator = new OvershootInterpolator(OVERSHOOT_TENSION);
        }

        @Override
        public void handleMessage(Message msg) {
            final FrameLayout trashView = mTrashView.get();

            if (trashView == null) {
                removeMessages(ANIMATION_OPEN);
                removeMessages(ANIMATION_CLOSE);
                removeMessages(ANIMATION_FORCE_CLOSE);
                return;
            }

            final int animationCode = msg.what;
            final int animationType = msg.arg1;
            final FrameLayout trash_icon_cont = (FrameLayout) trashView.findViewById(R.id.trash_icon_container);

            if (animationType == TYPE_FIRST) {
                mStartTime = SystemClock.uptimeMillis();
                mStartTransitionY = trash_icon_cont.getTranslationY();
                mStartedCode = animationCode;
            }

            final float elapsedTime = SystemClock.uptimeMillis() - mStartTime;

            if(animationCode == ANIMATION_OPEN) {
                if (elapsedTime >= TRASH_OPEN_DURATION_MILLIS) {
                    final float screenHeight = mMetrics.heightPixels;
                    final float targetPositionYRate = Math.min(2 * (mTargetPositionY + mTargetHeight) / (screenHeight + mTargetHeight), 1.0f);
                    final float stickyPositionY = mMoveStickyYRange * targetPositionYRate + mTrashIconLimitPosition.height() - mMoveStickyYRange;
                    final float translationYTimeRate = Math.min((elapsedTime - TRASH_OPEN_START_DELAY_MILLIS) / TRASH_OPEN_DURATION_MILLIS, 1.0f);
                    final float positionY = mTrashIconLimitPosition.bottom - stickyPositionY * mOvershootInterpolator.getInterpolation(translationYTimeRate);
                    Log.d("prueba","Y: "+positionY);
                    trashView.setTranslationY(positionY);

                }

                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
            } else if (animationCode == ANIMATION_CLOSE) {
                final float translationYTimeRate = Math.min(elapsedTime / TRASH_CLOSE_DURATION_MILLIS, 1.0f);
                // アニメーションが最後まで到達していない場合
                if (translationYTimeRate < 1.0f) {
                    final float position = mStartTransitionY + mTrashIconLimitPosition.height() * translationYTimeRate;
                    trashView.setTranslationY(position);
                    sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
                } else {
                    // 位置を強制的に調整
                    trashView.setTranslationY(mTrashIconLimitPosition.bottom);
                    mStartedCode = ANIMATION_NONE;
                }
            }


        }

        void sendAnimationMessageDelayed(int animation, long delayMillis) {
            sendMessageAtTime(newMessage(animation, TYPE_FIRST), SystemClock.uptimeMillis() + delayMillis);
        }

        void sendAnimationMessage(int animation) {
            sendMessage(newMessage(animation, TYPE_FIRST));
        }

        private static Message newMessage(int animation, int type) {
            final Message message = Message.obtain();
            message.what = animation;
            message.arg1 = type;
            return message;
        }

        void updateTargetPosition(float x, float y) {
            mTargetPositionX = x;
            mTargetPositionY = y;
        }

        void onUpdateViewPosition(){
            final FrameLayout trashView = mTrashView.get();
            if (trashView == null) {
                return;
            }
            final float backgroundHeight = trashView.findViewById(R.id.trash_icon_container).getMeasuredHeight();
            mTrashIconLimitPosition.set(-100, -600, 100, 200);
            mMoveStickyYRange = backgroundHeight*0.2f;
        }
    }
}
