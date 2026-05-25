package com.guillermonegrete.tts.importtext

import androidx.lifecycle.*
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.BookUriUpdate
import com.guillermonegrete.tts.importtext.visualize.io.EpubFileManager
import com.guillermonegrete.tts.utils.deleteAllFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    fileManager: EpubFileManager
): ViewModel() {

    val files = fileRepository.getRecentFiles()
        .map { LoadResult.Success(it) as LoadResult<List<BookFile>> }
        .catch { emit(LoadResult.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadResult.Loading)

    private val _updateFile = MutableSharedFlow<BookUriUpdate>()
    val updateFile: SharedFlow<BookUriUpdate> = _updateFile

    val filesPath = fileManager.filesDir

    fun deleteFile(file: BookFile) {
        viewModelScope.launch {
            fileRepository.deleteFile(file)
            val folder = File(filesPath, file.folderPath)
            deleteAllFolder(folder)
        }
    }

    fun updateUri(uri: String, fileId: Int) {
        viewModelScope.launch {
            val update = BookUriUpdate(fileId, uri)
            fileRepository.update(update)
            _updateFile.emit(update)
        }
    }
}
