package com.guillermonegrete.tts.data.source.remote

data class GoogleTranslateResponse(val sentences: List<Sentence>, val src: String)

data class Sentence(val trans: String, val orig: String)
