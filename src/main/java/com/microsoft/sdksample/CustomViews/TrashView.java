package com.microsoft.sdksample.CustomViews;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import com.microsoft.sdksample.R;

import java.lang.ref.WeakReference;

public class TrashView extends FrameLayout {

    static final int ANIMATION_NONE = 0;
    static final int ANIMATION_OPEN = 1;
    static final int ANIMATION_CLOSE = 2;
    static final int ANIMATION_FORCE_CLOSE = 3;

    private static final int BACKGROUND_HEIGHT = 164;
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private final WindowManager mWindowManager;
    private AnimationHandler mAnimationHandler;
    private final DisplayMetrics mMetrics;


    public TrashView(Context context) {
        super(context);
        FrameLayout rootView = (FrameLayout) inflate(context,R.layout.trash_layout,null);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mAnimationHandler = new AnimationHandler(this);
        mMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(mMetrics);

        final FrameLayout backgroundView = (FrameLayout) rootView.findViewById(R.id.backgroundView);
        final GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0x50000000});
        backgroundView.setBackground(gradientDrawable);
        final FrameLayout.LayoutParams backgroundParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (BACKGROUND_HEIGHT * mMetrics.density));
        backgroundParams.gravity = Gravity.BOTTOM;
        rootView.updateViewLayout(backgroundView, backgroundParams);
        final FrameLayout trashIconContainer = (FrameLayout) rootView.findViewById(R.id.trash_icon_container);
        trashIconContainer.setClipChildren(false);

        addView(rootView);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateViewLayout();
    }

    private void updateViewLayout(){
        mAnimationHandler.mTargetHeight = 48;
        mAnimationHandler.onUpdateViewLayout();
    }

    public void onTouchFloatingView(MotionEvent event, float x, float y){
        final int action = event.getAction();
        Log.d("prueba", "onTouchFloatingView action: "+action);

        if(action == MotionEvent.ACTION_DOWN){
            mAnimationHandler.updateTargetPosition(x, y);
            mAnimationHandler.sendAnimationMessageDelayed(ANIMATION_OPEN, LONG_PRESS_TIMEOUT);

        }else if (action == MotionEvent.ACTION_MOVE){
            if (!mAnimationHandler.isAnimationStarted(ANIMATION_OPEN)) {
                // 長押しのメッセージを削除
                mAnimationHandler.removeMessages(ANIMATION_OPEN);
                // オープン
                mAnimationHandler.sendAnimationMessage(ANIMATION_OPEN);
            }
        }else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
            mAnimationHandler.removeMessages(ANIMATION_OPEN);
            mAnimationHandler.sendAnimationMessage(ANIMATION_CLOSE);
        }
    }

    static class AnimationHandler extends Handler {

        private static final int TYPE_FIRST = 1;
        private static final int TYPE_UPDATE = 2;
        private static final float OVERSHOOT_TENSION = 1.0f;
        private static final long TRASH_OPEN_DURATION_MILLIS = 400L;
        private static final long TRASH_OPEN_START_DELAY_MILLIS = 200L;
        private static final long TRASH_CLOSE_DURATION_MILLIS = 200L;
        private static final int TRASH_MOVE_LIMIT_OFFSET_X = 22;
        private static final int TRASH_MOVE_LIMIT_TOP_OFFSET = -4;
        private static final long ANIMATION_REFRESH_TIME_MILLIS = 10L;

        private final WeakReference<TrashView> mTrashView;

        private int mStartedCode;
        private long mStartTime;
        private float mStartTransitionY;
        private float mTargetPositionX;
        private float mTargetPositionY;
        private float mTargetHeight;
        private float mMoveStickyYRange;


        private final Rect mTrashIconLimitPosition;
        private final OvershootInterpolator mOvershootInterpolator;


        AnimationHandler(TrashView trashView){
            mTrashView = new WeakReference<>(trashView);
            mStartedCode = ANIMATION_NONE;
            mTrashIconLimitPosition = new Rect();
            mOvershootInterpolator = new OvershootInterpolator(OVERSHOOT_TENSION);
        }

        @Override
        public void handleMessage(Message msg) {
            final TrashView trashView = mTrashView.get();

            if (trashView == null) {
                removeMessages(ANIMATION_OPEN);
                removeMessages(ANIMATION_CLOSE);
                removeMessages(ANIMATION_FORCE_CLOSE);
                return;
            }

            final int animationCode = msg.what;
            final int animationType = msg.arg1;
            final FrameLayout trash_icon_cont = (FrameLayout) trashView.findViewById(R.id.trash_icon_container);
            final FrameLayout backgroundView = (FrameLayout) trashView.findViewById(R.id.backgroundView);

            if (animationType == TYPE_FIRST) {
                mStartTime = SystemClock.uptimeMillis();
                mStartTransitionY = trash_icon_cont.getTranslationY();
                mStartedCode = animationCode;
            }

            final float elapsedTime = SystemClock.uptimeMillis() - mStartTime;

            if(animationCode == ANIMATION_OPEN) {
                if (elapsedTime >= TRASH_OPEN_START_DELAY_MILLIS) {
                    final float screenHeight = trashView.mMetrics.heightPixels;
                    final float targetPositionYRate = Math.min(2 * (mTargetPositionY + mTargetHeight) / (screenHeight + mTargetHeight), 1.0f);
                    final float stickyPositionY = mMoveStickyYRange * targetPositionYRate + mTrashIconLimitPosition.height() - mMoveStickyYRange;
                    final float translationYTimeRate = Math.min((elapsedTime - TRASH_OPEN_START_DELAY_MILLIS) / TRASH_OPEN_DURATION_MILLIS, 1.0f);
                    final float positionY = mTrashIconLimitPosition.bottom - stickyPositionY * mOvershootInterpolator.getInterpolation(translationYTimeRate);
                    Log.d("prueba","TargetpositionPositionYRate: "+targetPositionYRate+" StickyY: "+stickyPositionY+" TimeRate: "+translationYTimeRate+" PosY: "+positionY);
                    trash_icon_cont.setTranslationY(positionY);

                }

                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
            } else if (animationCode == ANIMATION_CLOSE) {
                final float translationYTimeRate = Math.min(elapsedTime / TRASH_CLOSE_DURATION_MILLIS, 1.0f);
                // アニメーションが最後まで到達していない場合
                if (translationYTimeRate < 1.0f) {
                    final float position = mStartTransitionY + mTrashIconLimitPosition.height() * translationYTimeRate;
                    trash_icon_cont.setTranslationY(position);
                    sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
                } else {
                    // 位置を強制的に調整
                    trash_icon_cont.setTranslationY(mTrashIconLimitPosition.bottom);
                    mStartedCode = ANIMATION_NONE;
                }
            }  else if (animationCode == ANIMATION_FORCE_CLOSE) {
                backgroundView.setAlpha(0.0f);
                trash_icon_cont.setTranslationY(mTrashIconLimitPosition.bottom);
            }


        }

        boolean isAnimationStarted(int animationCode) {
            return mStartedCode == animationCode;
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

        void onUpdateViewLayout(){
            final TrashView trashView = mTrashView.get();
            if (trashView == null) {
                return;
            }
            final float density = trashView.mMetrics.density;
            final float backgroundHeight = trashView.findViewById(R.id.backgroundView).getMeasuredHeight();
            final float offsetX = TRASH_MOVE_LIMIT_OFFSET_X * density;
            final int trashIconHeight = trashView.findViewById(R.id.trash_icon_container).getMeasuredHeight();
            final int left = (int) -offsetX;
            final int top = (int) (2*(trashIconHeight - backgroundHeight) / 2 - TRASH_MOVE_LIMIT_TOP_OFFSET * density);
            final int right = (int) offsetX;
            final int bottom = trashIconHeight;
            //mTrashIconLimitPosition.set(-100, -600, 100, 200);
            mTrashIconLimitPosition.set(left,top,right,bottom);
            mMoveStickyYRange = backgroundHeight*0.2f;
        }
    }
}
