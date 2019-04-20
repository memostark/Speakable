package com.guillermonegrete.tts.main.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.data.source.WordRepository
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.db.Words

class GetLangAndTranslation @JvmOverloads constructor(
    executor: Executor,
    mainThread: MainThread,
    private val wordRepository: WordRepository,
    private val text: String,
    private val languageTo: String = "en",
    private val callback: Callback
) : AbstractInteractor(executor, mainThread){

    override fun run() {
        wordRepository.getLanguageAndTranslation(
            text,
            languageTo,
            object : WordRepositorySource.GetTranslationCallback{
                override fun onTranslationAndLanguage(word: Words?) {
                    word?.let { callback.onTranslationAndLanguage(it) }
                }

                override fun onDataNotAvailable() {callback.onDataNotAvailable()}

            }
        )
    }

    interface Callback{
        fun onTranslationAndLanguage(word: Words) {}

        fun onDataNotAvailable() {}
    }
}