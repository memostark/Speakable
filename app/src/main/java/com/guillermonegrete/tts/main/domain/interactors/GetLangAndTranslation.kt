package com.guillermonegrete.tts.main.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.db.Words
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class GetLangAndTranslation @Inject constructor(
    executor: ExecutorService,
    mainThread: MainThread,
    private val wordRepository: WordRepositorySource
) : AbstractInteractor(executor, mainThread){

    private var text = ""
    private var languageFrom = ""
    private var languageTo = ""
    private var callback: Callback? = null

    /**
     * This function is deprecated, avoid usage for new features.
     * Only use when Kotlin Coroutines or RxJava are not options.
     */
    @Deprecated("Avoid usage for new features. Only use when Kotlin Coroutines or RxJava are not options.")
    @JvmOverloads
    operator fun invoke(
        text: String,
        callback: Callback,
        languageFrom: String = "auto",
        languageTo: String = "en"
    ){
        this.text = text
        this.languageFrom = languageFrom
        this.languageTo = languageTo
        this.callback = callback
        run()
    }

    operator fun invoke(
        text: String,
        languageFrom: String = "auto",
        languageTo: String = "en"
    ): Result<Translation>{
        return wordRepository.getTranslation(text, languageFrom, languageTo)
    }

    override fun run() {
        wordRepository.getLanguageAndTranslation(
            text,
            languageFrom,
            languageTo,
            object : WordRepositorySource.GetTranslationCallback{
                override fun onTranslationAndLanguage(word: Words?) {
                    word?.let { callback?.onTranslationAndLanguage(it) }
                }

                override fun onDataNotAvailable() {callback?.onDataNotAvailable()}

            }
        )
    }

    interface Callback{
        fun onTranslationAndLanguage(word: Words)

        fun onDataNotAvailable()
    }
}