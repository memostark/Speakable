package com.guillermonegrete.tts.importtext

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.guillermonegrete.tts.Event
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.importtext.visualize.io.EpubFileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
class ImportTextViewModel @ViewModelInject constructor(
    private val fileRepository: FileRepository,
    fileManager: EpubFileManager
): ViewModel() {

    private val _openTextVisualizer = MutableLiveData<Event<BookFile>>()
    val openTextVisualizer: LiveData<Event<BookFile>> = _openTextVisualizer

    private val _openItemMenu = MutableLiveData<Event<Int>>()
    val openItemMenu: LiveData<Event<Int>> = _openItemMenu

    private val _forceUpdate = ConflatedBroadcastChannel<Boolean>()

    val files = _forceUpdate.asFlow()
        .flatMapLatest {
            _dataLoading.value = true
            val files = fileRepository.getRecentFiles()
            _dataLoading.value = false
            files
        }
        .onCompletion { _dataLoading.value = false }
        .asLiveData()

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    val filesPath = fileManager.filesDir

    init {
        _forceUpdate.offer(true)
    }

    fun openVisualizer(book: BookFile){
        _openTextVisualizer.value = Event(book)
    }

    fun openItemMenu(itemPos: Int){
        _openItemMenu.value = Event(itemPos)
    }

    fun deleteFile(filePos: Int) = viewModelScope.launch {
        files.value?.let {
            val files = it.toMutableList()
            fileRepository.deleteFile(files[filePos])
        }
    }
}