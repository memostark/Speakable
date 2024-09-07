package com.guillermonegrete.tts.ui

enum class BrightnessTheme(val value: String) {
    WHITE("white"),
    BEIGE("beige"),
    BLACK("black");

    companion object{
        const val PREFERENCE_KEY = "brightness_pref_key"

        fun get(value: String): BrightnessTheme {
            return entries.find { it.value == value } ?: WHITE
        }

        fun get(isDarkMode: Boolean) = if (isDarkMode) BLACK else WHITE
    }
}