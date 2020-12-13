package com.guillermonegrete.tts.importtext.epub

data class EPUBMetadata(
    val title: String,
    val author: String,
    val language: String,
    val cover: String
){
    constructor(): this("", "", "", "")
}