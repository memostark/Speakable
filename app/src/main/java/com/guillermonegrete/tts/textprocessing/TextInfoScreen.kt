package com.guillermonegrete.tts.textprocessing

import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.LanguagesList
import com.guillermonegrete.tts.common.compose.Spinner
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.WordUI
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SentenceDialog(
    isVisible: Boolean,
    text: String,
    translation: MutableState<String>,
    languagesFrom: StringList,
    languagesTo: StringList,
    targetLangIndex: Int,
    playIconState: MutableState<PlayIconState> = mutableStateOf(PlayIconState()),
    sourceLangIndex: Int = 0,
    detectedLanguageState: MutableIntState = mutableIntStateOf(-1),
    highlightedSpanState: MutableState<SplitPageSpan?> = mutableStateOf(null),
    wordState: MutableState<WordState?> = mutableStateOf(null),
    onPlayButtonClick: () -> Unit = {},
    onTopTextClick: (Int) -> Unit = {},
    onBottomTextClick: (Int) -> Unit = {},
    onBookmarkClicked: () -> Unit = {},
    onMoreInfoClicked: () -> Unit = {},
    onSourceLangChanged: (Int) -> Unit = {},
    onTargetLangChanged: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (!isVisible) return

    Dialog(onDismissRequest = { onDismiss() }) {

        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        val window = dialogWindowProvider.window
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val wlp = window.attributes
        val initialY = wlp.y
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp

        val density = LocalDensity.current
        val swipeableState = remember {
            AnchoredDraggableState(
                SwipeDirection.Initial,
                { distance -> distance * 0.6f },
                { with(density) { 125.dp.toPx() }},
                tween()
            )
        }

        Card(
            modifier = Modifier
                .padding(16.dp)
                .onSizeChanged {
                    val sizePx = it.width.toFloat()
                    swipeableState.updateAnchors(
                        DraggableAnchors { SwipeDirection.Initial at 0f; SwipeDirection.Right at sizePx; SwipeDirection.Left at -sizePx }
                    )
                }
                .anchoredDraggable(swipeableState, Orientation.Horizontal)
                .pointerInput(Unit) {
                    // Because swipeable can only handle one axis at the time we use gestures for the vertical axis
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            // Because we are using Bottom gravity the axis sign is inverted
                            wlp.y -= dragAmount.toInt()
                            window.attributes = wlp
                        },
                        onDragEnd = {
                            wlp.y = initialY
                            window.attributes = wlp
                        }
                    )
                }
                .offset { IntOffset(swipeableState.requireOffset().roundToInt(), 0) }
                .testTag("sentence_dialog"),
            elevation = 8.dp
        ) {
            // For whatever reason the ClickableText doesn't use the same style as the Text composable, this causes problems with dark mode
            // This is similar to how Text creates its style
            val style = LocalTextStyle.current
            val color = LocalContentColor.current
            val alpha = LocalContentAlpha.current

            val newStyle = remember {
                style.copy(color = color.copy(alpha = alpha))
            }

            Column {

                WordRow(wordState, onBookmarkClicked, onMoreInfoClicked)

                TopText(text, wordState, highlightedSpanState, newStyle, onTopTextClick)

                TopTextBar(
                    languagesFrom,
                    languagesTo,
                    sourceLangIndex,
                    detectedLanguageState,
                    playIconState,
                    onSourceLangChanged,
                    onPlayButtonClick
                )

                BottomText(translation, highlightedSpanState, newStyle, onBottomTextClick)

                BottomTextBar(languagesTo, targetLangIndex, onTargetLangChanged)
            }
        }

        // Handle swipeable events
        if (swipeableState.isAnimationRunning) {
            DisposableEffect(Unit) {
                onDispose {
                    when (swipeableState.currentValue) {
                        SwipeDirection.Right, SwipeDirection.Left -> onDismiss()
                        else -> return@onDispose
                    }
                }
            }
        }
    }
}

