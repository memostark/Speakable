package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.ExternalLink

interface ExternalLinksDataSource {

    fun getLanguageLinks(language: String, callback: Callback)

    fun getLanguageLinks(language: String): List<ExternalLink>

    interface Callback{
        fun onLinksRetrieved(links: List<ExternalLink>)
    }
}