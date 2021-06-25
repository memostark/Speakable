package com.guillermonegrete.tts.main

import dagger.MapKey

/**
 * Value must be the same as the array resource in arrays.xml
 */
enum class TranslatorType(val value: Int) {
    GOOGLE_PUBLIC(10);

    companion object{
        const val PREFERENCE_KEY = "translator_pref_key"

        fun valueOf(value: Int): TranslatorType{
            return values().find { it.value == value } ?: GOOGLE_PUBLIC
        }
    }
}

@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class TranslatorEnumKey(val value: TranslatorType)