package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader
import javax.inject.Inject

class FakeDictionarySource @Inject constructor(): DictionaryDataSource {
    override fun getDefinition(word: String, callback: DictionaryDataSource.GetDefinitionCallback) {

        val items = listOf(WiktionaryLangHeader("English"), WiktionaryItem(word, "Description"))
        callback.onDefinitionLoaded(items)
    }
}