@Composable
fun WordRow(
    wordState: MutableState<WordState?>,
    onBookmarkClicked: () -> Unit,
    onMoreInfoClicked: () -> Unit
) {
    val state = wordState.value
    if (state != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.word.definition, Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBookmarkClicked) {
                val iconRes = if(state.isSaved) R.drawable.ic_bookmark_black_24dp else R.drawable.ic_bookmark_border_black_24dp
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = stringResource(R.string.save_icon_description),
                )
            }
            IconButton(onClick = onMoreInfoClicked) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_info_24),
                    contentDescription = stringResource(R.string.more_information),
                )
            }
        }

        Divider()
    }
}

@Composable
fun TopText(
    text: String,
    wordState: MutableState<WordState?>,
    highlightedSpanState: MutableState<SplitPageSpan?>,
    textStyle: TextStyle,
    onTopTextClick: (Int) -> Unit
) {

    val topString = buildAnnotatedString {
        append(text)

        val selectionColors = LocalTextSelectionColors.current
        val highlightColor = remember { selectionColors.backgroundColor }
        val topSpan = highlightedSpanState.value?.topSpan
        if (topSpan != null)
            addStyle(style = SpanStyle(background = highlightColor), topSpan.start, topSpan.end)

        val wordSpan = wordState.value?.span
        if (wordSpan != null) {
            val highlight = if (topSpan != null && topSpan.intersects(wordSpan)) YellowNoteHighlight else highlightColor
            addStyle(SpanStyle(background = highlight), wordSpan.start, wordSpan.end)
        }
    }

    ClickableText(
        topString,
        style = textStyle,
        modifier = Modifier
            .padding(8.dp)
            .heightIn(0.dp, 120.dp)
            .verticalScroll(rememberScrollState(0)),
        onClick = onTopTextClick,
    )
}

@Composable
fun TopTextBar(
    languagesFrom: StringList,
    languagesTo: StringList,
    sourceLangIndex: Int,
    detectedLanguageState: MutableIntState,
    playIconState: MutableState<PlayIconState>,
    onSourceLangChanged: (Int) -> Unit,
    onPlayButtonClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.primary)
            .fillMaxWidth()
    ) {

        Text(text = "From:", Modifier.padding(horizontal = 8.dp))

        var sourcePos by remember { mutableIntStateOf(sourceLangIndex) }
        val detectedLanguageIndex = detectedLanguageState.intValue
        val displayText = if (sourcePos == 0 && detectedLanguageIndex != -1)
            "Auto detect (${languagesTo.items.getOrNull(detectedLanguageIndex)})" else null
        Spinner(languagesFrom, sourcePos, displayText) { index, _ ->
            onSourceLangChanged(index)
            sourcePos = index
        }
        Spacer(Modifier.weight(1f))

        PlayButton(playIconState, onPlayButtonClick)
    }
}

@Composable
fun BottomText(
    translation: MutableState<String>,
    highlightedSpanState: MutableState<SplitPageSpan?>,
    textStyle: TextStyle,
    onBottomTextClick: (Int) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append(translation.value)
        val bottomSpan = highlightedSpanState.value?.bottomSpan
        if (bottomSpan != null)
            this.addStyle(SpanStyle(background = LocalTextSelectionColors.current.backgroundColor), bottomSpan.start, bottomSpan.end)
    }

    ClickableText(
        annotatedString,
        style = textStyle,
        modifier = Modifier
            .padding(8.dp)
            .heightIn(0.dp, 120.dp)
            .verticalScroll(rememberScrollState()),
        onClick = onBottomTextClick
    )
}

@Composable
fun BottomTextBar(languagesTo: StringList, langIndex: Int, onTargetLangChanged: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.primary)
            .fillMaxWidth()
    ) {
        Text(text = "To:", Modifier.padding(horizontal = 8.dp))
        Spinner(languagesTo, langIndex) { index, _ ->
            onTargetLangChanged(index)
        }
    }
}

