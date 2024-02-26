package com.guillermonegrete.tts.textprocessing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.webreader.Spinner

@Composable
fun SentenceDialog(text: String, translation: String, languages: List<String>) {
    Dialog(onDismissRequest = {}) {
        Surface {
            Column {

                Text(text, maxLines = 5, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {

                    Text(text = "Original:", Modifier.padding(horizontal = 8.dp))

                    Spinner(items = languages, preselected = 1)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up_black_24dp),
                        contentDescription = stringResource(R.string.play_tts_icon_description),
                        modifier = Modifier
                            .padding(16.dp)
                            .align(alignment = Alignment.CenterVertically)
                    )
                }

                Text(translation, maxLines = 5, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {
                    Text(text = "Translate to:", Modifier.padding(horizontal = 8.dp))
                    Spinner(items = languages, preselected = 0)
                }
            }
        }
    }
}

val languages = listOf("English", "Spanish", "German")

@Preview
@Composable
fun SentenceDialogPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme {
        SentenceDialog(text, text, languages)
    }
}