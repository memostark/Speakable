package com.guillermonegrete.tts.textprocessing

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
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
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import com.guillermonegrete.tts.webreader.Spinner
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SentenceDialog(
    isVisible: Boolean,
    text: String,
    translation: String,
    languagesFrom: List<String>,
    languagesTo: List<String>,
    targetLangIndex: Int,
    isPlaying: Boolean = false,
    isLoading: Boolean = false,
    isTTSAvailable: Boolean = true,
    sourceLangIndex: Int = 0,
    detectedLanguageIndex: Int? = null,
    highlightedSpan: SplitPageSpan? = null,
    wordState: WordState = WordState(),
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

        Surface(modifier = Modifier
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
            .testTag("sentence_dialog")
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

                val word = wordState.word
                if (word != null) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(word.definition, Modifier.padding(horizontal = 8.dp))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onBookmarkClicked) {
                            val iconRes = if(wordState.isSaved) R.drawable.ic_bookmark_black_24dp else R.drawable.ic_bookmark_border_black_24dp
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


                val selectionColors = LocalTextSelectionColors.current
                val highlightColor = remember { selectionColors.backgroundColor }
                val topString = buildAnnotatedString {
                    append(text)
                    if (highlightedSpan != null)
                        addStyle(style = SpanStyle(background = highlightColor), start = highlightedSpan.topSpan.start, end = highlightedSpan.topSpan.end)

                    val wordSpan = wordState.span
                    if (wordSpan != null) {
                        val highlight = if (highlightedSpan != null && highlightedSpan.topSpan.intersects(wordSpan)) YellowNoteHighlight else highlightColor
                        addStyle(SpanStyle(background = highlight), wordSpan.start, wordSpan.end)
                    }
                }

                ClickableText(
                    topString,
                    style = newStyle,
                    modifier = Modifier
                        .padding(8.dp)
                        .heightIn(0.dp, 120.dp)
                        .verticalScroll(rememberScrollState(0)),
                    onClick = onTopTextClick,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {

                    Text(text = "From:", Modifier.padding(horizontal = 8.dp))

                    var sourcePos by remember { mutableIntStateOf(sourceLangIndex) }
                    val displayText = if (sourcePos == 0 && detectedLanguageIndex != null)
                        "Auto detect (${languagesTo.getOrNull(detectedLanguageIndex)})" else null
                    Spinner(languagesFrom, sourcePos, displayText) { index, _ ->
                        onSourceLangChanged(index)
                        sourcePos = index
                    }
                    Spacer(Modifier.weight(1f))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    } else {
                        val iconRes = if(isTTSAvailable) {
                            if (isPlaying) R.drawable.ic_stop_black_24dp else R.drawable.ic_volume_up_black_24dp
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

                val annotatedString = buildAnnotatedString {
                    append(translation)
                    if (highlightedSpan != null)
                        this.addStyle(style = SpanStyle(background = highlightColor), start = highlightedSpan.bottomSpan.start, end = highlightedSpan.bottomSpan.end)
                }

                ClickableText(
                   annotatedString,
                   style = newStyle,
                   modifier = Modifier
                       .padding(8.dp)
                       .heightIn(0.dp, 120.dp)
                       .verticalScroll(rememberScrollState()),
                   onClick = onBottomTextClick
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {
                    Text(text = "To:", Modifier.padding(horizontal = 8.dp))
                    Spinner(languagesTo, targetLangIndex) { index, _ ->
                        onTargetLangChanged(index)
                    }
                }
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditWordDialog(
    isShown: Boolean,
    word: String,
    language: String,
    translation: String,
    notes: String?,
    languages: List<String>,
    languagesISO: List<String>,
    isSaved: Boolean = false,
    onSave: (Words) -> Unit = {},
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (!isShown) return

    var wordText by remember { mutableStateOf(word) }
    val isoIndex = languagesISO.indexOf(language)
    var indexLang by remember { mutableIntStateOf(isoIndex) }
    var translationText by remember { mutableStateOf(translation) }
    var notesText by remember { mutableStateOf(notes) }

    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface {
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
                        value = languages.getOrNull(indexLang) ?: "",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.language_edit_text)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        languages.forEachIndexed { i, lang ->
                            DropdownMenuItem(
                                onClick = {
                                    indexLang = i
                                    expanded = false
                                }
                            ){
                                Text(text = "$lang (${languagesISO[i]})")
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

                Row (horizontalArrangement = Arrangement.spacedBy(8.dp), modifier =  Modifier.padding(top = 8.dp))  {

                    if(isSaved) {
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
                        onSave(Words(wordText, languagesISO[indexLang], translationText).apply { this.notes = notesText })
                    }) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }

}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalLinksDialog(
    isShown: Boolean,
    links: List<ExternalLink>,
    selection: Int,
    onItemClick: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if(!isShown) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
        ) {
            Column {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()

                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(links[selection].link)
                    },
                    modifier = Modifier
                        .height(350.dp)
                        .fillMaxWidth()
                )

                LazyRow {
                    itemsIndexed(links) {index, link ->
                        if (index == selection) {
                            Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                                TextButton(onClick = { onItemClick(index) }) {
                                    Text(text = link.siteName, modifier = Modifier.padding(vertical = 6.dp))
                                }
                                Divider(
                                    thickness = 4.dp,
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        } else {
                            TextButton(onClick = { onItemClick(index) }) {
                                Text(text = link.siteName, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class WordState(
    val word: Words? = null,
    val isSaved: Boolean = false,
    val span: Span? = null,
)

enum class SwipeDirection(val state: Int) {
    Initial(0),
    Right(1),
    Left(2),
}

private val languages = listOf("Auto detect", "English", "Spanish", "German")

@Preview
@Composable
fun SentenceDialogPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme {
        SentenceDialog(true, text, text, languages, languages, targetLangIndex = 1, sourceLangIndex = 0, detectedLanguageIndex = 3)
    }
}

@Preview
@Composable
fun DarkSentenceDialogWithWordPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme(darkTheme = true) {
        SentenceDialog(
            true,
            text,
            text,
            languages,
            languages,
            targetLangIndex = 1,
            sourceLangIndex = 0,
            detectedLanguageIndex = 3,
            wordState = WordState(Words("Original", "en",  "Translation"), true, Span(6, 11))
        )
    }
}

@Preview
@Composable
fun EditWordDialogPreview() {
    AppTheme {
        EditWordDialog(true, "Hola", "es", "Hello", "Spanish greeting", listOf("English", "Spanish", "German"), listOf("en", "es", "de"), true)
    }
}

@Preview
@Composable
fun ExternalLinksDialogPreview() {
    AppTheme {
        ExternalLinksDialog(true, List(4) { ExternalLink("External site", "", "") }, 1)
    }
}