@Composable
fun PlayButton(playIconState: MutableState<PlayIconState>, onPlayButtonClick: () -> Unit) {
    val playIcon = playIconState.value

    if (playIcon.isLoading) {
        CircularProgressIndicator(
            color = MaterialTheme.colors.secondary,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
    } else {
        val iconRes = if (playIcon.isTTSAvailable) {
            if (playIcon.isPlaying) R.drawable.ic_stop_black_24dp else R.drawable.ic_volume_up_black_24dp
        } else {
            R.drawable.baseline_volume_off_24
        }
        IconButton(onClick = onPlayButtonClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(R.string.play_tts_icon_description),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditWordDialog(
    isShown: Boolean,
    word: String,
    language: String,
    translation: String,
    notes: String?,
    languages: LanguagesList,
    isSaved: Boolean = false,
    onSave: (WordUI) -> Unit = {},
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (!isShown) return

    var wordText by remember { mutableStateOf(word) }
    val isoIndex = languages.iso.indexOf(language)
    var indexLang by remember { mutableIntStateOf(isoIndex) }
    var translationText by remember { mutableStateOf(translation) }
    var notesText by remember { mutableStateOf(notes) }

    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.testTag(EDIT_WORD_DIALOG_TAG)) {
            Column(Modifier.padding(16.dp)) {
                TextField(
                    value = wordText,
                    onValueChange = { wordText = it },
                    label = { Text(stringResource(R.string.word_edit_text)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = languages.fullNames.getOrNull(indexLang) ?: "",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.language_edit_text)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {

                        Box(modifier = Modifier.size(width = 300.dp, height = 600.dp)) {
                            LazyColumn {
                                itemsIndexed(languages.fullNames) { i, lang ->
                                    DropdownMenuItem(onClick = {
                                        indexLang = i
                                        expanded = false
                                    }) {
                                        Text(text = "$lang (${languages.iso[i]})")
                                    }
                                }
                            }
                        }
                    }
                }

                TextField(
                    value = translationText,
                    onValueChange = { translationText = it },
                    label = { Text(stringResource(R.string.translation_edit_text)) },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = notesText ?: "",
                    onValueChange = { notesText = it },
                    label = { Text(stringResource(id = R.string.notes_edit_text)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier =  Modifier.padding(top = 8.dp))  {

                    if (isSaved) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_black_24dp),
                                contentDescription = stringResource(R.string.play_tts_icon_description),
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
                    Button(onClick = {
                        onSave(WordUI(wordText, languages.iso[indexLang], translationText, notesText))
                    }) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

const val EDIT_WORD_DIALOG_TAG = "edit word dialog tag"

data class WordState(
    val word: WordUI,
    val dbId: Int = NOT_SAVED_ID,
    val span: Span? = null,
) {
    val isSaved = dbId != NOT_SAVED_ID
}

fun WordState.toWord() = word.toWord().apply { id = dbId }

data class PlayIconState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isTTSAvailable: Boolean = true,
)

enum class SwipeDirection(val state: Int) {
    Initial(0),
    Right(1),
    Left(2),
    Top(3),
    Bottom(4),
}

private val languages = StringList(listOf("Auto detect", "English", "Spanish", "German"))

@Preview
@Composable
fun SentenceDialogPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme {
        SentenceDialog(
            true,
            text,
            remember { mutableStateOf(text) },
            languages,
            languages,
            targetLangIndex = 1,
            sourceLangIndex = 0,
            detectedLanguageState = remember { mutableIntStateOf(3) }
        )
    }
}

@Preview
@Composable
fun DarkSentenceDialogWithWordPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme(darkTheme = true) {
        SentenceDialog(
            true,
            text,
            remember { mutableStateOf(text) },
            languages,
            languages,
            targetLangIndex = 1,
            sourceLangIndex = 0,
            detectedLanguageState = remember { mutableIntStateOf(3) },
            wordState = remember { mutableStateOf(WordState(WordUI("Original", "en",  "Translation"), 1, Span(6, 11))) }
        )
    }
}

@Preview
@Composable
fun EditWordDialogPreview() {
    AppTheme {
        EditWordDialog(
            true,
            "Hola",
            "es",
            "Hello",
            "Spanish greeting",
            LanguagesList(listOf("English", "Spanish", "German"), listOf("en", "es", "de")),
            true
        )
    }
}

const val NOT_SAVED_ID = 0
