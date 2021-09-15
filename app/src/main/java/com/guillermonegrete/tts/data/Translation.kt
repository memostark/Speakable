package com.guillermonegrete.tts.data

data class Translation(val sentences: List<Segment>, val src: String){

    val translatedText = sentences.joinToString(""){ it.trans }
}

data class Segment(val trans: String, val orig: String)