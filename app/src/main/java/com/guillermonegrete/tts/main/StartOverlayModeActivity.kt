package com.guillermonegrete.tts.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.guillermonegrete.tts.services.ScreenTextService

class StartOverlayModeActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, ScreenTextService::class.java)
        intent.action = ScreenTextService.NO_FLOATING_ICON_SERVICE
        startService(intent)
        finish()
    }
}