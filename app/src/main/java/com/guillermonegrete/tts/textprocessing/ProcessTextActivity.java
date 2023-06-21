/*
    Custom text selection: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Floating activity: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
*/

package com.guillermonegrete.tts.textprocessing;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.provider.Settings;
import android.view.Window;

import com.guillermonegrete.tts.R;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class ProcessTextActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setFinishOnTouchOutside(false);
        hasOverlayDrawPermission();
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
    }

    private String getSelectedText() {
        var intent = getIntent();
        final CharSequence selected_text = intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        return selected_text.toString();
    }

    private void showDialog(){
        var selectedText = getSelectedText();

        var dialog = TextInfoDialog.newInstance(
                selectedText,
                getIntent().getAction(),
                getIntent().getParcelableExtra("Word")
        );
        dialog.show(getSupportFragmentManager(), "Text_info");
    }

    private void hideSystemUI() {

        var decorView = getWindow().getDecorView();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(getWindow(), decorView);
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars());
        controllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /**
     * If SDK is Android M or higher, we need to ask for permission to create the layout to detect the status bar.
     * More <a href="https://developer.android.com/reference/android/Manifest.permission.html#SYSTEM_ALERT_WINDOW">here</a>
     * <p>
     * Because of this we just hide the UI by default if we don't have permission.
     */
    private void hasOverlayDrawPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.canDrawOverlays(this)) {
                detectStatusBar();
            } else {
                requestOverlayPermission();
            }
        } else {
            detectStatusBar();
        }
    }

    /**
     * Checks if the status bar and other bars are initially visible.
     * If the bars are hidden we configure the dialog with immersive mode.
     */
    private void detectStatusBar(){
        var decorView = getWindow().getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {

            if (insets.isVisible(WindowInsetsCompat.Type.systemBars())) {
                hideSystemUI();
            }
            showDialog();
            // Only need the initial state, remove to avoid calling again.
            ViewCompat.setOnApplyWindowInsetsListener(decorView, null);
            return insets;
        });
    }

    private void requestOverlayPermission() {
        var requestOverlayPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Settings.canDrawOverlays(this)) {
                detectStatusBar();
            } else {
                finish();
            }
        });
        var intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        requestOverlayPermission.launch(intent);
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

