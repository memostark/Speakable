package com.guillermonegrete.tts.main

/**
 * Value must be the same as the array resource in arrays.xml
 */
enum class TranslatorType(val value: Int) {
    GOOGLE_PUBLIC(10),
    MICROSOFT(11);

    companion object{
        const val PREFERENCE_KEY = "translator_pref_key"

        fun valueOf(value: Int): TranslatorType{
            return values().find { it.value == value } ?: GOOGLE_PUBLIC
        }
    }
}