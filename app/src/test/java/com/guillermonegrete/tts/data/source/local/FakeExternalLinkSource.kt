package com.guillermonegrete.tts.data.source.local

import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink
import java.lang.RuntimeException
import java.util.LinkedHashMap

class FakeExternalLinkSource: ExternalLinksDataSource {

    var linksData = LinkedHashMap<String, MutableList<ExternalLink>>()

    override fun getLanguageLinks(language: String, callback: ExternalLinksDataSource.Callback) {
        TODO("Not yet implemented")
    }

    override fun getLanguageLinks(language: String): List<ExternalLink> {
        return linksData[language] ?: throw RuntimeException("No external links found for $language")
    }

    fun addLinks(vararg links: ExternalLink) {
        for(link in links) {
            val language = link.language
            val list = linksData[language]
            if (list == null) {
                linksData[language] = mutableListOf(link)
            } else {
                list.add(link)
            }
        }
    }
}
