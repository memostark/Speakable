package com.guillermonegrete.tts.utils

import android.widget.TextView
import com.guillermonegrete.tts.common.models.Span

/**
 * Finds the word in the text view for the given [offset] obtained using [TextView.getOffsetForPosition].
 */
fun TextView.findWordForRightHanded(
    offset: Int
): Span { // when you touch ' ', this method returns left word.
    val str = text.toString()
    var newOffset = offset
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
    } catch (e: StringIndexOutOfBoundsException) {
        startIndex = 0
    }
    try {
        while (Character.isLetterOrDigit(str[endIndex])) {
            endIndex++
        }
    } catch (e: StringIndexOutOfBoundsException) {
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
