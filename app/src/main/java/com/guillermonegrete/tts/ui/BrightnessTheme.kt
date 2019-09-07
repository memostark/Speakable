package com.guillermonegrete.tts.ui

enum class BrightnessTheme(val value: String) {
    WHITE("white"),
    BEIGE("beige"),
    BLACK("black");

    companion object{
        const val PREFERENCE_KEY = "brightness_pref_key"

        fun get(value: String): BrightnessTheme {
            return values().find { it.value == value } ?: WHITE
        }
    }
}