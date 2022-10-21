package com.guillermonegrete.tts.data.preferences

import kotlinx.coroutines.flow.Flow

class FakeSettingsRepository: SettingsRepository {

    override fun setLanguageTo(language: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setLanguageFrom(language: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLanguageTo(): String {
        return "en"
    }

    override fun getLanguageFrom(): String {
        return "auto"
    }

    override fun getImportTabPosition(): Flow<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun setImportTabPosition(pos: Int) {
        TODO("Not yet implemented")
    }
}