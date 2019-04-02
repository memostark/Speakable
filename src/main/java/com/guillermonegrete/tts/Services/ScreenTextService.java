/*
        References: http://www.piwai.info/chatheads-basics (MAIN SOURCE)
                     https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
                     Tip for trying to improve Google TTS initialization time:
                     https://stackoverflow.com/questions/42417606/bypassing-google-tts-engine-initialization-lag-in-android/42588475
 */

package com.guillermonegrete.tts.Services;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.guillermonegrete.tts.Main.AcquireScreenshotPermission;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.CustomViews.BubbleView;
import com.guillermonegrete.tts.CustomViews.DrawView;
import com.guillermonegrete.tts.CustomViews.TrashView;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.Main.SettingsFragment;
import com.guillermonegrete.tts.TextProcessing.ProcessTextActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenTextService extends Service {

    private WindowManager windowManager;
    private MediaProjectionManager mMediaProjectionManager;
    private View service_layout;
    private TrashView trash_layout;
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
    private Handler mHandler;
    private Display mDisplay;
    private static DisplayMetrics mMetrics;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private DrawView snipView;
    private BubbleView bubble;
    private ImageView takeSS;
    private LinearLayout icon_container;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams mParamsTrash;

    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    private final Rect mFloatingViewRect = new Rect();
    private final Rect mTrashViewRect = new Rect();

    static final int STATE_NORMAL = 0;
    static final int STATE_INTERSECTING = 1;
    static final int STATE_FINISHING = 2;

    static final String HE_LANG_CODE = "iw";

    private boolean isForeground;

    private ClipboardManager clipboard;

    /*
    *  Type of service
    * */
    public static final String NORMAL_SERVICE = "startService";
    public static final String NO_FLOATING_ICON_SERVICE = "startNoFloatingIcon";
    public static final String LONGPRESS_SERVICE = "showServiceg";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        isForeground=false;

        service_layout= LayoutInflater.from(this).inflate(R.layout.service_processtext, null);
        trash_layout= new TrashView(this);
        hasPermission=false;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMetrics = getResources().getDisplayMetrics();
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        // textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
//        tts = new CustomTTS(ScreenTextService.this);
        tts = CustomTTS.getInstance(getApplicationContext());

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        bubble = service_layout.findViewById(R.id.image_bubble);
        takeSS =  service_layout.findViewById(R.id.takeScreenShot_image);
        snipView = service_layout.findViewById(R.id.snip_view);
        icon_container = service_layout.findViewById(R.id.icon_container);

        params = new WindowManager.LayoutParams();
        defaultSnippingView();

        mParamsTrash = new WindowManager.LayoutParams();
        defaultTrashViewParams();

        setTrashViewVerticalPosition();

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private float touchX;
            private float touchY;
            int xByTouch;
            int yByTouch;

            //-------https://stackoverflow.com/questions/19538747/how-to-use-both-ontouch-and-onclick-for-an-imagebutton
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {

                    trash_layout.dismiss();
                    if(isSnipViewVisible()){
                        setSnippingView(false);
                    }else {
                        setSnippingView(true);
                    }
                    return true;
                } else if (!isSnipViewVisible()) {
                    // your code for move and drag
                    final int state = bubble.getState();
                    trash_layout.onTouchFloatingView(event, params.x, params.y);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            bubble.mAnimationHandler.sendAnimationMessage(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            touchX = event.getX();
                            touchY = event.getY();
                            //Log.i(TAG,"X: "+touchX+" Y: "+ touchY+" RawX: "+initialTouchX+" RawY: "+initialTouchY);
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (state == STATE_INTERSECTING) {
                                bubble.setFinishing();
                                trash_layout.setScaleTrashIcon(false);
                                if (service_layout != null)
                                    windowManager.removeView(service_layout);
                                if(trash_layout!=null)
                                    windowManager.removeView(trash_layout);
                                params.x = 0;
                                params.y = 100;
                            }else animateToEdge();

                            bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            xByTouch = initialX + (int) (event.getRawX() - initialTouchX);
                            yByTouch = initialY + (int) (event.getRawY() - initialTouchY);
                            final boolean isIntersecting = isIntersectingWithTrash();
                            final boolean isIntersect = state == STATE_INTERSECTING;
                            if (isIntersecting) {
                                bubble.setIntersecting( (int) trash_layout.getTrashIconCenterX(), (int) trash_layout.getTrashIconCenterY());
                                params.x= (int) trash_layout.getTrashIconCenterX();
                                params.y = (int) trash_layout.getTrashIconCenterY();
                            }else{
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            }
                            if(isIntersecting && !isIntersect){
                                bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                trash_layout.setScaleTrashIcon(true);
                            }else if(!isIntersecting && isIntersect){
                                bubble.mAnimationHandler.setState(STATE_NORMAL);
                                trash_layout.setScaleTrashIcon(false);
                            }
                            /*Rect rect = new Rect();
                            getBubbleWindowDrawingRect(rect);
                            trash_layout.setRect(rect);
                            trash_layout.invalidate();*/

                            //Log.i(TAG,"X: "+params.x+" Y: "+ params.y);
                            windowManager.updateViewLayout(service_layout, params);
                            return true;
                    }
                }
                return true;
            }
            //-----------https://stackoverflow.com/questions/18503050/how-to-create-draggabble-system-alert-in-android
            private void animateToEdge() {
                int currentX = params.x;
                int bubbleWidth =  bubble.getMeasuredWidth();
                Log.i(TAG,"Width: "+mMetrics.widthPixels);
                ValueAnimator ani;
                if (currentX > (mMetrics.widthPixels - bubbleWidth) / 2) {
                    ani = ValueAnimator.ofInt(currentX, mMetrics.widthPixels - 2*bubbleWidth/3);
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

            private boolean isIntersectingWithTrash() {

                trash_layout.getWindowDrawingRect(mTrashViewRect);
                getBubbleWindowDrawingRect(mFloatingViewRect);

                return Rect.intersects(mTrashViewRect, mFloatingViewRect);
            }

            private void getBubbleWindowDrawingRect(Rect outRect) {
                outRect.set(xByTouch, yByTouch, xByTouch + bubble.getWidth(), yByTouch + bubble.getHeight());

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
                                Log.e(TAG, "Failed to create file storage directory.");
                                return;
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to create file storage directory, getExternalFilesDir is null.");
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

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        setClipboardCallback();


    }

    private void setTrashViewVerticalPosition(){
        // Thanks! https://stackoverflow.com/questions/3779173/determining-the-size-of-an-android-view-at-runtime#6569243
        ViewTreeObserver viewTreeObserver = trash_layout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    trash_layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    final View trashIconContainer =  trash_layout.findViewById(R.id.trash_icon_container);
                    int heightTrashIcon = trashIconContainer.getMeasuredHeight();
                    trashIconContainer.setTranslationY(heightTrashIcon);
                }
            });
        }
    }

    private boolean isSnipViewVisible(){
        return service_layout.findViewById(R.id.snip_view).getVisibility()==View.VISIBLE;
    }

    private ClipboardManager.OnPrimaryClipChangedListener clipboardListener = new ClipboardManager.OnPrimaryClipChangedListener(){

        @Override
        public void onPrimaryClipChanged() {
            SharedPreferences sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(ScreenTextService.this);
            if (!sharedPref.getBoolean(SettingsFragment.PREF_CLIPBOARD_SWITCH, false)) return;

            ClipData clip = clipboard.getPrimaryClip();

            if(clip == null) return;
            if(clip.getItemCount() <= 0) return;

            final CharSequence pasteData = clip.getItemAt(0).getText();

            if (pasteData == null) return;

            Intent dialogIntent = new Intent(ScreenTextService.this, ProcessTextActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            dialogIntent.putExtra("android.intent.extra.PROCESS_TEXT", pasteData);
            startActivity(dialogIntent);
        }
    };

    private void setClipboardCallback(){
        if(clipboard != null) {
            clipboard.addPrimaryClipChangedListener(clipboardListener);
        }
    }

    public static void setScreenshotPermission(final Intent intent, int result) {
        permissionIntent = intent;
        resultCode= result;
        if(permissionIntent!=null) hasPermission=true;
    }


    private void defaultSnippingView(){
        params.x = 0;
        params.y = 100;
        params.gravity = Gravity.TOP | Gravity.START;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.type = WindowManager.LayoutParams.TYPE_PHONE;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.format = PixelFormat.TRANSLUCENT;
    }

    private void defaultTrashViewParams(){
        mParamsTrash.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        mParamsTrash.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParamsTrash.format = PixelFormat.TRANSLUCENT;

        mParamsTrash.gravity = Gravity.START | Gravity.BOTTOM;
        mParamsTrash.x = 0;
        mParamsTrash.y = 0;
    }

    private void setSnippingView(Boolean visible) {
        FrameLayout.LayoutParams rlparams = (FrameLayout.LayoutParams)icon_container.getLayoutParams();
        if(visible){
            rlparams.setMargins(0,35,0,0);
            snipView.setVisibility(View.VISIBLE);
            takeSS.setVisibility(View.VISIBLE);
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
            icon_container.setLayoutParams(rlparams);
            snipView.setVisibility(View.GONE);
            takeSS.setVisibility(View.GONE);

            defaultSnippingView();
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
                    Image.Plane[] planes = image.getPlanes();

                    // create bitmap
                    bitmap = createBitmapFromImagePlane(planes[0]);
                    int statusBarHeight = 0;
                    croppedBitmap = Bitmap.createBitmap(bitmap,snipView.getPosx(),snipView.getPosy() +statusBarHeight,snipView.getRectWidth(),snipView.getRectHeight()+statusBarHeight);

                    String mPath =  STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".png";
                    File imageFile = new File(mPath);

                    // write bitmap to a file
                    fos = new FileOutputStream(imageFile);
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    IMAGES_PRODUCED++;
                    Log.e(TAG, "Captured image: " + IMAGES_PRODUCED);
                    stopProjection();
                    // doFirebaseOCR(croppedBitmap);
                    //openScreenshot(imageFile); // Visualize captured image
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

                if (bitmap != null) bitmap.recycle();
                if (croppedBitmap != null) croppedBitmap.recycle();
                if (image != null) image.close();
                reader.close();
            }
        }

        private Bitmap createBitmapFromImagePlane(Image.Plane plane){
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * mWidth;

            // create bitmap
            Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            return bitmap;
        }

        /*private void doFirebaseOCR(Bitmap bitmap){
            //---------Reference https://firebase.google.com/docs/ml-kit/android/recognize-text?authuser=0
            FirebaseVisionImage imageFB = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionCloudTextDetector detector = FirebaseVision.getInstance().getVisionCloudTextDetector();
            Task<FirebaseVisionCloudText> result = detector.detectInImage(imageFB)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionCloudText>() {
                        @Override
                        public void onSuccess(FirebaseVisionCloudText firebaseVisionCloudText) {
                            String recognizedText = firebaseVisionCloudText.getText();
                            Log.i(TAG, "Firebase text: "+recognizedText);

                            Log.i(TAG, "pages size: " + firebaseVisionCloudText.getPages().size());
                            for (FirebaseVisionCloudText.Page page: firebaseVisionCloudText.getPages()) {
                                List<FirebaseVisionCloudText.DetectedLanguage> languages =
                                        page.getTextProperty().getDetectedLanguages();
                                int index=1;
                                Log.i(TAG, "blocks size: " + page.getBlocks().size());
                                for (FirebaseVisionCloudText.Block block: page.getBlocks()) {
                                    Log.i(TAG, "paragraphs size: " + block.getParagraphs().size());
                                    for (FirebaseVisionCloudText.Paragraph paragraph: block.getParagraphs()){
                                        Log.i(TAG, "word size: " + paragraph.getWords().size());
                                        StringBuilder sentenceString = new StringBuilder();
                                        String langCode = "";
                                        for (FirebaseVisionCloudText.DetectedLanguage lang : paragraph.getTextProperty().getDetectedLanguages()){
                                            langCode=lang.getLanguageCode();
                                            Log.i(TAG, "LANG: " + langCode);
                                        }
                                        int size = paragraph.getWords().size();
                                        if (HE_LANG_CODE.equals(langCode)){
                                            for(int i=size-1;i>=0;i--){
                                                FirebaseVisionCloudText.Word word = paragraph.getWords().get(i);
                                                StringBuilder wordString = new StringBuilder();
                                                for (FirebaseVisionCloudText.Symbol symbol: word.getSymbols()) {
                                                    wordString.append(symbol.getText());
                                                }
                                                Log.i(TAG, index + ".- " + wordString.toString());
                                                index++;
                                                String wordWhitespace= " " + wordString.toString();
                                                sentenceString.append(wordWhitespace);
                                            }
                                            Log.i(TAG, index + "sentence:  " + sentenceString.toString());
                                            tts.initializeTTS(langCode);
                                            tts.speak(sentenceString.toString());
                                        }else{
                                            tts.initializeTTS(langCode);
                                            tts.speak(recognizedText);
                                        }

                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG,"Failed to detect text: "+ e.getMessage());
                        }
                    });

        }*/

        private void doGoogleVisionOCR(Bitmap bitmap){
            // https://stackoverflow.com/questions/37287910/how-to-extract-text-from-image-android-app
            Frame imageFrame = new Frame.Builder()

                    .setBitmap(bitmap)                 // your image bitmap
                    .build();

            String imageText;
            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                imageText = textBlock.getValue();                   // return string
                Log.i(TAG, i+".- "+imageText);
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
            Log.d("prueba","Intent action " + action);
            if(NORMAL_SERVICE.equals(action) ) {
                addViews();
                if (!hasPermission) {
                    permissionIntent = intent;
                    resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                }
                hasPermission = true;
            }else if(NO_FLOATING_ICON_SERVICE.equals(action)){
                action=LONGPRESS_SERVICE;
            }else if(LONGPRESS_SERVICE.equals(action)) {
                if(!hasPermission){
                    final Intent intentGetPermission = new Intent(this, AcquireScreenshotPermission.class);
                    intentGetPermission.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentGetPermission);
                }
                addViews();
            }

            if(!isForeground) createForeground(action);
        }else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createForeground(String action){
        Intent intentHide = new Intent(this, Receiver.class);
        PendingIntent stopServiceIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intentHide, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent = new Intent(this, ScreenTextService.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        String CHANNEL_IMPORTANCE = "service_notification";
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Text to speech")
                .setContentText("Tap to start")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tap to start text recognition"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_close, getString(R.string.close_notification),stopServiceIntent)
                .setOngoing(true)
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
        isForeground=true;
    }

    private void addViews(){
        if(service_layout.getWindowToken() == null) windowManager.addView(service_layout, params);
        if(trash_layout.getWindowToken() == null) windowManager.addView(trash_layout, mParamsTrash);
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
        if(clipboard != null) {
            clipboard.removePrimaryClipChangedListener(clipboardListener);
        }
        if(tts!=null) tts.finishTTS();
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }


}
