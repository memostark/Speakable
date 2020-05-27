package com.guillermonegrete.tts.data.preferences

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
}