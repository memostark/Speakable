package com.guillermonegrete.tts.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.guillermonegrete.tts.main.SettingsFragment

const val LIGHT_MODE = "light"
const val DARK_MODE = "dark"
const val DEFAULT_MODE = "default"

fun applyTheme(themePref: String) {
    when (themePref) {
        LIGHT_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        DARK_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}

/**
 * Returns the night mode preference for this app, if no specific app preference is set then it returns the system preference.
 */
fun isNightMode(context: Context): Boolean {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    return when(sharedPreferences.getString(SettingsFragment.PREF_THEME, DEFAULT_MODE)) {
        LIGHT_MODE -> false
        DARK_MODE -> true
        else -> { // Use mode set in the phone's system
            when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> true // Night mode should be default so return true for every other case
            }
        }
    }
}