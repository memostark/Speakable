/*
    Custom text selection: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Floating activity: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
*/

package com.guillermonegrete.tts.textprocessing;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.guillermonegrete.tts.R;


public class ProcessTextActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setFinishOnTouchOutside(false);
        detectStatusBar();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
    }

    private String getSelectedText() {
        Intent intent = getIntent();
        final CharSequence selected_text = intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        return selected_text.toString();
    }

    private void showDialog(){
        String selectedText = getSelectedText();

        TextInfoDialog dialog = TextInfoDialog.newInstance(
                selectedText,
                getIntent().getAction(),
                getIntent().getParcelableExtra("Word")
        );
        dialog.show(getSupportFragmentManager(), "Text_info");
    }

    private void hideSystemUI() {

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * Creates transparent layout that covers the whole screen height, waits until the view has been added.
     * If view height is the same as the screen height then status bar is hidden.
     * If bar is hidden we configure the dialog with immersive mode. Taken from: https://stackoverflow.com/a/9195733/10244759
     */
    private void detectStatusBar(){

        final WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        p.gravity = Gravity.END | Gravity.TOP;
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        p.width = 1;
        p.height = WindowManager.LayoutParams.MATCH_PARENT;
        p.format = PixelFormat.TRANSPARENT;

        View helperWnd = new View(this);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if(wm != null) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int heightScreen = size.y;

            wm.addView(helperWnd, p);

            final ViewTreeObserver vto = helperWnd.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    boolean isStatusBarHidden = heightScreen == helperWnd.getHeight();
                    if(isStatusBarHidden) {
                        hideSystemUI();
                    }
                    showDialog();
                    wm.removeView(helperWnd);
                    helperWnd.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

        }else{
            showDialog();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}

