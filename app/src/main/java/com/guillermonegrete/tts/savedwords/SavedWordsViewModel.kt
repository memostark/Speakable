package com.guillermonegrete.tts.savedwords

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.guillermonegrete.tts.data.source.WordRepositorySource

import com.guillermonegrete.tts.db.Words
import kotlinx.coroutines.*

/**
 * Production dispatcher is always Dispatchers.IO, we only inject the dispatcher when testing
 * The official docs recommend to inject Dispatchers.Unconfined when testing code using withContext()
 */
class SavedWordsViewModel @ViewModelInject constructor(
    private val wordRepository: WordRepositorySource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    /**
     * Using LiveData as a stream instead of Coroutines Flow because the source contains Java files.
     * Not using RxJava either because this use case is simple enough for LiveData, in case of
     * increased complexity consider switching to RxJava.
     */
    val wordsList: LiveData<List<Words>> = liveData {
        emitSource(wordRepository.wordsStream)
    }

    val languagesList: LiveData<List<String>> = liveData(ioDispatcher) {
        emit(wordRepository.languagesISO)
    }

    fun insert(vararg words: Words) {
        wordRepository.insert(*words)
    }

    fun delete(word: String){
        viewModelScope.launch(ioDispatcher) {
            wordRepository.deleteWord(word)
        }
    }

    fun delete(vararg words: Words){
        viewModelScope.launch(ioDispatcher){
            wordRepository.delete(*words)
        }
    }
}
