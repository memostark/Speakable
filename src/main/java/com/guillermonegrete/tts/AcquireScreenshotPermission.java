/*
        Reference: https://stackoverflow.com/questions/33398211/how-do-i-get-a-media-projection-manager-without-disturbing-the-current-foregroun
*/
package com.guillermonegrete.tts;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;


public class AcquireScreenshotPermission extends Activity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) AcquireScreenshotPermission.this.getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), TextToSpeechFragment.REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TextToSpeechFragment.REQUEST_CODE_SCREEN_CAPTURE) {
            if (Activity.RESULT_OK == resultCode) {
                ScreenTextService.setScreenshotPermission((Intent) data.clone(), resultCode);
            }
        } else if (Activity.RESULT_CANCELED == resultCode) {
            ScreenTextService.setScreenshotPermission(null, resultCode);
            Log.e("PermissionActivity","no access");

        }
        finish();
    }
}
