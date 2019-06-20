package com.guillermonegrete.tts.importtext

enum class ImportedFileType(val mimeType: String) {
    TXT("text/plain"),
    EPUB("application/epub+zip")
}