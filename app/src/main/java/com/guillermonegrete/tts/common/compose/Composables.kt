package com.guillermonegrete.tts.common.compose

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.ExternalLinkUI
import com.guillermonegrete.tts.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun YesNoDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String?,
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = if (dialogText != null) { { Text(dialogText) } } else null,
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
                        DropdownMenuItem(
                            text = { Text(text = item) },
                            onClick = {
                                expanded = false
                                selected = index
                                onItemSelected(index, item)
                            }
                        )
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
    links: ExternalLinkList,
    selection: Int,
    onItemClick: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if(!isShown) return

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(selection) {
        coroutineScope.launch { listState.animateScrollToItem(selection) }
    }
    var selected by remember { mutableIntStateOf(selection) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
        ) {
            Column(Modifier.height(400.dp)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()

                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                        }
                    },
                    update = { webView ->
                        val externalLink = links.items.getOrNull(selected)
                        if (externalLink != null) webView.loadUrl(externalLink.link)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                LazyRow(state = listState) {
                    itemsIndexed(links.items) {index, link ->

                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min) // This prevents the divider's height from taking all the space
                        ) {
                            if (index == selected) {
                                Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                                    TextButton(onClick = {
                                        selected = index
                                        coroutineScope.launch { listState.animateScrollToItem(index) }
                                        onItemClick(index)
                                    }) {
                                        Text(text = link.siteName, modifier = Modifier.padding(vertical = 6.dp))
                                    }
                                    HorizontalDivider(
                                        thickness = 4.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                TextButton(onClick = {
                                    selected = index
                                    coroutineScope.launch { listState.animateScrollToItem(index) }
                                    onItemClick(index)
                                }) {
                                    Text(text = link.siteName, modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }

                            if (index < links.items.lastIndex) VerticalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogList(
    list: List<String>,
    title: String? = null,
    onItemSelected: (Int, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
) {

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(Modifier.padding(horizontal = 16.dp)) {
            Column {
                if (title != null) {
                    Text(text = title, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.headlineSmall)
                    HorizontalDivider()
                }

                LazyColumn(Modifier.testTag(DIALOG_LIST_TAG)) {
                    itemsIndexed(list) { index, item ->
                        DropdownMenuItem(
                            text = { Text(text = item) },
                            onClick = { onItemSelected(index, item) }
                        )
                    }
                }
            }
        }
    }
}

const val DIALOG_LIST_TAG = "dialog list tag"

@Preview
@Composable
fun ExternalLinksDialogPreview() {
    AppTheme {
        val links = ExternalLinkList(List(4) { ExternalLinkUI("External site", "", "") })
        ExternalLinksDialog(true, links, 1)
    }
}

private val suggestions = StringList(listOf("Item1", "Item2", "Item3"))

@Preview
@Composable
fun SpinnerPreview() {
    AppTheme {
        Column {
            Spinner(suggestions)
            Spinner(suggestions, 0)
        }
    }
}

@Preview
@Composable
fun DialogListPreview() {
    AppTheme {
        DialogList(List(3) { "Item ${it + 1}" }, "With title")
    }
}

@Preview
@Composable
fun DialogListNoTitlePreview() {
    AppTheme {
        DialogList(List(3) { "Item ${it + 1}" })
    }
}
