package com.guillermonegrete.tts.importtext.epub

data class EPUBMetadata(
    val title: String,
    val author: String,
    val language: String,
    val cover: String,
    /**
     * The language that was defined by the user, this differs from the [language] found in the EPUB file metadata.
     */
    val setLanguage: String? = null,
)
