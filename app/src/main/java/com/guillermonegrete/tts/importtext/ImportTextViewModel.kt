package com.guillermonegrete.tts.importtext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.Event
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import kotlinx.coroutines.launch
import javax.inject.Inject

class ImportTextViewModel @Inject constructor(private val fileRepository: FileRepository): ViewModel() {

    private val _openTextVisualizer = MutableLiveData<Event<BookFile>>()
    val openTextVisualizer: LiveData<Event<BookFile>> = _openTextVisualizer

    private val _files = MutableLiveData<List<BookFile>>()
    val files: LiveData<List<BookFile>> = _files

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    fun loadRecentFiles(){
        _dataLoading.value = true

        viewModelScope.launch {
            val files = fileRepository.getRecentFiles()
            _files.value = files

            _dataLoading.value = false
        }

    }

    fun openVisualizer(book: BookFile){
        _openTextVisualizer.value = Event(book)
    }
}