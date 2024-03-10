package com.guillermonegrete.tts.common.compose

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guillermonegrete.tts.ui.theme.AppTheme

@Composable
fun YesNoDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirmation() }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
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