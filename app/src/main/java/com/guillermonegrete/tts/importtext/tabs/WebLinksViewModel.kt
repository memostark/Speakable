package com.guillermonegrete.tts.importtext.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.utils.deleteAllFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WebLinksViewModel @Inject constructor(private val webLinkDAO: WebLinkDAO): ViewModel() {

    private val _uiState = MutableStateFlow<LoadResult<List<WebLink>>>(LoadResult.Success(emptyList()))
    val uiState: StateFlow<LoadResult<List<WebLink>>> = _uiState

    fun getRecentLinks(){

        _uiState.value = LoadResult.Loading

        viewModelScope.launch {
            webLinkDAO.getRecentLinks().collect {
                _uiState.value = LoadResult.Success(it)
            }
        }

    }

    fun delete(link: WebLink, rootFolder: String){
        viewModelScope.launch {
            deleteLinkFolder(link, rootFolder)
            webLinkDAO.delete(link)
        }
    }

    private fun deleteLinkFolder(link: WebLink, rootPath: String) {
        val uuid = link.uuid ?: return

        deleteAllFolder(File(rootPath, uuid.toString()))
    }
}
