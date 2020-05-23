package com.guillermonegrete.tts.utils

import android.content.Context
import android.util.DisplayMetrics

fun Context.dpToPixel(dp: Int): Int {
    return dp * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}