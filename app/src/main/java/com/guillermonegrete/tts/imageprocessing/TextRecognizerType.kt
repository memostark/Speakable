package com.guillermonegrete.tts.imageprocessing

import dagger.MapKey

enum class TextRecognizerType(val value: Int) {
    FIREBASE_LOCAL(0),
    FIREBASE_CLOUD(1);

    companion object{
        const val PREFERENCE_KEY = "textRecognizerPref"

        fun valueOf(value: Int): TextRecognizerType {
            return values().find { it.value == value } ?: FIREBASE_LOCAL
        }
    }
}

@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class TextRecognizerEnumKey(val value: TextRecognizerType)