package com.guillermonegrete.tts.data.preferences

import com.guillermonegrete.tts.common.models.Gestures
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun setLanguageTo(language: String)

    fun setLanguageFrom(language: String)

    suspend fun setShowSavedWords(enabled: Boolean)

    fun getLanguageTo(): String

    fun getLanguageFrom(): String

    fun showSavedWords(): Flow<Boolean>

    fun getImportTabPosition(): Flow<Int>

    fun getGestures(): Gestures

    suspend fun setImportTabPosition(pos: Int)
}
