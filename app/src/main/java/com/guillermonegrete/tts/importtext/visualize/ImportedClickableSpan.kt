package com.guillermonegrete.tts.importtext.visualize

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

open class ImportedClickableSpan: ClickableSpan() {

    private var clicked = false
    /**
     * Color taken from member variable mHighlightColor from TextView class.
     */
    private var highLightColor = 0x6633B5E5

    override fun onClick(widget: View) {
        clicked = true
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = if(clicked) highLightColor else Color.TRANSPARENT
        clicked = false
    }
}