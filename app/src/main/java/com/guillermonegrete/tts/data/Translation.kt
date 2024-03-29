package com.guillermonegrete.tts.data

data class Translation(
    val sentences: List<Segment>,
    /**
     * Original language of the translation (language from).
     */
    val src: String
){

    val originalText: String = sentences.joinToString("") { it.orig }

    val translatedText = sentences.joinToString(""){ it.trans }
}

data class Segment(val trans: String, val orig: String)