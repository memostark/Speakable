package com.guillermonegrete.tts.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Span(val start: Int, val end: Int): Parcelable {

    fun inside(pos: Int) = pos in start..end

    fun intersects(span: Span) = start < span.end && end > span.start
}

fun Span.hasInside(span: Span) = start <= span.start && end >= span.end
