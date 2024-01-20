package com.guillermonegrete.tts.importtext

enum class ImportedFileType(val mimeType: String) {
    TXT("text/plain"),
    EPUB("application/epub+zip"),
    /**
     * In APIs < 23, EPUB files are treated as octet-streams.
     */
    OCTET_STREAM("application/octet-stream");

    companion object{

        fun get(value: String): ImportedFileType{
            return values().find { it.mimeType == value } ?: TXT
        }
    }
}