package com.guillermonegrete.tts.importtext.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.utils.deleteAllFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WebLinksViewModel @Inject constructor(private val webLinkDAO: WebLinkDAO): ViewModel() {

    val uiState = webLinkDAO.getRecentLinks()
        .map { LoadResult.Success(it) as LoadResult<List<WebLink>> }
        .catch { emit(LoadResult.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadResult.Loading)

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
