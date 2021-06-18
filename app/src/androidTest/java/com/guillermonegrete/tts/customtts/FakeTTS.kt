package com.guillermonegrete.tts.customtts

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeTTS @Inject constructor(): TTS {
    var languages = arrayListOf<String>()

    override fun getAvailableLanguages() = languages
}