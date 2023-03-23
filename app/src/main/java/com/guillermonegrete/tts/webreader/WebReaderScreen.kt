package com.guillermonegrete.tts.webreader

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guillermonegrete.tts.R


@Composable
fun WebReaderBottomBar(
    languages: MutableState<List<String>>,
    langSelection: MutableState<Int> = mutableStateOf(-1),
    iconsEnabled: MutableState<Boolean> = mutableStateOf(true),
    onTranslateClicked: () -> Unit = {},
    onArrowClicked: (isLeft: Boolean) -> Unit = {},
    onLangSelected: (Int, String) -> Unit = { _, _ -> },
) {
    val iconsState by iconsEnabled

    BottomAppBar {
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

        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Desc")
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

private val suggestions = mutableStateOf(listOf("Item1", "Item2", "Item3"))

@Preview
@Composable
fun BarPreview() {
    MaterialTheme {
        WebReaderBottomBar(remember { suggestions })
    }
}

@Preview
@Composable
fun SpinnerPreview() {
    MaterialTheme {
        Spinner(remember { suggestions })
    }
}
