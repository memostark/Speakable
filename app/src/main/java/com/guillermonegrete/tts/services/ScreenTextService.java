/*
        References: http://www.piwai.info/chatheads-basics (MAIN SOURCE)
                     https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
                     Tip for trying to improve Google TTS initialization time:
                     https://stackoverflow.com/questions/42417606/bypassing-google-tts-engine-initialization-lag-in-android/42588475
 */

package com.guillermonegrete.tts.services;

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
import android.graphics.*;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;

import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
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

import com.google.firebase.ml.vision.FirebaseVision;
import com.guillermonegrete.tts.BuildConfig;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDatabase;
import com.guillermonegrete.tts.imageprocessing.FirebaseCloudTextProcessor;
import com.guillermonegrete.tts.imageprocessing.ImageProcessingSource;
import com.guillermonegrete.tts.imageprocessing.domain.interactors.DetectTextFromScreen;
import com.guillermonegrete.tts.main.AcquireScreenshotPermission;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customviews.BubbleView;
import com.guillermonegrete.tts.customviews.SnippingView;
import com.guillermonegrete.tts.customviews.TrashView;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.textprocessing.ProcessTextActivity;
import com.guillermonegrete.tts.imageprocessing.FirebaseTextProcessor;

import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor;
import com.guillermonegrete.tts.main.TranslatorType;
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.threading.MainThreadImpl;
import org.jetbrains.annotations.NotNull;

public class ScreenTextService extends Service {

    private WindowManager windowManager;
    private MediaProjectionManager mMediaProjectionManager;
    private View service_layout;
    private TrashView trash_layout;
    private GestureDetector gestureDetector;
    private CustomTTS tts;

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private static boolean hasPermission;

    private static int resultCode;
    private static Intent permissionIntent = null;

    private static DisplayMetrics mMetrics;
    private SnippingView snipView;
    private BubbleView bubble;
    private ImageButton playButton;
    private ImageButton translateButton;
    private ProgressBar playLoadingIcon;
    private ConstraintLayout icon_container;
    private FrameLayout play_icons_container;
    private WindowManager.LayoutParams windowParams;
    private WindowManager.LayoutParams mParamsTrash;

    private final Rect mFloatingViewRect = new Rect();
    private final Rect mTrashViewRect = new Rect();

    static final int STATE_NORMAL = 0;
    static final int STATE_INTERSECTING = 1;
//    static final int STATE_FINISHING = 2;

    private ClipboardManager clipboard;
    private SharedPreferences sharedPreferences;
    private String languageToPreference;

    private ImageProcessingSource textProcessor;

    /*
    *  Type of service
    * */
    public static final String NORMAL_SERVICE = "startService";
    public static final String NO_FLOATING_ICON_SERVICE = "startNoFloatingIcon";
    public static final String LONGPRESS_SERVICE = "showServiceLongPress";

    private Point screenSize;

    private boolean isPlaying;
    private boolean isAvailable;


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
        trash_layout= new TrashView(this);
        hasPermission=false;
        isPlaying = false;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMetrics = getResources().getDisplayMetrics();
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        tts = CustomTTS.getInstance(getApplicationContext());

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        bubble = service_layout.findViewById(R.id.image_bubble);
        snipView = service_layout.findViewById(R.id.snip_view);
        icon_container = service_layout.findViewById(R.id.icon_container);
        play_icons_container = service_layout.findViewById(R.id.play_icons_container);

        playButton = service_layout.findViewById(R.id.play_icon_button);
        translateButton = service_layout.findViewById(R.id.translate_icon_button);
        playLoadingIcon = service_layout.findViewById(R.id.play_loading_icon);

        screenSize = new Point();
        windowManager.getDefaultDisplay().getSize(screenSize);

        windowParams = new WindowManager.LayoutParams();
        defaultSnippingView();

        mParamsTrash = new WindowManager.LayoutParams();
        defaultTrashViewParams();

        setTrashViewVerticalPosition();

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            int xByTouch;
            int yByTouch;

