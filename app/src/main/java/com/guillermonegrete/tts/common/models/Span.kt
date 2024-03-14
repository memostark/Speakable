package com.guillermonegrete.tts.common.models

data class Span(val start: Int, val end: Int) {

    fun inside(pos: Int) = pos in start..end

    fun intersects(span: Span) = start < span.end && end > span.start
}
