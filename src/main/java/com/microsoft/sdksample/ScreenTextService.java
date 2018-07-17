/*
        Referencias: http://www.piwai.info/chatheads-basics
 */

package com.microsoft.sdksample;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class ScreenTextService extends Service {

    private WindowManager windowManager;
    private ImageView bubble;
    private String TAG = this.getClass().getSimpleName();
    private GestureDetector gestureDetector;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        bubble = new ImageView(this);
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
                            windowManager.updateViewLayout(bubble, params);
                            return true;
                    }
                }

                return false;
            }
        });

        windowManager.addView(bubble, params);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubble != null) windowManager.removeView(bubble);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }
}
