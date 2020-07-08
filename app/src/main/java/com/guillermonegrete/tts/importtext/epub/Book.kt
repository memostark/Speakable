package com.guillermonegrete.tts.importtext.epub

data class Book(
    val title: String,
    var currentChapter: String,
    val spine: List<SpineItem>,
    val manifest: Map<String, String>,
    val tableOfContents: TableOfContents = TableOfContents()
){
    val totalChars
        get() = spine.sumBy { it.charCount }
}

data class SpineItem(
    val idRef:String,
    val href: String,
    var charCount: Int = 0
)