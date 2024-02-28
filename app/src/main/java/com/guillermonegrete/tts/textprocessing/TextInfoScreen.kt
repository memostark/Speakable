package com.guillermonegrete.tts.textprocessing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
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
fun SentenceDialog(
    isVisible: Boolean,
    text: String,
    translation: String,
    languagesFrom: List<String>,
    languagesTo: List<String>,
    targetLangIndex: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    originLangIndex: Int = 0,
    detectedLanguage: String? = null,
    onDismiss: () -> Unit = {},
) {
    if (!isVisible) return

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface {
            Column {

                Text(text, modifier = Modifier
                    .padding(8.dp)
                    .height(120.dp)
                    .verticalScroll(rememberScrollState(0)))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {

                    Text(text = "Original:", Modifier.padding(horizontal = 8.dp))

                    val displayText = if (originLangIndex == 0 && detectedLanguage != null) "Auto detect ($detectedLanguage)" else null
                    Spinner(languagesFrom, originLangIndex, displayText)
                    Spacer(Modifier.weight(1f))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier
                                .padding(8.dp)
                                .height(24.dp)
                        )
                    } else {
                        val iconRes = if (isPlaying) R.drawable.ic_stop_black_24dp else R.drawable.ic_volume_up_black_24dp
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = stringResource(R.string.play_tts_icon_description),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text(translation, modifier = Modifier
                    .padding(8.dp)
                    .height(120.dp)
                    .verticalScroll(rememberScrollState()))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .fillMaxWidth()
                ) {
                    Text(text = "Translate to:", Modifier.padding(horizontal = 8.dp))
                    Spinner(languagesTo, targetLangIndex)
                }
            }
        }
    }
}

private val languages = listOf("Auto detect", "English", "Spanish", "German")

@Preview
@Composable
fun SentenceDialogPreview(@PreviewParameter(LoremIpsum::class) text: String) {
    AppTheme {
        SentenceDialog(true, text, text, languages, languages, targetLangIndex = 1, originLangIndex = 0, detectedLanguage = "Latin", isPlaying = false, isLoading = false)
    }
}