            //-------https://stackoverflow.com/questions/19538747/how-to-use-both-ontouch-and-onclick-for-an-imagebutton
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {

                    trash_layout.dismiss();
                    if(isSnipViewVisible()) setFloatingIconView();
                    else showSnippingView();

                    return true;
                } else if (!isSnipViewVisible()) {
                    // your code for move and drag
                    final int state = bubble.getState();
                    trash_layout.onTouchFloatingView(event, windowParams.x, windowParams.y);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            bubble.mAnimationHandler.sendAnimationMessage(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            initialX = windowParams.x;
                            initialY = windowParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
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
                                windowParams.x = 0;
                                windowParams.y = 100;
                            }else animateToEdge();

                            bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            xByTouch = initialX + (int) (event.getRawX() - initialTouchX);
                            yByTouch = initialY + (int) (event.getRawY() - initialTouchY);
                            System.out.println("x0: " + initialX + " y0: " + initialY);
                            System.out.println("x: " + xByTouch + " y: " + yByTouch);
                            final boolean isIntersecting = isIntersectingWithTrash();
                            final boolean isIntersect = state == STATE_INTERSECTING;
                            if (isIntersecting) {
                                bubble.setIntersecting( (int) trash_layout.getTrashIconCenterX(), (int) trash_layout.getTrashIconCenterY());
                                windowParams.x= (int) trash_layout.getTrashIconCenterX();
                                windowParams.y = (int) trash_layout.getTrashIconCenterY();
                            }else{
                                windowParams.x = xByTouch;
                                windowParams.y = yByTouch;
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

                            //Log.i(TAG,"X: "+windowParams.x+" Y: "+ windowParams.y);
                            windowManager.updateViewLayout(service_layout, windowParams);
                            return true;
                    }
                }
                return true;
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

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPlaying) {
                    tts.stop();
                    isPlaying = false;
                    playButton.setImageResource(R.drawable.ic_volume_up_black_24dp);
                }else {
                    isAvailable = true;
                    playLoadingIcon.setVisibility(View.VISIBLE);
                    playButton.setVisibility(View.GONE);
                    DetectTextFromScreen interactor = new DetectTextFromScreen(
                            ThreadExecutor.getInstance(),
                            MainThreadImpl.getInstance(),
                            new ScreenImageCaptor(mMediaProjectionManager, mMetrics, screenSize, resultCode, permissionIntent),
                            textProcessor,
                            snipView.getSnipRectangle(),
                            new DetectTextFromScreen.Callback() {
                                @Override
                                public void onTextDetected(@NotNull String text, @NotNull String language) {
                                    Toast.makeText(ScreenTextService.this, "Language detected: " + language, Toast.LENGTH_SHORT).show();
                                    // TODO should probably move this condition to the custom tts class
                                    tts.setListener(ttsListener);
                                    boolean isInitialized = tts.getInitialized() && tts.getLanguage().equals(language);
                                    if (!isInitialized) tts.initializeTTS(language);
                                    if(isAvailable) tts.speak(text);
                                }
                            }
                    );
                    interactor.run();
                }

            }
        });

        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DetectTextFromScreen interactor = new DetectTextFromScreen(
                        ThreadExecutor.getInstance(),
                        MainThreadImpl.getInstance(),
                        new ScreenImageCaptor(mMediaProjectionManager, mMetrics, screenSize, resultCode, permissionIntent),
                        textProcessor,
                        snipView.getSnipRectangle(),
                        new DetectTextFromScreen.Callback() {
                            @Override
                            public void onTextDetected(@NotNull String text, @NotNull String language) {
                                System.out.println("detected text: " + text);
                                detectLanguageAndTranslate(text);
                            }
                        }
                );
                interactor.run();
            }
        });

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTextRecognizer();
        setClipboardCallback();

    }

    private void detectLanguageAndTranslate(String text){
        languageToPreference = getLanguageToPreference();
        GetLangAndTranslation interactor = new GetLangAndTranslation(
                ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                WordRepository.getInstance(getTranslatorSource(), WordLocalDataSource.getInstance(WordsDatabase.getDatabase(getApplicationContext()).wordsDAO())),
                text,
                "auto",
                languageToPreference,
                new GetLangAndTranslation.Callback(){
                    @Override
                    public void onDataNotAvailable() { }

                    @Override
                    public void onTranslationAndLanguage(@NotNull Words word) {
                        showPopUpTranslation(word);
                    }
                });
        interactor.execute();
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
        return snipView.getVisibility() == View.VISIBLE;
    }

    private ClipboardManager.OnPrimaryClipChangedListener clipboardListener = new ClipboardManager.OnPrimaryClipChangedListener(){

        @Override
        public void onPrimaryClipChanged() {
            if (!sharedPreferences.getBoolean(SettingsFragment.PREF_CLIPBOARD_SWITCH, false)) return;

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
        if(clipboard != null) clipboard.addPrimaryClipChangedListener(clipboardListener);
    }

    enum TextRecognizerType{
        FIREBASE_LOCAL, FIREBASE_CLOUD
    }

    private void setTextRecognizer(){
        int recognizerPreference = Integer.parseInt(sharedPreferences.getString("textRecognizerPref", "0"));
        System.out.println("Recognizer preference " + recognizerPreference);

        TextRecognizerType recognizerType = TextRecognizerType.values()[recognizerPreference];

        switch (recognizerType){
            case FIREBASE_CLOUD:
//                Toast.makeText(this, "Cloud recognizer", Toast.LENGTH_SHORT).show();
                textProcessor = FirebaseCloudTextProcessor.Companion.getInstance(FirebaseVision.getInstance().getCloudTextRecognizer());
                break;
            case FIREBASE_LOCAL:
//                Toast.makeText(this, "Local recognizer", Toast.LENGTH_SHORT).show();
                textProcessor = FirebaseTextProcessor.Companion.getInstance();
                break;
            default:
                break;
        }
    }

    private WordDataSource getTranslatorSource(){
        int translatorPreference = Integer.parseInt(sharedPreferences.getString(TranslatorType.PREFERENCE_KEY, ""));
        TranslatorType translatorType = TranslatorType.Companion.valueOf(translatorPreference);

        switch (translatorType){
            case GOOGLE_PUBLIC:
                return GooglePublicSource.Companion.getInstance();
            case MICROSOFT:
                return MSTranslatorSource.getInstance(BuildConfig.TranslatorApiKey);
            default:
                return GooglePublicSource.Companion.getInstance();
        }
    }

    private String getLanguageToPreference(){
        int englishIndex = 15;
        int languagePreferenceIndex = sharedPreferences.getInt(SettingsFragment.PREF_LANGUAGE_TO, englishIndex);
        String[] languagesISO = getResources().getStringArray(R.array.googleTranslateLanguagesValue);
        return languagesISO[languagePreferenceIndex];
    }


    private void defaultSnippingView(){
        windowParams.x = 0;
        windowParams.y = 100;
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        windowParams.format = PixelFormat.TRANSLUCENT;
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

    private void showSnippingView(){
        FrameLayout.LayoutParams frameLayoutParams = (FrameLayout.LayoutParams) icon_container.getLayoutParams();
        frameLayoutParams.setMargins(0,0,0,35);
        snipView.setVisibility(View.VISIBLE);
        showContainerActionButtons();
        setContainerBackground();
        windowParams.x=0;
        windowParams.y=100;
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        windowManager.updateViewLayout(service_layout, windowParams);
    }

    private void setFloatingIconView(){
        snipView.setVisibility(View.GONE);

        hideContainerActionButtons();
        removeContainerBackground();

        defaultSnippingView();
        windowManager.updateViewLayout(service_layout, windowParams);
        icon_container.post(new Runnable() {
            @Override
            public void run() {
                animateToEdge();
            }
        });
    }

    private void setContainerBackground(){
        icon_container.setBackgroundResource(R.drawable.slot_background);
    }

    private void removeContainerBackground(){
        icon_container.setBackgroundResource(0);
    }

    private void showContainerActionButtons(){
        play_icons_container.setVisibility(View.VISIBLE);
        translateButton.setVisibility(View.VISIBLE);
    }

    private void hideContainerActionButtons() {
        play_icons_container.setVisibility(View.GONE);
        translateButton.setVisibility(View.GONE);
    }

    private void showPopUpTranslation(Words word){
        View layout = LayoutInflater.from(this).inflate(R.layout.pop_up_translation, (ViewGroup) service_layout, false);
        TextView translationTextView = layout.findViewById(R.id.text_view_popup_translation);
        translationTextView.setText(word.definition);
        TextView languageFrom = layout.findViewById(R.id.language_from_text);
        languageFrom.setText(word.lang);
        TextView languageTo = layout.findViewById(R.id.language_to_text);
        languageTo.setText(languageToPreference);
        PopupWindow popupWindow = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setElevation(24);
        popupWindow.setAnimationStyle(R.style.PopUpWindowAnimation);
        popupWindow.showAtLocation(icon_container, Gravity.BOTTOM, 0, 24);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null) {
            String action = intent.getAction();
            System.out.println("Service intent action " + action);
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
            }
            createForeground(action);
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
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_IMPORTANCE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Clipboard translation is activated")
                .setContentText("Tap to start")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tap to start text recognition"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_close, getString(R.string.close_notification),stopServiceIntent)
                .setOngoing(true)
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    private void addViews(){
        if(service_layout.getWindowToken() == null) {
            windowManager.addView(service_layout, windowParams);
            // Runs after the view has been drawn
            icon_container.post(new Runnable() {
                @Override
                public void run() {
                    animateToEdge();
                }
            });
        }
        if(trash_layout.getWindowToken() == null) windowManager.addView(trash_layout, mParamsTrash);
    }

    //-----------https://stackoverflow.com/questions/18503050/how-to-create-draggabble-system-alert-in-android
    private void animateToEdge() {
        int currentX = windowParams.x;
        int bubbleWidth =  icon_container.getMeasuredWidth();
        ValueAnimator ani;
        int toPosition;
        if (currentX > (mMetrics.widthPixels - bubbleWidth) / 2) toPosition = mMetrics.widthPixels - 2*bubbleWidth/3;
        else toPosition = -bubbleWidth / 3;

        System.out.println("currentX: " + currentX + " bubble width: " + bubbleWidth + " to: " + toPosition);
        ani = ValueAnimator.ofInt(currentX, toPosition);
        //windowParams.y = Math.min(Math.max(0, initialY),mMetrics.heightPixels - bubble.getMeasuredHeight());

        ani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                windowParams.x = (Integer) animation.getAnimatedValue();
                windowManager.updateViewLayout(service_layout, windowParams);
            }
        });
        ani.setDuration(350L);
        ani.setInterpolator(new AccelerateDecelerateInterpolator());
        ani.start();

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

    private CustomTTS.Listener ttsListener = new CustomTTS.Listener() {
        @Override
        public void onLanguageUnavailable() {
            isPlaying = false;
            isAvailable = false;
            playLoadingIcon.setVisibility(View.GONE);
            playButton.setVisibility(View.VISIBLE);
            Toast.makeText(ScreenTextService.this, "Language not available for TTS", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSpeakStart() {
            // TODO Should create a presenter or view model for this main thread stuff
            MainThreadImpl.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    playLoadingIcon.setVisibility(View.GONE);
                    isPlaying = true;
                    playButton.setImageResource(R.drawable.ic_stop_black_24dp);
                    playButton.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onSpeakDone() {
            // For some reason I need to use main thread here even though I'm not using other threads
            // Should find out why this is necessary
            MainThreadImpl.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    isPlaying = false;
                    playButton.setImageResource(R.drawable.ic_volume_up_black_24dp);
                }
            });
        }
    };


}
