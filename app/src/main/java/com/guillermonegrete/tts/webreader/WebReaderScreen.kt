package com.guillermonegrete.tts.webreader

import androidx.annotation.ColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowWidthSizeClass
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.Spinner
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.common.compose.YesNoDialog
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.ui.theme.BlueNoteHighlight
import com.guillermonegrete.tts.ui.theme.GreenNoteHighlight
import com.guillermonegrete.tts.ui.theme.RedNoteHighlight
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import okhttp3.internal.toHexString


@Composable
fun WebReaderBottomBar(
    languages: StringList,
    langSelection: MutableState<Int> = mutableIntStateOf(-1),
    iconsEnabled: MutableState<Boolean> = mutableStateOf(true),
    isPageSaved: MutableState<Boolean> = mutableStateOf(false),
    wordsShown: Boolean = false,
    getPageVersion: () -> String = {""},
    onTranslateClicked: () -> Unit = {},
    onArrowClicked: (isLeft: Boolean) -> Unit = {},
    onMenuItemClick: (action: WebReaderMenuAction) -> Unit = {},
    onLangSelected: (Int, String) -> Unit = { _, _ -> },
) {
    val iconsState by remember { iconsEnabled }

    BottomAppBar(
        modifier = Modifier
            .testTag("web_reader_bar")
            .height(WebReaderBarHeight),
        contentPadding = PaddingValues(vertical = 8.dp), // Default is 12dp which causes asymmetry with height 64 dp
        windowInsets = WindowInsets(0, 0, 0, 0), // The default insets take too much space when E2E, the insets are handled in the parent view of this bar
    ) {
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

        Spacer(Modifier.weight(1f))

        Spinner(languages, preselected = langSelection.value, onItemSelected = onLangSelected)

        WebReaderBarMenu(isPageSaved, wordsShown, getPageVersion, false, onMenuItemClick)
    }
}

val pageVersionStates = mapOf(PageVersion.LOCAL to "Local", PageVersion.WEB to "Web")
val pageVersionLabels = pageVersionStates.values.toList()

@Composable
fun WebReaderBarMenu(
    isPageSaved: MutableState<Boolean>,
    wordsShown: Boolean,
    getPageVersion: () -> String,
    menuExpanded: Boolean = false,
    onMenuItemClick: (action: WebReaderMenuAction) -> Unit,
) {
    Box {
        var menuExpanded by rememberSaveable { mutableStateOf(menuExpanded) }

        var checked by remember { mutableStateOf(wordsShown) }

        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Desc")
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {

            val isSaved by isPageSaved
            DropdownMenuItem(
                text = { Text(stringResource(if (isSaved) R.string.delete else R.string.save)) },
                onClick = {
                    onMenuItemClick(WebReaderMenuAction.PageStatus)
                    menuExpanded = false
                },
                leadingIcon = {
                    val icon = if (isSaved) R.drawable.ic_delete_black_24dp else R.drawable.baseline_save_24
                    Icon(painter = painterResource(icon), contentDescription = if (isSaved) "Delete" else "Save")
                },
            )

            if (isSaved) {
                DropdownMenuItem(
                    text = {
                        MultiToggleButton(getPageVersion(), StringList(pageVersionLabels)) { state ->
                            menuExpanded = false
                            val version = pageVersionStates.entries.first { it.value == state }.key
                            onMenuItemClick(WebReaderMenuAction.PageVersionToggle(version))
                        }
                    },
                    onClick = {},
                )

                DropdownMenuItem(
                    text = { Text(stringResource( R.string.show_notes_list)) },
                    onClick = {
                        onMenuItemClick(WebReaderMenuAction.OpenNotesList)
                        menuExpanded = false
                    },
                )
            }

            DropdownMenuItem(
                text = { Text(stringResource(R.string.web_reader_words_action)) },
                onClick = {},
                trailingIcon = {
                    Switch(
                        checked,
                        onCheckedChange = {
                            checked = it
                            onMenuItemClick(WebReaderMenuAction.ShowWords(it))
                        }
                    )
                }
            )

            DropdownMenuItem(
                leadingIcon = { Icon(painterResource(R.drawable.baseline_link_24), stringResource(R.string.link_description)) },
                text = { Text(stringResource(R.string.copy_link)) },
                onClick = { onMenuItemClick(WebReaderMenuAction.CopyLink) },
            )
        }
    }
}

