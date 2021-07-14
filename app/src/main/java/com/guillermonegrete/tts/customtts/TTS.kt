package com.guillermonegrete.tts.customtts

import java.util.*

interface TTS {
    fun getAvailableLanguages(): List<Locale>
}