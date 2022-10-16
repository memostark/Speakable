package com.guillermonegrete.tts.utils

import android.widget.TextView

/**
 * Finds the word in the text view for the given [offset] obtained using [TextView.getOffsetForPosition].
 */
fun TextView.findWordForRightHanded(
    offset: Int
): String { // when you touch ' ', this method returns left word.
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
        while (Character.isLetterOrDigit(str[startIndex])) {
            startIndex--
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

    return str.substring(startIndex, endIndex)
}
