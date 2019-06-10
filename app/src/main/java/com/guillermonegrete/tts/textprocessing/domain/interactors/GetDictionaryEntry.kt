package com.guillermonegrete.tts.textprocessing.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem
import com.guillermonegrete.tts.data.source.DictionaryDataSource
import com.guillermonegrete.tts.data.source.DictionaryRepository

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
                    mMainThread.post{ callback.onDictionaryLayoutDetermined(definitions) }
                }
            }

            override fun onDataNotAvailable() {
                callback.onEntryNotAvailable()
            }

        })
    }

}