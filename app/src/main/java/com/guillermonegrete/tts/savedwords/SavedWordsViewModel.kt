package com.guillermonegrete.tts.savedwords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.source.WordRepository

import com.guillermonegrete.tts.db.Words
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SavedWordsViewModel @Inject constructor(private val wordRepository: WordRepository) : ViewModel() {

    private val _wordsList = MutableLiveData<List<Words>>().apply { value = emptyList() }
    val wordsList: LiveData<List<Words>> = _wordsList

    private val _languagesList = MutableLiveData<List<String>>().apply { value = emptyList() }
    val languagesList: LiveData<List<String>> = _languagesList

    /**
     * This method purpose is to show how to use coroutines with a view model to get data.
     * When you don't have to modify the database query, a better way to implement this is wrapping
     * the result with LiveData, Room automatically makes the query asynchronous.
     */
    fun getWords(){
        viewModelScope.launch {
            val words = getAsyncWords()
            _wordsList.value = words
        }
    }

    /**
     * Similar to getWords but creates it's own scope instead of calling a suspend function
     */
    fun getLanguages(){
        viewModelScope.launch {
            val languages = withContext(Dispatchers.IO){
                return@withContext wordRepository.languagesISO
            }
            _languagesList.value = languages
        }
    }

    fun insert(vararg words: Words) {
        wordRepository.insert(*words)
    }

    private suspend fun getAsyncWords(): List<Words>{
        return withContext(Dispatchers.IO) {
            return@withContext wordRepository.words
        }
    }
}
