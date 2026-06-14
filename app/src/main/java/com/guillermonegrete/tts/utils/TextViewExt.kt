package com.guillermonegrete.tts.utils

import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.widget.TextView
import androidx.annotation.ColorInt
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.ui.theme.HighlightColorInt

/**
 * Finds the word in the text view for the given [offset] obtained using [TextView.getOffsetForPosition].
 */
fun TextView.findWordForRightHanded(
    offset: Int
): Span { // when you touch ' ', this method returns left word.
    return text.findWord(offset)
}

fun CharSequence.findWord(position: Int): Span {
    val str = this
    var newOffset = position
    if (str.length == newOffset) {
        newOffset-- // without this code, you will get exception when touching end of the text
    }
    if (str[newOffset] == ' ') {
        newOffset--
    }
    var startIndex = newOffset
    var endIndex = newOffset
    try {
        if(Character.isLetterOrDigit(str[startIndex])){
            do {
                startIndex--
            } while (Character.isLetterOrDigit(str[startIndex]))
            startIndex++
        }
    } catch (_: StringIndexOutOfBoundsException) {
        startIndex = 0
    }
    try {
        while (Character.isLetterOrDigit(str[endIndex])) {
            endIndex++
        }
    } catch (_: StringIndexOutOfBoundsException) {
        endIndex = str.length
    }

    return Span(startIndex, endIndex)
}

/**
 * Returns the selected [CharSequence] selected
 */
fun TextView.getSelectedText(): CharSequence? {
    return if (isFocused) {

        // We need to make sure start and end are within the text length
        val min = 0.coerceAtLeast(selectionStart.coerceAtMost(selectionEnd))
        val max = 0.coerceAtLeast(selectionStart.coerceAtLeast(selectionEnd))

        text.subSequence(min, max)
    } else null
}

fun Spannable.addHighlightedText(
    start: Int,
    end: Int,
    @ColorInt color: Int = HighlightColorInt
): BackgroundColorSpan {
    val span = BackgroundColorSpan(color)
    setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return span
}

fun Spannable.addHighlightedText(
    start: Int,
    end: Int,
    span: BackgroundColorSpan
): BackgroundColorSpan {
    setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return span
}

fun String.isWord() = split(" ").size == 1

fun Spanned.splitKeepingSpans(delimiter: String): List<Spanned> {
    val result = mutableListOf<Spanned>()
    var startIndex = 0

    while (startIndex <= this.length) {
        var delimiterIndex = this.indexOf(delimiter, startIndex)
        if (delimiterIndex == -1) {
            delimiterIndex = this.length
        }

        // Extract the chunk and its spans
        val chunk = this.subSequence(startIndex, delimiterIndex) as Spanned
        result.add(chunk)

        // Move past the newline character length
        startIndex = delimiterIndex + delimiter.length

        // Break loop if we reached the end of the text
        if (delimiterIndex == this.length) break
    }
    return result
}

