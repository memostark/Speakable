package com.guillermonegrete.tts.CustomViews;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import java.lang.ref.WeakReference;


public class BubbleView extends ImageView {

    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mParams;

    public final FloatingAnimationHandler mAnimationHandler;

    static final int STATE_NORMAL = 0;
    static final int STATE_INTERSECTING = 1;
    static final int STATE_FINISHING = 2;

    private float mScreenTouchX;
    private float mScreenTouchY;

    public BubbleView(Context context) {
        super(context);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams();
        mAnimationHandler = new FloatingAnimationHandler(this);
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.type =  WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.gravity = Gravity.START | Gravity.BOTTOM;

    }

    public BubbleView(Context context, AttributeSet attrs){
        super(context, attrs);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams();
        mAnimationHandler = new FloatingAnimationHandler(this);
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.type =  WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.gravity = Gravity.START | Gravity.BOTTOM;

    }

    private void updateViewLayout() {
        if (!ViewCompat.isAttachedToWindow(this)) {
            return;
        }
        mWindowManager.updateViewLayout(this, mParams);
    }

    public WindowManager.LayoutParams getWindowLayoutParams() {
        return mParams;
    }

    public void setCoordinatesByTouch(float x, float y){
        mScreenTouchX = x;
        mScreenTouchY = y;

    }

    public void setNormal() {
        mAnimationHandler.setState(STATE_NORMAL);
    }

    public void setIntersecting(int centerX, int centerY) {
        mAnimationHandler.setState(STATE_INTERSECTING);
        mAnimationHandler.updateTargetPosition(centerX, centerY);
    }

    public void setFinishing() {
        mAnimationHandler.setState(STATE_FINISHING);
    }

    public int getState() {
        return mAnimationHandler.getState();
    }


    public static class FloatingAnimationHandler extends Handler {

        private static final long ANIMATION_REFRESH_TIME_MILLIS = 10L;

        public static final int ANIMATION_NONE = 0;
        public static final int ANIMATION_IN_TOUCH = 1;

        private static final int TYPE_FIRST = 1;
        private static final int TYPE_UPDATE = 2;

        private long mStartTime;
        private float mStartX;
        private float mStartY;
        private int mStartedCode;
        private int mState;
        private boolean mIsChangeState;

        private float mTargetPositionX;
        private float mTargetPositionY;

        private float mTouchPositionX;
        private float mTouchPositionY;

        private final WeakReference<BubbleView> mBubbleView;

        FloatingAnimationHandler(BubbleView bubbleView){
            mBubbleView = new WeakReference<>(bubbleView);
            mState = STATE_NORMAL;
            mStartedCode = ANIMATION_NONE;
        }

        @Override
        public void handleMessage(Message msg) {
            final BubbleView bubbleView = mBubbleView.get();
            if (bubbleView == null) {
                removeMessages(ANIMATION_IN_TOUCH);
                return;
            }

            final int animationCode = msg.what;
            final int animationType = msg.arg1;
            final WindowManager.LayoutParams params = bubbleView.mParams;

            if(animationType == TYPE_FIRST){
                mStartTime = mIsChangeState ? SystemClock.uptimeMillis() : 0;
                mStartX = params.x;
                mStartY = params.y;
                mStartedCode = animationCode;
                mIsChangeState = false;
            }

            if(mState == BubbleView.STATE_INTERSECTING){
                //final float basePosition = calcAnimationPosition(trackingTargetTimeRate);
                // 最終的な到達点
                final float targetPositionX = mTargetPositionX - bubbleView.getWidth() / 2;
                final float targetPositionY = mTargetPositionY - bubbleView.getHeight() / 2;
                // 現在地からの移動
                params.x = (int) (mStartX + (targetPositionX - mStartX));
                params.y = (int) (mStartY + (targetPositionY - mStartY));
                //bubbleView.updateViewLayout();
                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
            }


        }

        public void sendAnimationMessage(int animation) {
            sendMessage(newMessage(animation, TYPE_FIRST));
        }

        void updateTouchPosition(float positionX, float positionY) {
            mTouchPositionX = positionX;
            mTouchPositionY = positionY;
        }

        private static Message newMessage(int animation, int type) {
            final Message message = Message.obtain();
            message.what = animation;
            message.arg1 = type;
            return message;
        }

        void updateTargetPosition(float centerX, float centerY) {
            mTargetPositionX = centerX;
            mTargetPositionY = centerY;
        }

        public void setState(int newState) {
            // 状態が異なった場合のみ状態を変更フラグを変える
            if (mState != newState) {
                mIsChangeState = true;
            }
            mState = newState;
        }

        /**
         * 現在の状態を返します。
         *
         * @return STATE_NORMAL or STATE_INTERSECTING or STATE_FINISHING
         */
        public int getState() {
            return mState;
        }
    }
}
