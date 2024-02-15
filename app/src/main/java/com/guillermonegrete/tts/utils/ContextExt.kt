package com.guillermonegrete.tts.utils

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import timber.log.Timber

fun Context.dpToPixel(dp: Int): Int {
    return (dp * this.resources.displayMetrics.density).toInt()
}

val Context.actionBarSize
    get() = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        .let { attrs -> attrs.getDimension(0, 0F).toInt().also { attrs.recycle() } }

fun Context.getScreenSizes(): ScreenInfo {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets = windowMetrics.windowInsets

        val insets = windowInsets.getInsets(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
        val b = windowMetrics.bounds
        Timber.d("Insets: $insets")

        ScreenInfo(b.width(), b.height(), insets.top, insets.bottom)
    } else @Suppress("DEPRECATION", "InternalInsetResource", "DiscouragedApi") {
        val display = wm.defaultDisplay // deprecated in API 30
        val realSize = Point()
        display?.getRealSize(realSize) // deprecated in API 30
        val usableSize = Point()
        display?.getSize(usableSize)
        Timber.d("Real: $realSize, Usable: $usableSize")

        var statusHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusHeight = resources.getDimensionPixelSize(resourceId)
        }

        ScreenInfo(realSize.x, realSize.y, statusHeight, realSize.y - usableSize.y)
    }
}

@RequiresApi(Build.VERSION_CODES.R)
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
