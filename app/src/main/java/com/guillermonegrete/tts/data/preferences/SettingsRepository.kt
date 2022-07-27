package com.guillermonegrete.tts.data.preferences

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun setLanguageTo(language: String)

    fun setLanguageFrom(language: String)

    fun getLanguageTo(): String

    fun getLanguageFrom(): String

    fun getImportTabPosition(): Flow<Int>

    suspend fun setImportTabPosition(pos: Int)
}
