package com.guillermonegrete.tts.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView with scrolling disabled
 */
class NonScrollingTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun scrollTo(x: Int, y: Int) {
        // Do nothing
    }
}