package com.guillermonegrete.tts.common.notes

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import androidx.core.graphics.toColorInt
import com.guillermonegrete.tts.common.compose.YesNoDialog
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun NotesListScreen(
    notes: List<NoteItem>,
    onMenuAction: (item: NoteMenuItem, note: NoteItem) -> Unit = { _, _ -> },
) {

    var selectedNote by remember { mutableStateOf<NoteItem?>(null) }

    Surface (modifier = Modifier.statusBarsPadding()) {
        LazyColumn {
            items(
                notes,
                key = { it.id }
            ) { note ->
                Column  {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column (Modifier.padding(8.dp))  {
                            Text(note.originalText, fontWeight = FontWeight.Bold)
                            Text(note.note)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { selectedNote = note }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(Color(note.color))
                    )
                }
            }
        }
    }

    selectedNote?.let { note ->
        NoteItemMenu(onDismiss = { selectedNote = null }) {
            onMenuAction(it, note)
            selectedNote = null
        }
    }
}

@Composable
fun NotesListScreen(viewModel: NotesListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is LoadResult.Success) {
        val notes = (uiState as LoadResult.Success).data.map {
            NoteItem(it.id, it.originalText, it.text, it.color.toColorInt())
        }
        
        var deleteDialogShown by rememberSaveable { mutableStateOf<NoteItem?>(null) }
        NotesListScreen(notes) { menuAction, note ->
            when(menuAction) {
                NoteMenuItem.DELETE -> deleteDialogShown = note
                NoteMenuItem.GO_TO -> {}
            }
        }

        deleteDialogShown?.let { note ->
            YesNoDialog(
                onDismissRequest = { deleteDialogShown = null },
                onConfirmation = {
                    viewModel.deleteNote(note.id)
                    deleteDialogShown = null
                },
                dialogTitle = stringResource(R.string.delete_item),
                dialogText = null
            )
        }

        SnackBarError(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItemMenu(
    onDismiss: () -> Unit,
    onItemClick: (item: NoteMenuItem) -> Unit = {},
) {
    ModalBottomSheet(onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            val goToDesc = stringResource(R.string.go_to_text)
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, goToDesc) },
                text = { Text(goToDesc) },
                onClick = { onItemClick(NoteMenuItem.GO_TO) }
            )

            val deleteDesc = stringResource(R.string.delete)
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Filled.Delete, deleteDesc) },
                text = { Text(deleteDesc) },
                onClick = { onItemClick(NoteMenuItem.DELETE) }
            )
        }
    }
}

@Composable
fun SnackBarError(viewModel: NotesListViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val error by viewModel.errorMessage.collectAsState()

    SnackbarHost(
        snackbarHostState,
        Modifier.statusBarsPadding(),
    )

    LaunchedEffect(error) {
        error?.let { e ->
            Timber.e(e)
            scope.launch {
                snackbarHostState.showSnackbar(e.message ?: "")
                viewModel.clearErrorMessage()
            }
        }
    }
}

@Preview
@Composable
fun NotesListScreenPreview() {
    NotesListScreen(dummyNotes)
}

@Preview
@Composable
fun NoteItemMenuPreview() {
    NoteItemMenu ({}) {  }
}

val dummyNotes = listOf(
    NoteItem(0, "Dummy text", "Dummy note text", 0xFF0000FF.toInt()),
    NoteItem(1, "Another dummy text", "More dummy note text", 0xFF00FF00.toInt()),
)

data class NoteItem(
    val id: Long,
    val originalText: String,
    val note: String,
    @param:ColorInt val color: Int,
)

enum class NoteMenuItem {
    DELETE,
    GO_TO;
}
