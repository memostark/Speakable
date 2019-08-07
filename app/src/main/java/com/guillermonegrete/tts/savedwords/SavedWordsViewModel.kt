package com.guillermonegrete.tts.savedwords

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.db.WordsDAO
import javax.inject.Inject

class SavedWordsViewModel @Inject constructor(private val wordsDAO: WordsDAO) : ViewModel() {

    val wordsList: LiveData<List<Words>> = wordsDAO.allWords
    val languagesList: LiveData<List<String>> = wordsDAO.languagesISO

    fun insert(vararg words: Words) {
        wordsDAO.insert(*words)
    }

    fun update(word: Words) {
        wordsDAO.update(word)
    }

    fun deleteAll() {
        wordsDAO.deleteAll()
    }
}
