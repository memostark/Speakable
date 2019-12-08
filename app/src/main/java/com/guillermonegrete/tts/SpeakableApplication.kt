package com.guillermonegrete.tts

import com.guillermonegrete.tts.di.DaggerApplicationComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import androidx.preference.PreferenceManager
import com.guillermonegrete.tts.di.ApplicationComponent
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.utils.DEFAULT_MODE
import com.guillermonegrete.tts.utils.applyTheme


class SpeakableApplication: DaggerApplication() {

    val appComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.factory().create(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Set app theme
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePref = sharedPreferences.getString(SettingsFragment.PREF_THEME, DEFAULT_MODE)
        themePref?.let { applyTheme(themePref) }
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> = appComponent
}