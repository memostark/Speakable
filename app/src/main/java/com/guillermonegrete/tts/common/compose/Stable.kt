package com.guillermonegrete.tts.common.compose

import androidx.compose.runtime.Immutable
import com.guillermonegrete.tts.common.models.ExternalLinkUI

@Immutable
data class StringList(val items: List<String>)

@Immutable
data class LanguagesList(val fullNames: List<String>, val iso: List<String>)

@Immutable
data class ExternalLinkList(val items: List<ExternalLinkUI>)
