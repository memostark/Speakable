package com.guillermonegrete.tts.webreader

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.theme.AppTheme


@Composable
fun WebReaderBottomBar(
    languages: MutableState<List<String>>,
    langSelection: MutableState<Int> = mutableStateOf(-1),
    iconsEnabled: MutableState<Boolean> = mutableStateOf(true),
    isPageSaved: MutableState<Boolean> = mutableStateOf(false),
    onTranslateClicked: () -> Unit = {},
    onArrowClicked: (isLeft: Boolean) -> Unit = {},
    onMenuItemClick: (index: Int) -> Unit = {},
    onLangSelected: (Int, String) -> Unit = { _, _ -> },
) {
    val iconsState by iconsEnabled

    BottomAppBar(modifier = Modifier.testTag("web_reader_bar")) {
        IconButton(
            onClick = { onTranslateClicked() },
            enabled = iconsState,
            modifier = Modifier.alpha(if (iconsState) 1f else 0f),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_translate_black_24dp),
                contentDescription = stringResource(R.string.translate_description)
            )
        }
        IconButton(
            onClick = { onArrowClicked(true) },
            enabled = iconsState,
            modifier = Modifier.alpha(if (iconsState) 1f else 0f),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_new_24),
                contentDescription = stringResource(R.string.previous_selection)
            )
        }
        IconButton(
            onClick = { onArrowClicked(false) },
            enabled = iconsState,
            modifier = Modifier.alpha(if (iconsState) 1f else 0f),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_arrow_forward_ios_24),
                contentDescription = stringResource(R.string.next_selection)
            )
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Spinner(languages, langSelection, onLangSelected)
        }

        Box {
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Desc")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {

                DropdownMenuItem(onClick = {
                    onMenuItemClick(0)
                    menuExpanded = false
                }) {
                    val isSaved by isPageSaved
                    val icon =
                        if (isSaved) R.drawable.ic_delete_black_24dp else R.drawable.baseline_save_24
                    Icon(painter = painterResource(icon), contentDescription = "Desc")
                    Spacer(modifier = Modifier.width(8.dp))
                    val text = stringResource(id = if (isSaved) R.string.delete else R.string.save)
                    Text(text)
                }
            }
        }
    }
}

@Composable
fun Spinner(
    items: MutableState<List<String>>,
    preselected: MutableState<Int> = mutableStateOf(-1),
    onItemSelected: (Int, String) -> Unit = { _, _ -> }
) {
    val spinnerItems by items
    var selected by preselected
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = !expanded }) {
            Text(text = spinnerItems.getOrNull(selected) ?: "")
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(stringResource(R.string.web_reader_lang_spinner_prompt))

            spinnerItems.forEachIndexed { index, item ->
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

@Composable
fun LoadingDialog(isVisible: Boolean) {
    if (isVisible) {
        Dialog(onDismissRequest = {}) {
            Card {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                    Text(
                        stringResource(R.string.saving_page_dialog),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeletePageDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit = {},
    okClicked: () -> Unit = {},
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest =  { onDismiss() },
            title = { Text(text = stringResource(R.string.delete_page_dialog_title)) },
            text = { Text(stringResource(R.string.delete_page_dialog_body)) },
            buttons = {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { onDismiss() }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { okClicked() }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        )
    }
}

private val suggestions = mutableStateOf(listOf("Item1", "Item2", "Item3"))

@Preview
@Composable
fun BarPreview() {
    AppTheme {
        WebReaderBottomBar(remember { suggestions })
    }
}

@Preview
@Composable
fun SpinnerPreview() {
    AppTheme {
        Column {
            Spinner(remember { suggestions })
            Spinner(remember { suggestions }, remember { mutableStateOf(0) })
        }
    }
}

@Preview
@Composable
fun LoadingDialogPreview() {
    AppTheme {
        LoadingDialog(true)
    }
}

@Preview
@Composable
fun DeletePageDialogPreview() {
    AppTheme {
        DeletePageDialog(true)
    }
}