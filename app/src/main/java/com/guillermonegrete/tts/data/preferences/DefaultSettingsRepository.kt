package com.guillermonegrete.tts.data.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class DefaultSettingsRepository @Inject constructor(private val preferences: SharedPreferences): SettingsRepository {


    override fun setLanguageTo(language: String) {
        preferences.edit { putString(LANGUAGE_TO_KEY, language) }
    }

    override fun setLanguageFrom(language: String) {
        preferences.edit { putString(LANGUAGE_FROM_KEY, language) }
    }

    override fun getLanguageTo(): String {
        return preferences.getString(LANGUAGE_TO_KEY, null) ?: "en"
    }

    override fun getLanguageFrom(): String {
        return preferences.getString(LANGUAGE_FROM_KEY, null) ?: "auto"
    }

    companion object{
        const val LANGUAGE_TO_KEY = "translate_to_pref"
        const val LANGUAGE_FROM_KEY = "translate_from_pref_key"
    }
}