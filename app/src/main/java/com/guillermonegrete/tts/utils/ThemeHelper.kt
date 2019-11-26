package com.guillermonegrete.tts.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat

const val LIGHT_MODE = "light"
const val DARK_MODE = "dark"
const val DEFAULT_MODE = "default"

fun applyTheme(themePref: String) {
    when (themePref) {
        LIGHT_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        DARK_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else -> {
            if (BuildCompat.isAtLeastQ()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}