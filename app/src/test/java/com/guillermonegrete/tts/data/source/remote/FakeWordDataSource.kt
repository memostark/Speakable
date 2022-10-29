package com.guillermonegrete.tts.data.source.remote

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import java.lang.Exception
import java.util.LinkedHashMap

class FakeWordDataSource: WordDataSource {

    var translationsData: LinkedHashMap<String, Words> = LinkedHashMap()

    override fun getWords(): MutableList<Words> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWordsStream(): LiveData<MutableList<Words>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLanguagesISO(): MutableList<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun insertWords(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteWords(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWordLanguageInfo(
        wordText: String?,
        languageFrom: String?,
        languageTo: String?,
        callback: WordDataSource.GetWordCallback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWordLanguageInfo(
        wordText: String,
        languageFrom: String,
        languageTo: String
    ): Words {
        return translationsData[wordText] ?: throw Exception("Not found")
    }

    override fun loadWord(word: String?, language: String?): LiveData<Words> {
        TODO("Not yet implemented")
    }

    @VisibleForTesting
    fun addTranslation(vararg words: Words) {
        for (word in words) {
            translationsData[word.word] = word
        }
    }
}