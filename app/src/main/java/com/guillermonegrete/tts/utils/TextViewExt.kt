package com.guillermonegrete.tts.utils

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.BackgroundColorSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.guillermonegrete.tts.common.models.Span

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

/**
 * If this [AppCompatTextView] is placed inside ScrollView then we allow it get scrolled inside
 * that ScrollView
 */
fun AppCompatTextView.makeScrollableInsideScrollView() {
    movementMethod = ScrollingMovementMethod()
    setOnTouchListener { v, event ->
        v.parent.requestDisallowInterceptTouchEvent(true)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
                // It is required to call performClick() in onTouch event.
                performClick()
            }
        }
        false
    }
}

fun TextView.addHighlightedText(start: Int, end: Int, color: Int = Color.argb(128, 255, 0, 0)){
    val text = SpannableString(this.text)

    text.setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    this.setText(text, TextView.BufferType.SPANNABLE)
}

fun Spannable.addHighlightedText(start: Int, end: Int, color: Int = Color.argb(128, 255, 0, 0)){
    setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}
