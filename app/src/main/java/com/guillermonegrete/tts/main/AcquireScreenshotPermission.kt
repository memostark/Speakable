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
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            when(requestCode) {
                REQUEST_CODE_SCREEN_CAPTURE -> {
                    val intent = Intent(this, ScreenTextService::class.java)
                    intent.action = NORMAL_SERVICE
                    intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                    intent.putExtras(data)
                    startService(intent)
                    finish()
                }
                REQUEST_CODE_DRAW_OVERLAY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                        getScreenCaptureIntent()
                    } else {
                        finish()
                    }
                }
            }
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
