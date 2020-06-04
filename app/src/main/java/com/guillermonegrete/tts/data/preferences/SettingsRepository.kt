package com.guillermonegrete.tts.data.preferences

interface SettingsRepository {

    fun setLanguageTo(language: String)

    fun setLanguageFrom(language: String)

    fun getLanguageTo(): String

    fun getLanguageFrom(): String
}