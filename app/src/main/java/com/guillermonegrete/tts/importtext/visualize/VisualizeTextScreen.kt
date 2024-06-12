package com.guillermonegrete.tts.importtext.visualize

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
            tween()
        )
    }

    Popup(
        Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(dismissOnClickOutside = false),
    ) {
        Card(
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface),
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

@Preview
@Composable
fun NoteSheetPreview() {
    AppTheme {
        NoteSheet(true, {"My note text"})
    }
}