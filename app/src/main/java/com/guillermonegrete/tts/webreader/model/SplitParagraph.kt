package com.guillermonegrete.tts.webreader.model

import com.guillermonegrete.tts.common.models.Span

data class SplitParagraph(
    val paragraph: CharSequence,
    /**
     * The start and end indexes for each sentence in the paragraph
     */
    val indexes: List<Span>,
    val sentences: List<String>
)
