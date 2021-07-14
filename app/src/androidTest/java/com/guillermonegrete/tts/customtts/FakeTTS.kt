package com.guillermonegrete.tts.customtts

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeTTS @Inject constructor(): TTS {
    var languages = arrayListOf<Locale>()

    override fun getAvailableLanguages(): List<Locale> = languages
}