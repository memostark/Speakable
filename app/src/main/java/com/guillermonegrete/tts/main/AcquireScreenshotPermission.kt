/*
        Reference: https://stackoverflow.com/questions/33398211/how-do-i-get-a-media-projection-manager-without-disturbing-the-current-foregroun
*/
package com.guillermonegrete.tts.main

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import com.guillermonegrete.tts.services.ScreenTextService
import com.guillermonegrete.tts.services.ScreenTextService.NORMAL_SERVICE


class AcquireScreenshotPermission : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY)
        } else {
            getScreenCaptureIntent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // For the overlay permission, the result code might not be OK and the intent data null, so handle separately.
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                getScreenCaptureIntent()
            } else {
                finish()
            }
            return
        }

        if (resultCode == RESULT_OK && data != null) {
            when(requestCode) {
                REQUEST_CODE_SCREEN_CAPTURE -> {
                    val intent = Intent(this, ScreenTextService::class.java)
                    val intentAction = getIntent().action
                    // If an action is set (e.g. translate text), return it to the service so it performs the action otherwise return a normal service action
                    val action = if (intentAction == null) {
                        NORMAL_SERVICE
                    } else {
                        // If the intent action is VIEW, it means this was called from a shortcut, start a normal service
                        if (intentAction == Intent.ACTION_VIEW) NORMAL_SERVICE else intentAction
                    }
                    intent.action = action
                    intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                    intent.putExtras(data)
                    startService(intent)
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun getScreenCaptureIntent(){
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 200
        const val REQUEST_CODE_DRAW_OVERLAY = 400
    }
}
