package com.guillermonegrete.tts.webreader

import androidx.annotation.ColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.ui.theme.BlueNoteHighlight
import com.guillermonegrete.tts.ui.theme.GreenNoteHighlight
import com.guillermonegrete.tts.ui.theme.RedNoteHighlight
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import kotlinx.coroutines.android.awaitFrame
import okhttp3.internal.toHexString


@Composable
fun WebReaderBottomBar(
    languages: MutableState<List<String>>,
    langSelection: MutableState<Int> = mutableStateOf(-1),
    iconsEnabled: MutableState<Boolean> = mutableStateOf(true),
    isPageSaved: MutableState<Boolean> = mutableStateOf(false),
    onTranslateClicked: () -> Unit = {},
    onArrowClicked: (isLeft: Boolean) -> Unit = {},
    onMenuItemClick: (index: Int) -> Unit = {},
    onPageVersionChanged: (String) -> Unit = {},
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
            val pageVersionStates = listOf("Local", "Web")
            var pageVersionSelection by remember { mutableStateOf(pageVersionStates.first()) }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Desc")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {

                val isSaved by isPageSaved
                DropdownMenuItem(onClick = {
                    onMenuItemClick(0)
                    menuExpanded = false
                }) {
                    val icon =
                        if (isSaved) R.drawable.ic_delete_black_24dp else R.drawable.baseline_save_24
                    Icon(painter = painterResource(icon), contentDescription = "Desc")
                    Spacer(modifier = Modifier.width(8.dp))
                    val text = stringResource(id = if (isSaved) R.string.delete else R.string.save)
                    Text(text)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSaved) {
                    DropdownMenuItem(onClick = {}) {
                        MultiToggleButton(pageVersionSelection, pageVersionStates) {
                            pageVersionSelection = it
                            menuExpanded = false
                            onPageVersionChanged(pageVersionSelection)
                        }
                    }
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
            onDismissRequest = { onDismiss() },
            title = { Text(text = stringResource(R.string.delete_page_dialog_title)) },
            text = { Text(stringResource(R.string.delete_page_dialog_body)) },
            buttons = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
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

const val NOTE_TEXT_TAG = "note text field"
const val ACCEPT_BTN_TAG = "add note accept btn"

val COLORS = listOf(YellowNoteHighlight, RedNoteHighlight, GreenNoteHighlight, BlueNoteHighlight)

@Composable
fun AddNoteDialog(
    isVisible: Boolean,
    noteText: String,
    @ColorInt noteColor: Int,
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSaveClicked: (result: AddNoteResult) -> Unit = {},
) {

    if (!isVisible) return
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {

                var text by remember { mutableStateOf(noteText) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.add_note_placeholder)) },
                    minLines = 4,
                    maxLines = 4,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .testTag(NOTE_TEXT_TAG)
                )

                val index = COLORS.indexOfFirst { noteColor == it.toArgb() }
                val indexColor = if (index == -1) 0 else index

                var colorSel by remember { mutableStateOf(indexColor) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    COLORS.forEachIndexed { index, color ->
                        val isSelected = index == colorSel
                        val modifier = if (isSelected) Modifier
                            .padding(3.dp) // margin
                            .border(3.dp, color, shape = CircleShape)
                            .padding(6.dp) // space between circle and ring, real size is this value minus the border size. 6dp - 3dp = 3dp
                            .size(36.dp) else Modifier.padding(12.dp).size(36.dp)

                        // use a box with constant size, otherwise the items move when changing selections
                        Box(Modifier.size(48.dp)) {
                            OutlinedButton(
                                onClick = { colorSel = index },
                                modifier = modifier.testTag(index.toString()),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(backgroundColor = color)
                            ) {}
                        }

                    }
                }

                Row {
                    Button(onClick = { onDelete() }, Modifier.weight(1f)) {
                        Text(stringResource(id = R.string.delete))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        onSaveClicked(AddNoteResult(text, COLORS[colorSel].toHex()))
                    },
                        Modifier
                            .weight(1f)
                            .testTag(ACCEPT_BTN_TAG)) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    // We only want to show the keyboard at start when the user is adding a new note.
    if (noteText.isNotEmpty()) return
    // Used to request focus which shows the keyboard
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}

data class AddNoteResult(val text: String, val colorHex: String)

fun Color.toHex() = "#${this.toArgb().toHexString()}"

@Composable
fun MultiToggleButton(
    currentSelection: String,
    toggleStates: List<String>,
    onToggleChange: (String) -> Unit
) {
    val selectedTint = MaterialTheme.colors.primary
    val unselectedTint = Color.Unspecified

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .border(BorderStroke(1.dp, Color.LightGray))
    ) {
        toggleStates.forEachIndexed { index, toggleState ->
            val isSelected = currentSelection.lowercase() == toggleState.lowercase()
            val backgroundTint = if (isSelected) selectedTint else unselectedTint
            val textColor = if (isSelected) Color.White else Color.Unspecified

            if (index != 0) {
                Divider(
                    color = Color.LightGray,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            }

            Row(
                modifier = Modifier
                    .background(backgroundTint)
                    .padding(vertical = 6.dp, horizontal = 8.dp)
                    .toggleable(
                        value = isSelected,
                        enabled = true,
                        onValueChange = { selected ->
                            if (selected) {
                                onToggleChange(toggleState)
                            }
                        })
            ) {
                Text(toggleState, color = textColor, modifier = Modifier.padding(4.dp))
            }

        }
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

@Preview
@Composable
fun AddNoteDialogPreview() {
    AppTheme {
        AddNoteDialog(true, "", 0)
    }
}

@Preview
@Composable
fun MultiToggleButtonPreview() {
    var selection by remember { mutableStateOf("Local") }
    AppTheme {
        MultiToggleButton(selection, listOf("Local", "Web")) { selection = it }
    }
}