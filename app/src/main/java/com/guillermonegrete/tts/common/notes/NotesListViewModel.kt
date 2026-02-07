package com.guillermonegrete.tts.common.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = NotesListViewModel.Factory::class)
class NotesListViewModel @AssistedInject constructor(
    @Assisted id: Int,
    notesDAO: NoteDAO,
): ViewModel() {

    val uiState = notesDAO.getNotesFlow(id)
        .map { LoadResult.Success(it) as LoadResult<List<Note>> }
        .catch { emit(LoadResult.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadResult.Loading)

    @AssistedFactory
    interface Factory {
        fun create(id: Int): NotesListViewModel
    }
}
