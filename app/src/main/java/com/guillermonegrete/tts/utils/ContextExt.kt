package com.guillermonegrete.tts.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

fun Context.dpToPixel(dp: Int): Int {
    return dp * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

val Context.actionBarSize
    get() = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        .let { attrs -> attrs.getDimension(0, 0F).toInt().also { attrs.recycle() } }

fun Context.getScreenSizes(): ScreenInfo {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowMetrics = wm.currentWindowMetrics
    val windowInsets = windowMetrics.windowInsets

    val insets = windowInsets.getInsets(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
    val b = windowMetrics.bounds

    return ScreenInfo(b.width(), b.height(), insets.top, insets.bottom)
}

fun Context.statusBarVisible(): Boolean {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowInsets = wm.currentWindowMetrics.windowInsets

    return windowInsets.isVisible(WindowInsets.Type.statusBars())
}

data class ScreenInfo(
    val width: Int,
    val height: Int,
    val statusHeight: Int,
    val navHeight: Int,
)
