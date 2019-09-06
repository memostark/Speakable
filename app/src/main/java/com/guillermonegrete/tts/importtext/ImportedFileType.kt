package com.guillermonegrete.tts.importtext

enum class ImportedFileType(val mimeType: String) {
    TXT("text/plain"),
    EPUB("application/epub+zip");

    companion object{

        fun get(value: String): ImportedFileType{
            return values().find { it.mimeType == value } ?: TXT
        }
    }
}