package com.guillermonegrete.tts.savedwords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.db.WordsDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SaveWordDialogViewModel @Inject constructor(private val wordsDAO: WordsDAO): ViewModel() {

    private val _update = MutableLiveData<ResultType>()
    val update: LiveData<ResultType> = _update

    fun save(word: Words){

        if (word.word.isEmpty() || word.lang.isEmpty() || word.definition.isEmpty())
            return

        viewModelScope.launch {
            withContext(Dispatchers.IO) { wordsDAO.insert(word) }
            _update.value = ResultType.Insert(word)
        }
    }

    fun update(newWord: Words){
        viewModelScope.launch {
            val word = withContext(Dispatchers.IO) { wordsDAO.findWord(newWord.word) }

            word ?: return@launch

            word.definition = newWord.definition
            word.notes = newWord.notes
            withContext(Dispatchers.IO) { wordsDAO.update(word) }
            _update.value = ResultType.Update
        }
    }
}

sealed class ResultType {
    class Insert(val word: Words): ResultType()
    object Update : ResultType()
}