sealed interface WebReaderMenuAction {
    data object PageStatus : WebReaderMenuAction
    data class PageVersionToggle(val version: PageVersion): WebReaderMenuAction
    data class ShowWords(val shown: Boolean): WebReaderMenuAction
    data object CopyLink: WebReaderMenuAction
    data object OpenNotesList: WebReaderMenuAction
}

val WebReaderBarHeight = 64.dp // Default bar height is 80dp, looks too big for this case

@Composable
fun LoadingDialog(isVisible: Boolean) {
    if (!isVisible) return

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

@Composable
fun DeletePageDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit = {},
    okClicked: () -> Unit = {},
) {
    if (isOpen) {
        YesNoDialog(
            onDismiss,
            okClicked,
            stringResource(R.string.delete_page_dialog_title),
            stringResource(R.string.delete_page_dialog_body),
        )
    }
}

const val NOTE_TEXT_TAG = "note text field"
const val ACCEPT_BTN_TAG = "add note accept btn"
const val DELETE_BTN_TAG = "add note delete btn"

val COLORS = listOf(YellowNoteHighlight, RedNoteHighlight, GreenNoteHighlight, BlueNoteHighlight)

@Composable
fun AddNoteDialog(
    isVisible: Boolean,
    noteText: String,
    @ColorInt noteColor: Int,
    noteSaved: Boolean = false,
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSaveClicked: (result: AddNoteResult) -> Unit = {},
) {

    if (!isVisible) return

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {

                val text = remember { mutableStateOf(noteText) }
                NoteText(noteText, modifier = Modifier.fillMaxWidth()) { text.value = it }

                val index = COLORS.indexOfFirst { noteColor == it.toArgb() }
                val indexColor = if (index == -1) 0 else index
                val colorSel = remember { mutableIntStateOf(indexColor) }

                ColorsRow(colorSel, Modifier.fillMaxWidth().padding(vertical = 8.dp))

                Row {
                    if(noteSaved) {
                        Button(onClick = { onDelete() },
                            Modifier
                                .weight(1f)
                                .testTag(DELETE_BTN_TAG)) {
                            Text(stringResource(id = R.string.delete))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Button(
                        onClick = { onSaveClicked(AddNoteResult(text.value, COLORS[colorSel.intValue].toHex())) },
                        Modifier
                            .weight(1f)
                            .testTag(ACCEPT_BTN_TAG)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun AddNoteDialogMedium(
    isVisible: Boolean,
    noteText: String,
    @ColorInt noteColor: Int,
    noteSaved: Boolean = false,
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSaveClicked: (result: AddNoteResult) -> Unit = {},
) {

    if (!isVisible) return

    Dialog(
        onDismissRequest = { onDismiss() },
        DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {

                val text = remember { mutableStateOf(noteText) }
                NoteText(noteText, modifier = Modifier.width(200.dp)) { text.value = it }

                Spacer(Modifier.width(8.dp))

                Column(Modifier.width(IntrinsicSize.Min)) {
                    val index = COLORS.indexOfFirst { noteColor == it.toArgb() }
                    val indexColor = if (index == -1) 0 else index
                    val colorSel = remember { mutableIntStateOf(indexColor) }

                    ColorsRow(colorSel, Modifier)

                    Spacer(Modifier.height(8.dp))

                    Row {
                        if(noteSaved) {
                            Button(
                                onClick = onDelete,
                                Modifier.weight(1f).testTag(DELETE_BTN_TAG)
                            ) {
                                Text(stringResource(id = R.string.delete))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Button(
                            onClick = { onSaveClicked(AddNoteResult(text.value, COLORS[colorSel.intValue].toHex())) },
                            Modifier.weight(1f).testTag(ACCEPT_BTN_TAG)
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddNoteDialog(
    state: AddNoteDialogUI?,
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSaveClicked: (result: AddNoteResult) -> Unit = {},
) {

    if (state == null) return

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    
    if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
        AddNoteDialog(true, state.noteText, state.noteColor, state.noteSaved, onDismiss, onDelete, onSaveClicked)
    } else {
        AddNoteDialogMedium(true, state.noteText, state.noteColor, state.noteSaved, onDismiss, onDelete, onSaveClicked)
    }
}

data class AddNoteDialogUI(
    val noteText: String,
    @ColorInt val noteColor: Int,
    val noteSaved: Boolean = false,
)

@Composable
fun NoteText(noteText: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    var textFieldLoaded by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(noteText, TextRange(noteText.length))) }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onValueChange(it.text)
        },
        placeholder = { Text(stringResource(R.string.add_note_placeholder)) },
        minLines = 4,
        maxLines = 4,
        modifier = modifier
            .focusRequester(focusRequester)
            .onGloballyPositioned {
                if (!textFieldLoaded) {
                    focusRequester.requestFocus()
                    textFieldLoaded = true // stop cyclic recompositions
                }
            }
            .testTag(NOTE_TEXT_TAG)
    )
}

@Composable
fun ColorsRow(colorSel: MutableIntState, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        COLORS.forEachIndexed { index, color ->
            CircleColorButton(color, index, colorSel)
        }
    }
}

@Composable
fun CircleColorButton(color: Color, index: Int, colorSel: MutableIntState) {
    val isSelected = index == colorSel.intValue
    val modifier = if (isSelected) Modifier
        .padding(3.dp) // margin
        .border(3.dp, color, shape = CircleShape)
        .padding(6.dp) // space between circle and ring, real size is this value minus the border size. 6dp - 3dp = 3dp
        .size(36.dp)
    else Modifier
        .padding(12.dp)
        .size(36.dp)

    // use a box with constant size, otherwise the items move when changing selections
    Box(Modifier.size(48.dp)) {
        OutlinedButton(
            onClick = { colorSel.intValue = index },
            modifier = modifier.semantics{
                contentDescription = color.toString()
            }.testTag(index.toString()),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
        ) {}
    }
}

data class AddNoteResult(val text: String, val colorHex: String)

fun Color.toHex() = "#${this.toArgb().toHexString()}"

@Composable
fun MultiToggleButton(
    currentSelection: String,
    toggleStates: StringList,
    onToggleChange: (String) -> Unit
) {
    val selectedTint = MaterialTheme.colorScheme.primary
    val unselectedTint = Color.Unspecified

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .border(BorderStroke(1.dp, Color.LightGray))
    ) {
        toggleStates.items.forEachIndexed { index, toggleState ->
            val isSelected = currentSelection.equals(toggleState, ignoreCase = true)
            val backgroundTint = if (isSelected) selectedTint else unselectedTint
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified

            if (index != 0) {
                VerticalDivider(
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

private val suggestions = StringList(listOf("Item1", "Item2", "Item3"))

@Preview
@Composable
fun BarPreview() {
    AppTheme {
        WebReaderBottomBar(suggestions)
    }
}

@Preview
@Composable
fun WebReaderBarMenuPreview() {
    AppTheme {
        Surface {
            WebReaderBarMenu(remember { mutableStateOf(true) }, true, { "Local" }, true) { }
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

@PreviewScreenSizes
@Composable
fun AddNoteDialogPreview() {
    AppTheme {
        val state = AddNoteDialogUI("", 0, true)
        AddNoteDialog(state)
    }
}

@Preview
@Composable
fun MultiToggleButtonPreview() {
    var selection by remember { mutableStateOf("Local") }
    AppTheme {
        Surface {
            MultiToggleButton(selection, StringList(listOf("Local", "Web"))) { selection = it }
        }
    }
}