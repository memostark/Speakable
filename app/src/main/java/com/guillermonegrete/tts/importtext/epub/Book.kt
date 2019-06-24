package com.guillermonegrete.tts.importtext.epub

data class Book(
    val title: String,
    val currentChapter: String,
    val spine: List<String>,
    val manifest: Map<String, String>,
    val tableOfContents: TableOfContents
)