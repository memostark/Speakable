/*
        Reference: https://stackoverflow.com/questions/33398211/how-do-i-get-a-media-projection-manager-without-disturbing-the-current-foregroun
*/
package com.guillermonegrete.tts.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

import com.guillermonegrete.tts.services.ScreenTextService

import com.guillermonegrete.tts.services.ScreenTextService.NORMAL_SERVICE


class AcquireScreenshotPermission : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY)
        } else {
            getScreenCaptureIntent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // For the overlay permission, the result code might not be OK and the intent data null, so handle separately.
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                getScreenCaptureIntent()
            } else {
                finish()
            }
            return
        }

        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            when(requestCode) {
                REQUEST_CODE_SCREEN_CAPTURE -> {
                    val intent = Intent(this, ScreenTextService::class.java)
                    // If an action is set, return it to the service so it performs the action otherwise return a normal service action
                    val action = getIntent().action ?: NORMAL_SERVICE
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
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 200
        const val REQUEST_CODE_DRAW_OVERLAY = 400
    }
}
