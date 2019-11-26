package com.guillermonegrete.tts.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Queries the theme of the given `context` for a theme color.
 *
 * @param context   the context holding the current theme.
 * @param attrResId the theme color attribute to resolve.
 * @return the theme color
 */
@ColorInt
fun getThemeColor(context: Context, @AttrRes attrResId: Int): Int {
    val a = context.obtainStyledAttributes(null, intArrayOf(attrResId))
    try {
        return a.getColor(0, Color.MAGENTA)
    } finally {
        a.recycle()
    }
}