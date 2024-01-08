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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.*;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

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

import com.google.android.material.snackbar.Snackbar;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.LoadResult;
import com.guillermonegrete.tts.data.PlayAudioState;
import com.guillermonegrete.tts.data.Translation;
import com.guillermonegrete.tts.databinding.PopUpTranslationBinding;
import com.guillermonegrete.tts.databinding.ServiceProcesstextBinding;
import com.guillermonegrete.tts.imageprocessing.*;
import com.guillermonegrete.tts.imageprocessing.domain.interactors.DetectTextFromScreen;
import com.guillermonegrete.tts.main.AcquireScreenshotPermission;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customviews.BubbleView;
import com.guillermonegrete.tts.customviews.TrashView;
import com.guillermonegrete.tts.R;

import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.utils.ThemeHelperKt;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScreenTextService extends Service {

    private WindowManager windowManager;
    private MediaProjectionManager mMediaProjectionManager;
    private ServiceProcesstextBinding binding;
    private TrashView trash_layout;
    private GestureDetector gestureDetector;
    @Inject CustomTTS tts;

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private static boolean hasPermission;

    private static int resultCode;
    private static Intent permissionIntent = null;

    private static DisplayMetrics mMetrics;
    private WindowManager.LayoutParams windowParams;
    private WindowManager.LayoutParams mParamsTrash;

    private final Rect mFloatingViewRect = new Rect();
    private final Rect mTrashViewRect = new Rect();

    static final int STATE_NORMAL = 0;
    static final int STATE_INTERSECTING = 1;
//    static final int STATE_FINISHING = 2;

    private SharedPreferences sharedPreferences;
    private String languageToPreference;

    @Inject MainThread mainThread;

    @Inject DetectTextFromScreen detectTextInteractor;
    @Inject GetLangAndTranslation getTranslationInteractor;

    /**
     * This type of service display the floating icon button and listens for clipboard events.
     */
    public static final String NORMAL_SERVICE = "startService";
    /**
     * This type of service listens to clipboard events, no layout added.
     */
    public static final String NO_FLOATING_ICON_SERVICE = "startNoFloatingIcon";

    private Point screenSize;

    private ScreenTextViewModel viewModel;

    // Observers
    private Observer<String> langDetectedObserver;
    private Observer<Integer> langToPreferenceObserver;
    private Observer<PlayAudioState> playAudioObserver;
    private Observer<LoadResult<Translation>> textTranslatedObserver;

    private String[] languagesNames;
    private List<String> languagesISO;

    /**
     * These contexts are used for inflating views with either night or light mode.
     */
    private Context nightContext;
    private Context lightContext;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppTheme);
        languagesNames = getResources().getStringArray(R.array.googleTranslateLanguagesArray);
        languagesISO = Arrays.asList(getResources().getStringArray(R.array.googleTranslateLanguagesValue));
        hasPermission = false;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMetrics = getResources().getDisplayMetrics();
        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        screenSize = new Point();
        windowManager.getDefaultDisplay().getRealSize(screenSize);

        windowParams = new WindowManager.LayoutParams();
        mParamsTrash = new WindowManager.LayoutParams();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        nightContext = createNightModeContext(this, true);
        lightContext = createNightModeContext(this, false);
    }

    private void destroyLayouts() {
        if (binding != null)
            windowManager.removeView(binding.getRoot());
        if(trash_layout != null)
            windowManager.removeView(trash_layout);
        // release all the reference to views to avoid leaks
        binding = null;
        trash_layout = null;
        viewModel.getPlayingAudio().removeObserver(playAudioObserver);
        playAudioObserver = null;
    }

    private void setViewModel() {
        // Because service has no lifecycle we have to observeForever, don't forget to unbind onDestroy.
        var playButton = binding.playIconButton;
        var playLoadingIcon = binding.playLoadingIcon;

        playAudioObserver = state -> {
            if (state instanceof PlayAudioState.Playing) {
                playLoadingIcon.setVisibility(View.INVISIBLE);
                playButton.setImageResource(R.drawable.ic_stop_black_24dp);
                playButton.setVisibility(View.VISIBLE);

            } else if (state instanceof PlayAudioState.Stopped) {
                defaultPlayButton();
            } else if (state instanceof PlayAudioState.Loading) {
                playLoadingIcon.setVisibility(View.VISIBLE);
                playButton.setVisibility(View.INVISIBLE);

            } else if (state instanceof PlayAudioState.Error error) {
                String message = error.getException().getMessage();
                if (message == null) message = "Unknown error";
                Snackbar.make(binding.iconContainer, message, Snackbar.LENGTH_SHORT).show();
                defaultPlayButton();
            }
        };
        viewModel.getPlayingAudio().observeForever(playAudioObserver);

        langDetectedObserver = lang -> {
            binding.languageText.setText(getLanguageName(lang));
            binding.languageText.setVisibility(View.VISIBLE);
        };
        viewModel.getLangDetected().observeForever(langDetectedObserver);

        langToPreferenceObserver = langIndex -> languageToPreference = languagesNames[langIndex];
        viewModel.getLangToPreference().observeForever(langToPreferenceObserver);

        textTranslatedObserver = result -> {
            binding.loadingTranslate.setVisibility(View.GONE);
            binding.translateIconButton.setVisibility(View.VISIBLE);

            if (result instanceof LoadResult.Success<Translation> wordResult) {
                showPopUpTranslation(wordResult.getData());
            } else if (result instanceof LoadResult.Error<Translation> errorResult) {
                handleTranslationError(errorResult.getException());
            } else if (result instanceof LoadResult.Loading) {
                binding.translateIconButton.setVisibility(View.INVISIBLE);
                binding.loadingTranslate.setVisibility(View.VISIBLE);
            }
        };
        viewModel.getTextTranslated().observeForever(textTranslatedObserver);
    }

    private void defaultPlayButton() {
        binding.playLoadingIcon.setVisibility(View.INVISIBLE);
        binding.playIconButton.setVisibility(View.VISIBLE);
        binding.playIconButton.setImageResource(R.drawable.ic_volume_up_black_24dp);
        binding.languageText.setVisibility(View.INVISIBLE);
    }

    private void handleTranslationError(Exception error) {
        String errorText = error.getMessage();
        if (errorText == null) errorText = "Unknown error";
        Snackbar.make(binding.iconContainer, errorText, Snackbar.LENGTH_SHORT).show();
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
        return binding.snipView.getVisibility() == View.VISIBLE;
    }


    private void defaultSnippingView(){
        windowParams.x = 0;
        windowParams.y = 100;
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.type = getLayoutParamType();
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        windowParams.format = PixelFormat.TRANSLUCENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void defaultTrashViewParams(){
        mParamsTrash.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mParamsTrash.type = getLayoutParamType();
        mParamsTrash.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParamsTrash.format = PixelFormat.TRANSLUCENT;
        // Android 12 requires this alpha value to allow touch events to pass to other apps or the system UI.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            mParamsTrash.alpha = 0.8f;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mParamsTrash.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        mParamsTrash.gravity = Gravity.START | Gravity.BOTTOM;
        mParamsTrash.x = 0;
        mParamsTrash.y = 0;
    }

    private int getLayoutParamType(){
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return LAYOUT_FLAG;
    }

    private void showSnippingView(){
        var frameLayoutParams = (FrameLayout.LayoutParams) binding.iconContainer.getLayoutParams();
        frameLayoutParams.setMargins(0,0,0,35);
        showContainerActionButtons();
        setContainerBackground();
        windowParams.x=0;
        windowParams.y=100;
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        windowManager.updateViewLayout(binding.getRoot(), windowParams);
        binding.snipView.prepareLayout();
        binding.snipView.setVisibility(View.VISIBLE);
    }

    private void setFloatingIconView(){
        binding.snipView.setVisibility(View.GONE);

        hideContainerActionButtons();
        removeContainerBackground();

        defaultSnippingView();
        windowManager.updateViewLayout(binding.getRoot(), windowParams);
        binding.iconContainer.post(this::animateToEdge);
    }

    private void setContainerBackground(){
        binding.iconContainer.setBackgroundResource(R.drawable.slot_background);
    }

    private void removeContainerBackground(){
        binding.iconContainer.setBackgroundResource(0);
    }

    private void showContainerActionButtons(){
        binding.playIconsContainer.setVisibility(View.VISIBLE);
        binding.translateIconButton.setVisibility(View.VISIBLE);
    }

    private void hideContainerActionButtons() {
        binding.playIconsContainer.setVisibility(View.GONE);
        binding.translateIconButton.setVisibility(View.GONE);
    }

    /**
     * Creates a context wrapper used for inflating views with the dark/light theme.
     * By default, views inflated in a Service use the system's theme instead of the one set for the app.
     * If you want to use a specific dark/light theme you need to use this wrapped context.
     * <p>
     * Taken from <a href="https://gist.github.com/chrisbanes/bcf4b11154cb59e3f302f278902eb3f7">here</a>.
     */
    public Context createNightModeContext(Context context, boolean isNightMode) {
        var uiModeFlag = isNightMode ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        var config = new Configuration(context.getResources().getConfiguration());
        config.uiMode = uiModeFlag | (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        return new ContextThemeWrapper(context.createConfigurationContext(config), R.style.AppTheme);
    }

    private void showPopUpTranslation(Translation translation){
        boolean isNightMode = ThemeHelperKt.isNightMode(this);
        var context = isNightMode ? nightContext: lightContext;
        var popupBinding = PopUpTranslationBinding.inflate(LayoutInflater.from(context));
        popupBinding.textViewPopupTranslation.setText(translation.getTranslatedText());
        popupBinding.textViewPopupTranslation.setMovementMethod(new ScrollingMovementMethod());
        popupBinding.languageFromText.setText(getLanguageName(translation.getSrc()));
        popupBinding.languageToText.setText(languageToPreference);

        var popupWindow = new PopupWindow(popupBinding.getRoot(), ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setFocusable(true);
        popupWindow.setElevation(24);
        popupWindow.setAnimationStyle(R.style.PopUpWindowAnimation);
        popupWindow.showAtLocation(binding.iconContainer, Gravity.BOTTOM, 0, 24);
    }

    /**
     * Return the language full name based on the ISO characters.
     * @return Language full name if exists otherwise "unknown" resource
     */
    private String getLanguageName(String iso){
        var index = languagesISO.indexOf(iso);
        return index != -1 ? languagesNames[index] : getString(R.string.unknown_language);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null) {
            String action = intent.getAction();
            int pendingFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            var notificationIntent = new Intent(this, ScreenTextService.class);
            notificationIntent.setAction(action);
            var pendingIntent = PendingIntent.getService(this, 0, notificationIntent, pendingFlags);

            if(NORMAL_SERVICE.equals(action) ) {
                addViews();
                if (!hasPermission) {
                    permissionIntent = intent;
                    resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                }
                hasPermission = true;

                viewModel = new ScreenTextViewModel(
                        getResources().getStringArray(R.array.googleTranslateLanguagesValue),
                        mainThread,
                        tts,
                        sharedPreferences,
                        detectTextInteractor,
                        getTranslationInteractor
                );

                setViewModel();
            } else if(NO_FLOATING_ICON_SERVICE.equals(action)) {
                // For this case, when the notification is clicked it should show the bubble layout.
                // Before that the screenshot permission is necessary and then it recreates the service as "NORMAL_SERVICE"
                if(!hasPermission){
                    notificationIntent = new Intent(this, AcquireScreenshotPermission.class);
                    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingFlags);
                }
            }
            createForeground(pendingIntent);
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createForeground(PendingIntent intent) {
        var intentHide = new Intent(this, Receiver.class);
        int stopFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        var stopServiceIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intentHide, stopFlags);

        String CHANNEL_IMPORTANCE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CHANNEL_IMPORTANCE = createNotificationChannel();
        } else {
            CHANNEL_IMPORTANCE = "service_notification";
        }

        var notification = new NotificationCompat.Builder(this, CHANNEL_IMPORTANCE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Clipboard translation is activated")
                .setContentText("Tap to start")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tap to start text recognition"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_close, getString(R.string.close_notification), stopServiceIntent)
                .setOngoing(true)
                .setContentIntent(intent).build();

        startForeground(1337, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        NotificationChannel chan = new NotificationChannel("my_service",
                "My Background Service", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return "my_service";
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addViews(){
        if(binding == null) {
            binding = ServiceProcesstextBinding.inflate(LayoutInflater.from(this));
            var bubble = binding.imageBubble;
            var icon_container = binding.iconContainer;
            defaultSnippingView();

            trash_layout = new TrashView(this);
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
                            case MotionEvent.ACTION_DOWN -> {
                                bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                                bubble.mAnimationHandler.sendAnimationMessage(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                                initialX = windowParams.x;
                                initialY = windowParams.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                //Log.i(TAG,"X: "+touchX+" Y: "+ touchY+" RawX: "+initialTouchX+" RawY: "+initialTouchY);
                                return true;
                            }
                            case MotionEvent.ACTION_UP -> {
                                if (state == STATE_INTERSECTING) {
                                    bubble.setFinishing();
                                    trash_layout.setScaleTrashIcon(false);
                                    destroyLayouts();
                                    windowParams.x = 0;
                                    windowParams.y = 100;
                                } else animateToEdge();
                                bubble.mAnimationHandler.removeMessages(BubbleView.FloatingAnimationHandler.ANIMATION_IN_TOUCH);
                                return true;
                            }
                            case MotionEvent.ACTION_MOVE -> {
                                xByTouch = initialX + (int) (event.getRawX() - initialTouchX);
                                yByTouch = initialY + (int) (event.getRawY() - initialTouchY);
                                final boolean isIntersecting = isIntersectingWithTrash();
                                final boolean isIntersect = state == STATE_INTERSECTING;
                                if (isIntersecting) {
                                    bubble.setIntersecting((int) trash_layout.getTrashIconCenterX(), (int) trash_layout.getTrashIconCenterY());
                                    int containerWidth = icon_container.getWidth() / 2;
                                    int containerHeight = icon_container.getHeight() / 2;

                                    windowParams.x = (int) trash_layout.getTrashIconCenterX() - containerWidth;
                                    windowParams.y = (int) trash_layout.getTrashIconCenterY() - containerHeight;
                                } else {
                                    windowParams.x = xByTouch;
                                    windowParams.y = yByTouch;
                                }
                                if (isIntersecting && !isIntersect) {
                                    bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    trash_layout.setScaleTrashIcon(true);
                                } else if (!isIntersecting && isIntersect) {
                                    bubble.mAnimationHandler.setState(STATE_NORMAL);
                                    trash_layout.setScaleTrashIcon(false);
                                }
                                windowManager.updateViewLayout(binding.getRoot(), windowParams);
                                return true;
                            }
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

            var snipView = binding.snipView;
            binding.playIconButton.setOnClickListener(view ->
                    viewModel.onPlayClick(
                            new ScreenImageCaptor(mMediaProjectionManager, mMetrics, screenSize, resultCode, permissionIntent),
                            snipView.getSnipRectangle()
                    )
            );

            binding.translateIconButton.setOnClickListener(v ->
                    viewModel.onTranslateClick(
                            new ScreenImageCaptor(mMediaProjectionManager, mMetrics, screenSize, resultCode, permissionIntent),
                            snipView.getSnipRectangle()
                    )
            );

            windowManager.addView(binding.getRoot(), windowParams);
            // Runs after the view has been drawn
            binding.iconContainer.post(this::animateToEdge);
            windowManager.addView(trash_layout, mParamsTrash);
        }
    }

    //-----------https://stackoverflow.com/questions/18503050/how-to-create-draggabble-system-alert-in-android
    private void animateToEdge() {
        int currentX = windowParams.x;
        int bubbleWidth =  binding.iconContainer.getMeasuredWidth();
        ValueAnimator ani;
        int toPosition;
        if (currentX > (mMetrics.widthPixels - bubbleWidth) / 2) toPosition = mMetrics.widthPixels - 2 * bubbleWidth / 3;
        else toPosition = -bubbleWidth / 3;

        System.out.println("currentX: " + currentX + " bubble width: " + bubbleWidth + " to: " + toPosition);
        ani = ValueAnimator.ofInt(currentX, toPosition);
        //windowParams.y = Math.min(Math.max(0, initialY),mMetrics.heightPixels - bubble.getMeasuredHeight());

        ani.addUpdateListener(animation -> {
            windowParams.x = (Integer) animation.getAnimatedValue();
            windowManager.updateViewLayout(binding.getRoot(), windowParams);
        });
        ani.setDuration(350L);
        ani.setInterpolator(new AccelerateDecelerateInterpolator());
        ani.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            var service_layout = binding.getRoot();
            if(service_layout.getWindowToken() != null) windowManager.removeView(service_layout);
        }
        if (trash_layout != null) {
            if(trash_layout.getWindowToken() != null) windowManager.removeView(trash_layout);
        }
        if(tts!=null) tts.finishTTS();

        if(viewModel != null) unbindObservers();
    }

    private void unbindObservers() {
        viewModel.getPlayingAudio().removeObserver(playAudioObserver);
        viewModel.getLangDetected().removeObserver(langDetectedObserver);
        viewModel.getTextTranslated().removeObserver(textTranslatedObserver);
        viewModel.getLangToPreference().removeObserver(langToPreferenceObserver);
    }

    private static class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

}
