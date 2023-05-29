package com.guillermonegrete.tts.importtext

import androidx.lifecycle.*
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.importtext.visualize.io.EpubFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    fileManager: EpubFileManager
): ViewModel() {

    private val _files = MutableStateFlow<LoadResult<List<BookFile>>>(LoadResult.Success(emptyList()))
    val files: StateFlow<LoadResult<List<BookFile>>> = _files

    val filesPath = fileManager.filesDir

    fun loadFiles() {
        _files.value = LoadResult.Loading

        viewModelScope.launch {
            fileRepository.getRecentFiles().collect {
                _files.value = LoadResult.Success(it)
            }
        }
    }

    fun deleteFile(filePos: Int) {
        files.value.let {
            if (it !is LoadResult.Success) return
            val files = it.data
            viewModelScope.launch {
                fileRepository.deleteFile(files[filePos])
            }
        }
    }
}