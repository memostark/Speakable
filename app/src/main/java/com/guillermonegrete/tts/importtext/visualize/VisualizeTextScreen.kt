package com.guillermonegrete.tts.importtext.visualize

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.textprocessing.SwipeDirection
import com.guillermonegrete.tts.ui.theme.AppTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteSheet(
    isShown: Boolean,
    textProvider: () -> String,
    infoButtonVisibility: () -> Boolean = { true },
    onEditClicked: () -> Unit = {},
    onInfoClicked: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {

    if (!isShown) return

    val density = LocalDensity.current
    val swipeableState = remember {
        AnchoredDraggableState(
            SwipeDirection.Initial,
            { distance -> distance * 0.5f },
            { with(density) { 125.dp.toPx() }},
            tween(),
            FloatExponentialDecaySpec().generateDecayAnimationSpec(),
        )
    }

    // Allows nested scrolling of the swipeable note and the scrollable text
    val connection = remember {
        object: NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Only handle one direction of delta because the sheet can only swipe downwards.
                return if (delta < 0) {
                    Offset(0f, swipeableState.dispatchRawDelta(delta))
                } else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity) = available

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return  Offset(0f, swipeableState.dispatchRawDelta(available.y))
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                swipeableState.settle(available.y)
                return super.onPostFling(consumed, available)
            }
        }
    }

    Popup(
        Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(dismissOnClickOutside = false),
    ) {
        OutlinedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .onSizeChanged {
                    val sizePx = it.height.toFloat()
                    swipeableState.updateAnchors(
                        DraggableAnchors { SwipeDirection.Initial at 0f; SwipeDirection.Bottom at sizePx}
                    )
                }
                .anchoredDraggable(swipeableState, Orientation.Vertical)
                .offset { IntOffset(0, swipeableState.requireOffset().roundToInt()) }
                .nestedScroll(connection)
                .widthIn(0.dp, 700.dp)
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {

                val scroll = rememberScrollState(0)
                Text(
                    textProvider(),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .heightIn(0.dp, 96.dp)
                        .weight(1f)
                        .verticalScroll(scroll)
                )
                IconButton(onClick = onEditClicked) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_icon_description),
                    )
                }
                if (infoButtonVisibility()) {
                    IconButton(onClick = onInfoClicked) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.more_information),
                        )
                    }
                }
            }
        }
    }

    // Handle swipeable events
    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                when (swipeableState.currentValue) {
                    SwipeDirection.Bottom -> onDismiss()
                    else -> return@onDispose
                }
            }
        }
    }
}

@Composable
fun ContentMenu(
    hasToC: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (item: ContentMenuItem) -> Unit = {},
) {
    Box(
        Modifier
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(end = 8.dp) // Adds additional padding
    ) {
        Popup(
            alignment = Alignment.BottomEnd,
            onDismissRequest = onDismiss,
        ) {
            ElevatedCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (hasToC) {
                        Text(
                            text = AnnotatedString(stringResource(R.string.table_of_contents)),
                            modifier = Modifier
                                .clickable(true) { onItemClick(ContentMenuItem.TABLE_OF_CONTENTS) }
                                .testTag(TOC_BTN_TAG)
                                .padding(8.dp),
                        )
                    }

                    Text(
                        text = AnnotatedString(stringResource(R.string.show_notes_list)),
                        modifier = Modifier
                            .clickable(true) { onItemClick(ContentMenuItem.NOTES) }
                            .testTag(NOTE_BTN_TAG)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NoteSheetPreview() {
    AppTheme {
        NoteSheet(true, {"My note text"})
    }
}

@Preview
@Composable
fun ContentMenuPreview() {
    AppTheme {
        ContentMenu(true, {})
    }
}

const val TOC_BTN_TAG = "show toc btn"
const val NOTE_BTN_TAG = "show notes btn"

enum class ContentMenuItem {
    TABLE_OF_CONTENTS,
    NOTES;
}
