/*
    Custom text selection: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Floating activity: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
*/

package com.guillermonegrete.tts.textprocessing;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Window;

import com.guillermonegrete.tts.R;


public class ProcessTextActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        String selectedText = getSelectedText();

        TextInfoDialog dialog = TextInfoDialog.newInstance(
                selectedText,
                getIntent().getAction(),
                getIntent().getParcelableExtra("Word")
        );
        dialog.show(getSupportFragmentManager(), "Text_info");
    }

    private String getSelectedText() {
        Intent intent = getIntent();
        final CharSequence selected_text = intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        return selected_text.toString();
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

