package com.guillermonegrete.tts.importtext.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun addNew(link: WebLink){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                webLinkDAO.upsert(link)
            }
        }
    }
}
