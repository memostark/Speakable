/*
        Reference: https://stackoverflow.com/questions/33398211/how-do-i-get-a-media-projection-manager-without-disturbing-the-current-foregroun
*/
package com.guillermonegrete.tts.main

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

import com.guillermonegrete.tts.Services.ScreenTextService

import com.guillermonegrete.tts.Services.ScreenTextService.NORMAL_SERVICE


class AcquireScreenshotPermission : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = this.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                val intent = Intent(this, ScreenTextService::class.java)
                intent.action = NORMAL_SERVICE
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                intent.putExtras(data)
                startService(intent)
            }
        }
        finish()
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 200
    }
}
