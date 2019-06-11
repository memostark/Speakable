package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.ExternalLink

interface ExternalLinksDataSource {

    fun getLanguageLinks(language: String, callback: Callback)

    interface Callback{
        fun onLinksRetrieved(links: List<ExternalLink>)
    }
}