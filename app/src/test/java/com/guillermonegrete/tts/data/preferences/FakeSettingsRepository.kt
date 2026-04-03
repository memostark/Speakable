package com.guillermonegrete.tts.data.preferences

import com.guillermonegrete.tts.common.models.Gestures
import kotlinx.coroutines.flow.Flow

class FakeSettingsRepository: SettingsRepository {

    private var languageFrom = "auto"
    private var languageTo = "auto"

    override fun setLanguageTo(language: String) {
        languageTo = language
    }

    override fun setLanguageFrom(language: String) {
        languageFrom = language
    }

    override suspend fun setShowSavedWords(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getLanguageTo(): String {
        return languageTo
    }

    override fun getLanguageFrom(): String {
        return languageFrom
    }

    override fun showSavedWords(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getImportTabPosition(): Flow<Int> {
        TODO("Not yet implemented")
    }

    override fun getGestures(): Gestures {
        TODO("Not yet implemented")
    }

    override suspend fun setImportTabPosition(pos: Int) {
        TODO("Not yet implemented")
    }
}