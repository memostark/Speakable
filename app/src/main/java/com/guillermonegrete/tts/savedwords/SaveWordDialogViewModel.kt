package com.guillermonegrete.tts.savedwords

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.ApplicationModule.WordsLocalDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SaveWordDialogViewModel @Inject constructor(
    @WordsLocalDataSource private val wordSource: WordDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): ViewModel() {

    private val _update = MutableLiveData<ResultType>()
    val update: LiveData<ResultType> = _update

    fun save(word: Words){

        if (word.word.isEmpty() || word.lang.isEmpty() || word.definition.isEmpty())
            return

        viewModelScope.launch {
            withContext(ioDispatcher) { wordSource.insertWords(word) }
            _update.value = ResultType.Insert(word)
        }
    }

    fun update(newWord: Words){
        viewModelScope.launch {
            val rowsUpdated = withContext(ioDispatcher) { wordSource.update(newWord) }
            if(rowsUpdated > 0) _update.value = ResultType.Update
        }
    }
}

sealed class ResultType {
    data class Insert(val word: Words): ResultType()
    object Update : ResultType()
}
