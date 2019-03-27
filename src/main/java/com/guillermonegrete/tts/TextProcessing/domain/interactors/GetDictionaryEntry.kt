package com.guillermonegrete.tts.TextProcessing.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem
import com.guillermonegrete.tts.data.source.DictionaryDataSource
import com.guillermonegrete.tts.data.source.DictionaryRepository
import com.guillermonegrete.tts.db.Words

class GetDictionaryEntry(
        executor: Executor,
        mainThread: MainThread,
        private val dictionaryRepository: DictionaryRepository,
        private val text: String,
        private val callback: GetDictionaryEntryInteractor.Callback
) : AbstractInteractor(executor, mainThread), GetDictionaryEntryInteractor {

    override fun run() {

        dictionaryRepository.getDefinition(text, object : DictionaryDataSource.GetDefinitionCallback{

            override fun onDefinitionLoaded(definitions: MutableList<WikiItem>?) {
                if (definitions != null) {
                    callback.onDictionaryLayoutDetermined(definitions)
                }
            }

            override fun onDataNotAvailable() {
                callback.onEntryNotAvailable()
            }

        })
    }

}