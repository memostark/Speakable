package com.guillermonegrete.tts.importtext.visualize

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView


/**
 * Custom movement method that allows to select text when using ClickableSpans.
 *
 * Taken from: https://stackoverflow.com/a/30572151/10244759
 * Same code as LinkMovementMethod but removes all calls to Selection.removeSelection
 */
class SelectMovementMethod: LinkMovementMethod() {

    override fun canSelectArbitrarily() = true

    override fun initialize(widget: TextView?, text: Spannable?) {
        text?.let { Selection.setSelection(it, it.length) }
    }

    override fun onTakeFocus(view: TextView?, text: Spannable?, dir: Int) {
        text?.let {
            if ((dir and (View.FOCUS_FORWARD or View.FOCUS_DOWN)) != 0) {
                if (view?.layout == null) {
                    // This shouldn't be null, but do something sensible if it is.
                    Selection.setSelection(text, it.length)
                }
            } else {
                Selection.setSelection(text, it.length)
            }
        }
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action

        if(action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN){
            var x = event.x.toInt()
            var y = event.y.toInt()

            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop

            x += widget.scrollX
            y += widget.scrollY

            val layout = widget.layout
            val line = layout?.getLineForVertical(y) ?: 0
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            val link = buffer.getSpans(off, off, ClickableSpan::class.java)

            if (link.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) {
                    link.first().onClick(widget)
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(buffer,
                        buffer.getSpanStart(link.first()),
                        buffer.getSpanEnd(link.first()))
                }
                return true
            }
        }

        return Touch.onTouchEvent(widget, buffer, event)
    }
}