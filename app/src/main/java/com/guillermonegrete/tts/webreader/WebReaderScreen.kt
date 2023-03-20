package com.guillermonegrete.tts.webreader

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guillermonegrete.tts.R


@Composable
fun WebReaderBottomBar(languages: MutableState<List<String>>) {

    BottomAppBar {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(R.drawable.ic_translate_black_24dp),
                contentDescription = stringResource(R.string.translate_description)
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_new_24),
                contentDescription = stringResource(R.string.previous_selection)
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_arrow_forward_ios_24),
                contentDescription = stringResource(R.string.next_selection)
            )
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Spinner(languages)
        }

        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Desc")
        }
    }
}

@Composable
fun Spinner(items: MutableState<List<String>>) {
    var selected by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val spinnerItems by items

    Box {
        Button(onClick = { expanded = !expanded }) {
            Text(text = selected)
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

            spinnerItems.forEach { label ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    selected = label
                }) {
                    Text(text = label)
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
