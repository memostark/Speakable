package com.guillermonegrete.tts.textprocessing.domain.model

import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.textprocessing.ProcessTextLayoutType
import java.lang.Exception

sealed class GetLayoutResult {
    data class WordSuccess(val type: ProcessTextLayoutType, val word: Words): GetLayoutResult()
    data class DictionarySuccess(val word: Words, val items: List<WikiItem>): GetLayoutResult()
    data class Error(val exception: Exception): GetLayoutResult()
}