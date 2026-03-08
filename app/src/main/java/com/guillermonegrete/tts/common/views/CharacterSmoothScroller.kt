package com.guillermonegrete.tts.common.views

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


/**
 * Scrolls to the index of a char within a [TextView], placing the line at the top of the [RecyclerView].
 * The view type of the list items must be [TextView].
 */
class CharacterSmoothScroller(context: Context, val charIndex: Int) : LinearSmoothScroller(context) {

    private var charOffset: Int? = null

    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        if (targetView is TextView) charOffset = targetView.charLocationY(charIndex)
        super.onTargetFound(targetView, state, action)
    }

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        val offset = charOffset ?: 0
        return boxStart - (viewStart + offset)
    }

    fun TextView.charLocationY(offset: Int): Int? {
        layout ?: return null

        val lineOfText = layout.getLineForOffset(offset)
        return layout.getLineTop(lineOfText)
    }
}
