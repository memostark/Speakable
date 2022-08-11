package com.guillermonegrete.tts.utils

import android.content.Context
import android.util.DisplayMetrics

fun Context.dpToPixel(dp: Int): Int {
    return dp * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

val Context.actionBarSize
    get() = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        .let { attrs -> attrs.getDimension(0, 0F).toInt().also { attrs.recycle() } }