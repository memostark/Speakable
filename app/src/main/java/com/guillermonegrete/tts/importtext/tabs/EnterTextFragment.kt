package com.guillermonegrete.tts.importtext.tabs

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.window.core.layout.WindowHeightSizeClass
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextFragment
import com.guillermonegrete.tts.ui.theme.AppTheme

class EnterTextFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        EnterTextScreen()
    }

    private fun getClipboardText(): String{
        val clip = (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        val pasteData = clip.getItemAt(0).text
        return pasteData?.toString() ?: ""
    }

    private fun visualizeText(text: String){
        val intent = Intent(context, VisualizeTextActivity::class.java)
        intent.putExtra(VisualizeTextFragment.IMPORTED_TEXT, text)
        startActivity(intent)
    }

    @PreviewScreenSizes
    @Composable
    fun EnterTextScreen() {
        AppTheme {
            val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
            var textField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue())
            }
            if (windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT) {
                EnterTextScreenHeightCompact( { textField }, { textField = it } )
            } else {
                EnterTextScreenDefault( { textField }, { textField = it } )
            }
        }
    }

    @Composable
    fun EnterTextScreenDefault(
        textProvider: () -> TextFieldValue,
        onTextChange: (text: TextFieldValue) -> Unit,
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp)
        ) {
            EnterTextField(
                { textProvider() },
                onTextChange,
                textLines = 8,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        val text = getClipboardText()
                        onTextChange(TextFieldValue(text, TextRange(text.length)))
                    }
                ) {
                    Text(stringResource(id = R.string.paste_icon_description))
                }
                Button(onClick = { visualizeText(textProvider().text) }) {
                    Text(stringResource(id = R.string.visualize_label))
                }
            }
        }
    }

    @Composable
    fun EnterTextScreenHeightCompact(
        textProvider: () -> TextFieldValue,
        onTextChange: (text: TextFieldValue) -> Unit,
    ) {
        Row (
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp)
        ) {
            EnterTextField(
                { textProvider() },
                onTextChange
            )

            Spacer(Modifier.width(16.dp))

            Column (Modifier.width(IntrinsicSize.Min)) {
                Button(
                    onClick = {
                        val text = getClipboardText()
                        onTextChange(TextFieldValue(text, TextRange(text.length)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(id = R.string.paste_icon_description))
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { visualizeText(textProvider().text) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(id = R.string.visualize_label))
                }
            }
        }
    }

    @Composable
    fun EnterTextField(
        textField: () -> TextFieldValue,
        onTextFieldChange: (textFieldValue: TextFieldValue) -> Unit,
        textLines: Int = 4,
    ) {
        val field = textField()
        var textFieldValue by remember(field) { mutableStateOf(field) }
        val clearVisible by remember(textFieldValue.text) { derivedStateOf { textFieldValue.text.isNotBlank() } }
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = onTextFieldChange,
            label = { Text(stringResource(R.string.import_text_hint)) },
            minLines = textLines,
            maxLines = textLines,
            trailingIcon = {
                if (clearVisible) {
                    IconButton(onClick = { onTextFieldChange(TextFieldValue()) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            }
        )
    }
}
