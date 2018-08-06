package com.microsoft.sdksample.CustomViews;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.microsoft.sdksample.R;

import java.lang.ref.WeakReference;

public class TrashView extends FrameLayout {

    static final int ANIMATION_NONE = 0;
    static final int ANIMATION_OPEN = 1;
    static final int ANIMATION_CLOSE = 2;
    static final int ANIMATION_FORCE_CLOSE = 3;

    private static final int BACKGROUND_HEIGHT = 164;
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final float TARGET_CAPTURE_HORIZONTAL_REGION = 30.0f;
    private static final float TARGET_CAPTURE_VERTICAL_REGION = 4.0f;
    private static final long TRASH_ICON_SCALE_DURATION_MILLIS = 200L;

    private final WindowManager mWindowManager;
    private ObjectAnimator mEnterScaleAnimator;
    private ObjectAnimator mExitScaleAnimator;
    private AnimationHandler mAnimationHandler;
    private final DisplayMetrics mMetrics;

    private final FrameLayout mRootView;
    private final FrameLayout mTrashIconRootView;
    private final ImageView mActionTrashIconView;

    private int mActionTrashIconBaseWidth;
    private int mActionTrashIconBaseHeight;

    private float mActionTrashIconMaxScale;

    Paint myPaint;
    Rect rec;


