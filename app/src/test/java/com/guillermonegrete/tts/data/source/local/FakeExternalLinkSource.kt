package com.guillermonegrete.tts.data.source.local

import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink

class FakeExternalLinkSource: ExternalLinksDataSource {
    override fun getLanguageLinks(language: String, callback: ExternalLinksDataSource.Callback) {
        TODO("Not yet implemented")
    }

    override fun getLanguageLinks(language: String): List<ExternalLink> {
        TODO("Not yet implemented")
    }
}
