package com.guillermonegrete.tts.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.theme.AppTheme

@Composable
fun YesNoDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun YesNoDialogPreview() {
    AppTheme {
        YesNoDialog(
            onDismissRequest = { },
            onConfirmation = { },
            dialogTitle = "My title",
            dialogText = "Do you want to do action?",
        )
    }
}

@Composable
fun Spinner(
    list: StringList,
    preselected: Int = -1,
    displayText: String? = null,
    onItemSelected: (Int, String) -> Unit = { _, _ -> }
) {
    var selected by remember(preselected) { mutableIntStateOf(preselected) }
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(8.dp, end = 0.dp),
        ) {
            Text(text = displayText ?: list.items.getOrNull(selected) ?: "")
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(stringResource(R.string.web_reader_lang_spinner_prompt), Modifier.padding(8.dp))

            // In order to use LazyColumn we have to put it inside a Box
            // The Box's size has to be explicitly defined otherwise it will crash (fillMaxWidth() crashes)
            // See related issue: https://issuetracker.google.com/issues/242398344
            Box(modifier = Modifier.size(width = 200.dp, height = 500.dp)) {
                LazyColumn {
                    itemsIndexed(list.items) { index, item ->
                        DropdownMenuItem(onClick = {
                            expanded = false
                            selected = index
                            onItemSelected(index, item)
                        }) {
                            Text(text = item)
                        }
                    }
                }
            }
        }
    }
}