    public TrashView(Context context) {
        super(context);
        mRootView = (FrameLayout) inflate(context,R.layout.trash_layout,null);
        mRootView.setClipChildren(false);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mAnimationHandler = new AnimationHandler(this);
        mMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(mMetrics);

        final FrameLayout backgroundView = (FrameLayout) mRootView.findViewById(R.id.backgroundView);
        backgroundView.setAlpha(0.0f);
        final GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0x50000000});
        backgroundView.setBackground(gradientDrawable);
        final FrameLayout.LayoutParams backgroundParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (BACKGROUND_HEIGHT * mMetrics.density));
        backgroundParams.gravity = Gravity.BOTTOM;
        mRootView.updateViewLayout(backgroundView, backgroundParams);
        mTrashIconRootView = (FrameLayout) mRootView.findViewById(R.id.trash_icon_container);
        mTrashIconRootView.setClipChildren(false);

        mActionTrashIconView = (ImageView) mRootView.findViewById(R.id.trash_action_icon);

        setWillNotDraw(false); //------------FOR TESTING----------------------
        myPaint = new Paint();
        rec = new Rect();
        myPaint.setStyle(Paint.Style.STROKE);

        final Drawable drawable = mActionTrashIconView.getDrawable();
        if (drawable != null) {
            mActionTrashIconBaseWidth = drawable.getIntrinsicWidth();
            mActionTrashIconBaseHeight = drawable.getIntrinsicHeight();
        }

        myPaint = new Paint();
        myPaint.setColor(Color.rgb(0,0,0));
        myPaint.setStrokeWidth(10);
        myPaint.setStyle(Paint.Style.STROKE);
        rec = new Rect();

        addView(mRootView);
    }

    public void setRect(Rect rect){
        rec=rect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(rec, myPaint);
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
        mAnimationHandler.mTargetWidth = 48;
        updateActionTrashIcon(160.0f,160.0f);
        mAnimationHandler.onUpdateViewLayout();
    }

    public void getWindowDrawingRect(Rect outRect) {

        //final ImageView iconView = hasActionTrashIcon() ? mActionTrashIconView : mFixedTrashIconView;
        final ImageView iconView = mActionTrashIconView;
        final float iconPaddingLeft = iconView.getPaddingLeft();
        final float iconPaddingTop = iconView.getPaddingTop();
        //Log.d("PRUEBA", "PaddingLeft: "+iconPaddingLeft+" PaddingTop: "+iconPaddingTop);
        final float iconWidth = iconView.getWidth() - iconPaddingLeft - iconView.getPaddingRight();
        final float iconHeight = iconView.getHeight() - iconPaddingTop - iconView.getPaddingBottom();
        final float x = mTrashIconRootView.getX() + iconPaddingLeft;
        final float y = mRootView.getHeight() - mTrashIconRootView.getY() - iconPaddingTop - iconHeight;
        final int left = (int) (x - TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density);
        final int top = mRootView.getHeight() - (int) (y + iconHeight + TARGET_CAPTURE_VERTICAL_REGION * mMetrics.density);
        final int right = (int) (x + iconWidth + TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density);
        final int bottom = mRootView.getHeight();
        outRect.set(left, top, right, bottom);
    }

    void updateActionTrashIcon(float width, float height) {

        mAnimationHandler.mTargetWidth = width;
        mAnimationHandler.mTargetHeight = height;
        final float newWidthScale = width / mActionTrashIconBaseWidth;
        final float newHeightScale = height / mActionTrashIconBaseHeight;
        mActionTrashIconMaxScale = Math.max(newWidthScale, newHeightScale);
        // ENTERアニメーション作成
        mEnterScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, mActionTrashIconMaxScale), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, mActionTrashIconMaxScale));
        mEnterScaleAnimator.setInterpolator(new OvershootInterpolator());
        mEnterScaleAnimator.setDuration(TRASH_ICON_SCALE_DURATION_MILLIS);
        // Exitアニメーション作成
        mExitScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, 1.0f), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, 1.0f));
        mExitScaleAnimator.setInterpolator(new OvershootInterpolator());
        mExitScaleAnimator.setDuration(TRASH_ICON_SCALE_DURATION_MILLIS);
    }

    public float getTrashIconCenterX() {
        final ImageView iconView = mActionTrashIconView;
        final float iconViewPaddingLeft = iconView.getPaddingLeft();
        final float iconWidth = iconView.getWidth() - iconViewPaddingLeft - iconView.getPaddingRight();
        final float x = mTrashIconRootView.getX() + iconViewPaddingLeft;
        return x;
    }

    public float getTrashIconCenterY() {
        final ImageView iconView = mActionTrashIconView;
        final float iconViewHeight = iconView.getHeight();
        final float iconViewPaddingBottom = iconView.getPaddingBottom();
        final float iconHeight = iconViewHeight - iconView.getPaddingTop() - iconViewPaddingBottom;
        final float y = mTrashIconRootView.getY() + iconViewPaddingBottom;
        return y;
    }

    private void setScaleTrashIconImmediately(boolean isEnter) {
        cancelScaleTrashAnimation();

        mActionTrashIconView.setScaleX(isEnter ? mActionTrashIconMaxScale : 1.0f);
        mActionTrashIconView.setScaleY(isEnter ? mActionTrashIconMaxScale : 1.0f);
    }

    public void setScaleTrashIcon(boolean isEnter) {

        cancelScaleTrashAnimation();

        // 領域に入った場合
        if (isEnter) {
            mEnterScaleAnimator.start();
            //Log.d("prueba","mEnterScaleAnimator.start()");
        } else {
            mExitScaleAnimator.start();
            //Log.d("prueba","mExitScaleAnimator.start()");
        }
    }

    private void cancelScaleTrashAnimation() {
        // 枠内アニメーション
        if (mEnterScaleAnimator != null && mEnterScaleAnimator.isStarted()) {
            mEnterScaleAnimator.cancel();
        }

        // 枠外アニメーション
        if (mExitScaleAnimator != null && mExitScaleAnimator.isStarted()) {
            mExitScaleAnimator.cancel();
        }
    }

    public void onTouchFloatingView(MotionEvent event, float x, float y){
        final int action = event.getAction();

        if(action == MotionEvent.ACTION_DOWN){
            mAnimationHandler.updateTargetPosition(x, y);
            mAnimationHandler.sendAnimationMessageDelayed(ANIMATION_OPEN, LONG_PRESS_TIMEOUT);

        }else if (action == MotionEvent.ACTION_MOVE){
            if (!mAnimationHandler.isAnimationStarted(ANIMATION_OPEN)) {
                mAnimationHandler.removeMessages(ANIMATION_OPEN);
                mAnimationHandler.sendAnimationMessage(ANIMATION_OPEN);
            }
        }else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
            mAnimationHandler.removeMessages(ANIMATION_OPEN);
            mAnimationHandler.sendAnimationMessage(ANIMATION_CLOSE);
        }
    }

    public void dismiss() {

        mAnimationHandler.removeMessages(ANIMATION_OPEN);
        mAnimationHandler.removeMessages(ANIMATION_CLOSE);
        mAnimationHandler.sendAnimationMessage(ANIMATION_FORCE_CLOSE);

        setScaleTrashIconImmediately(false);
    }

    static class AnimationHandler extends Handler {

        private static final int TYPE_FIRST = 1;
        private static final int TYPE_UPDATE = 2;

        private static final float MAX_ALPHA = 1.0f;
        private static final float MIN_ALPHA = 0.0f;

        private static final float OVERSHOOT_TENSION = 1.0f;
        private static final long TRASH_OPEN_DURATION_MILLIS = 400L;
        private static final long TRASH_OPEN_START_DELAY_MILLIS = 200L;
        private static final long TRASH_CLOSE_DURATION_MILLIS = 200L;
        private static final int TRASH_MOVE_LIMIT_OFFSET_X = 22;
        private static final int TRASH_MOVE_LIMIT_TOP_OFFSET = -4;
        private static final long ANIMATION_REFRESH_TIME_MILLIS = 10L;
        private static final long BACKGROUND_DURATION_MILLIS = 200L;

        private final WeakReference<TrashView> mTrashView;

        private float mStartAlpha;
        private int mStartedCode;
        private long mStartTime;
        private float mStartTransitionY;
        private float mTargetPositionX;
        private float mTargetPositionY;
        private float mTargetWidth;
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
                mStartAlpha = backgroundView.getAlpha();
                mStartTransitionY = trash_icon_cont.getTranslationY();
                mStartedCode = animationCode;
            }

            final float elapsedTime = SystemClock.uptimeMillis() - mStartTime;

            if(animationCode == ANIMATION_OPEN) {

                final float currentAlpha = backgroundView.getAlpha();
                if (currentAlpha < MAX_ALPHA) {
                    final float alphaTimeRate = Math.min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f);
                    final float alpha = Math.min(mStartAlpha + alphaTimeRate, MAX_ALPHA);
                    backgroundView.setAlpha(alpha);
                }

                if (elapsedTime >= TRASH_OPEN_START_DELAY_MILLIS) {
                    final float screenHeight = trashView.mMetrics.heightPixels;
                    final float targetPositionYRate = Math.min(2 * (mTargetPositionY + mTargetHeight) / (screenHeight + mTargetHeight), 1.0f);
                    final float stickyPositionY = mMoveStickyYRange * targetPositionYRate + mTrashIconLimitPosition.height() - mMoveStickyYRange;
                    final float translationYTimeRate = Math.min((elapsedTime - TRASH_OPEN_START_DELAY_MILLIS) / TRASH_OPEN_DURATION_MILLIS, 1.0f);
                    final float positionY = mTrashIconLimitPosition.bottom - stickyPositionY * mOvershootInterpolator.getInterpolation(translationYTimeRate);
                    // Log.d("prueba","TargetpositionPositionYRate: "+targetPositionYRate+" StickyY: "+stickyPositionY+" TimeRate: "+translationYTimeRate+" PosY: "+positionY);
                    trash_icon_cont.setTranslationY(positionY);

                }

                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
            } else if (animationCode == ANIMATION_CLOSE) {
                final float alphaElapseTimeRate = Math.min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f);
                final float alpha = Math.max(mStartAlpha - alphaElapseTimeRate, MIN_ALPHA);
                backgroundView.setAlpha(alpha);

                final float translationYTimeRate = Math.min(elapsedTime / TRASH_CLOSE_DURATION_MILLIS, 1.0f);
                if (alphaElapseTimeRate < 1.0f || translationYTimeRate < 1.0f) {
                    final float position = mStartTransitionY + mTrashIconLimitPosition.height() * translationYTimeRate;
                    trash_icon_cont.setTranslationY(position);
                    sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
                } else {
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
