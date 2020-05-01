package com.guillermonegrete.tts.importtext.epub

data class Book(
    val title: String,
    val currentChapter: String,
    val spine: List<SpineItem>,
    val manifest: Map<String, String>,
    val tableOfContents: TableOfContents
)

data class SpineItem(
    val idRef:String,
    val href: String,
    val charCount: Int
)