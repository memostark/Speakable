package com.guillermonegrete.tts.common.notes

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guillermonegrete.tts.R

@Composable
fun NotesListScreen(notes: List<NoteItem>) {
    Surface {
        LazyColumn {
            items(notes) { note ->
                Column  {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column  {
                            Text(note.originalText)
                            Text(note.note)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Clear"
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(2.dp).fillMaxWidth().background(Color.Red)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItemMenu(onItemClick: (item: NoteMenuItem) -> Unit) {
    ModalBottomSheet(onDismissRequest = {}) {
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

@Preview
@Composable
fun NotesListScreenPreview() {
    NotesListScreen(listOf(NoteItem("First text", "First note"), NoteItem("Second text", "Second note")))
}

@Preview
@Composable
fun NoteItemMenuPreview() {
    NoteItemMenu {  }
}


data class NoteItem(
    val originalText: String,
    val note: String,
)

enum class NoteMenuItem {
    DELETE,
    GO_TO;
}