package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.db.Words
import java.lang.Exception
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeWordRepository @Inject constructor(): WordRepositorySource {

    var wordsServiceData: LinkedHashMap<String, Words> = LinkedHashMap()
    var remoteWordServiceData: LinkedHashMap<String, Words> = LinkedHashMap()

    var translationsWordData: LinkedHashMap<String, Words> = LinkedHashMap()
    var translationsData: LinkedHashMap<String, Translation> = LinkedHashMap()

    var languagesData: MutableSet<String> = mutableSetOf()

    override fun getWords(): MutableList<Words> {
        return wordsServiceData.values.toMutableList()
    }

    override fun getWordsStream(): LiveData<MutableList<Words>> {
        return MutableLiveData(wordsServiceData.values.toMutableList())
    }

    override fun getLocalWord(word: String, language: String): LiveData<Words> {
        TODO("Not yet implemented")
    }

    override fun getLanguagesISO(): MutableList<String> {
        return languagesData.toMutableList()
    }

    override fun getWordLanguageInfo(
        wordText: String,
        languageFrom: String,
        languageTo: String,
        callback: WordRepositorySource.GetWordRepositoryCallback
    ) {

        val localWord = wordsServiceData[wordText]
        if(localWord != null){
            callback.onLocalWordLoaded(localWord)
        } else {
            callback.onLocalWordNotAvailable()

            val remoteWord = remoteWordServiceData[wordText]
            if(remoteWord != null) callback.onRemoteWordLoaded(remoteWord)
            else callback.onDataNotAvailable(Words(wordText, "un", "un"))
        }
    }

    override fun getLanguageAndTranslation(text: String, callback: WordRepositorySource.GetTranslationCallback) {
        getLanguageAndTranslation(text, "auto", "en", callback)
    }

    override fun getLanguageAndTranslation(
        text: String,
        languageFrom: String,
        languageTo: String,
        callback: WordRepositorySource.GetTranslationCallback
    ) {
        val translation = translationsWordData[text]
        if(translation != null) callback.onTranslationAndLanguage(translation)
        else callback.onDataNotAvailable()
    }

    override fun getTranslation(
        text: String,
        languageFrom: String,
        languageTo: String
    ): Result<Translation> {
        val word = translationsData[text] ?: return Result.Error(Exception("Translation not found"))
        return Result.Success(word)
    }

    override fun deleteWord(word: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteWord(word: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun insert(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @VisibleForTesting
    fun addWords(vararg words: Words) {
        for (word in words) {
            wordsServiceData[word.word] = word
            languagesData.add(word.lang)
        }
    }

    @VisibleForTesting
    fun addRemoteWords(vararg words: Words) {
        for (word in words) {
            remoteWordServiceData[word.word] = word
        }
    }

    @VisibleForTesting
    fun addTranslation(vararg words: Words) {
        for (word in words) {
            translationsWordData[word.word] = word
        }
    }

    @VisibleForTesting
    fun addTranslation(vararg translations: Translation) {
        for (translation in translations) {
            translationsData[translation.sentences.first().orig] = translation
        }
    }
}