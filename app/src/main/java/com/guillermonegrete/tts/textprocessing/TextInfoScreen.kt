package com.guillermonegrete.tts.textprocessing

import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.webreader.Spinner
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SentenceDialog(
    isVisible: Boolean,
    text: String,
    translation: String,
    languagesFrom: List<String>,
    languagesTo: List<String>,
    targetLangIndex: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    isTTSAvailable: Boolean,
    sourceLangIndex: Int = 0,
    detectedLanguage: String? = null,
    highlightedSpan: SplitPageSpan? = null,
    onPlayButtonClick: () -> Unit = {},
    onBottomTextClick: (Int) -> Unit = {},
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

        val swipeableState = rememberSwipeableState(SwipeDirection.Initial)
        var sizePx by remember { mutableStateOf(1f) }
        val anchors = mapOf(0f to SwipeDirection.Initial, sizePx to SwipeDirection.Right, -sizePx to SwipeDirection.Left)

        Surface(modifier = Modifier
            .padding(16.dp)
            .onGloballyPositioned { sizePx = it.size.width.toFloat() }
            .swipeable( // Use swipeable API for handling the horizontal dismiss
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.6f) },
                orientation = Orientation.Horizontal
            )
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
            .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
        ) {
            Column {

                val selectionColors = LocalTextSelectionColors.current
                val highlightColor = remember { selectionColors.backgroundColor }
                val topString = buildAnnotatedString {
                    append(text)
                    if (highlightedSpan != null)
                        addStyle(style = SpanStyle(background = highlightColor), start = highlightedSpan.topSpan.start, end = highlightedSpan.topSpan.end)
                }

                Text(topString, modifier = Modifier
                    .padding(8.dp)
                    .heightIn(0.dp, 120.dp)
                    .verticalScroll(rememberScrollState(0)))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {

                    Text(text = "From:", Modifier.padding(horizontal = 8.dp))

                    var sourcePos by remember { mutableStateOf(sourceLangIndex) }
                    val displayText = if (sourcePos == 0 && detectedLanguage != null) "Auto detect ($detectedLanguage)" else null
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

                // For whatever reason the ClickableText doesn't use the same style as the Text composable, this causes problems with dark mode
                // This is similar to how Text creates its style
                val style = LocalTextStyle.current
                val color = LocalContentColor.current
                val alpha = LocalContentAlpha.current

                val newStyle = remember {
                    style.copy(color = color.copy(alpha = alpha))
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
        SentenceDialog(true, text, text, languages, languages, targetLangIndex = 1, sourceLangIndex = 0, detectedLanguage = "Latin", isPlaying = false, isLoading = false, isTTSAvailable = true)
    }
}