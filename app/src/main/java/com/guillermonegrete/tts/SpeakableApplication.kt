package com.guillermonegrete.tts

import android.app.Application
import androidx.preference.PreferenceManager
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.utils.DEFAULT_MODE
import com.guillermonegrete.tts.utils.applyTheme
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpeakableApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // Set app theme
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePref = sharedPreferences.getString(SettingsFragment.PREF_THEME, DEFAULT_MODE)
        themePref?.let { applyTheme(themePref) }
    }
}