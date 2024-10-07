package com.guillermonegrete.tts.savedwords

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.LanguagesList
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.common.compose.YesNoDialog
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.ApplicationModule.WordsLocalDataSource
import com.guillermonegrete.tts.textprocessing.EditWordDialog
import com.guillermonegrete.tts.ui.theme.AppTheme
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
            val id = withContext(ioDispatcher) { wordSource.insertWord(word) }
            word.id = id
            _update.value = ResultType.Insert(word)
        }
    }

    fun update(newWord: Words){
        viewModelScope.launch {
            val rowsUpdated = withContext(ioDispatcher) { wordSource.update(newWord) }
            if(rowsUpdated > 0) _update.value = ResultType.Update(newWord)
        }
    }
}

sealed class ResultType {
    data class Insert(val word: Words): ResultType()
    data class Update(val word: Words): ResultType()
    data class Delete(val id: Int) : ResultType()
}

/**
 * This function provides a way to show the EditWordDialog from Java.
 */
@JvmOverloads
fun setContent(
    composeView: ComposeView,
    word: Words,
    isSaved: Boolean,
    editDialogVisible: MutableState<Boolean>,
    deleteDialogVisible: MutableState<Boolean>,
    onSave: (Words) -> Unit,
    onDelete: (String) -> Unit = {},
) {

    val context = composeView.context
    val languagesFull = context.resources.getStringArray(R.array.googleTranslateLanguagesArray).toList()
    val languagesISO = context.resources.getStringArray(R.array.googleTranslateLanguagesValue).toList()
    val languages = LanguagesList(languagesFull, languagesISO)

    composeView.setContent {
        AppTheme {
            var editDialogShown by remember { editDialogVisible }
            var deleteDialogShown by remember { deleteDialogVisible }

            EditWordDialog(
                isShown = editDialogShown,
                word = word.word,
                language = word.lang,
                translation = word.definition,
                notes = word.notes,
                languages = languages,
                isSaved = isSaved,
                onSave = { onSave(it.toWord()) },
                onDelete = { deleteDialogShown = true },
                onDismiss = { editDialogShown = false }
            )

            if (deleteDialogShown) {
                YesNoDialog(
                    onDismissRequest = { deleteDialogShown = false },
                    onConfirmation = { onDelete(word.word) },
                    dialogTitle = context.getString(R.string.delete_word_message),
                    dialogText = context.getString(R.string.delete_word_message),
                )
            }
        }
    }
}
