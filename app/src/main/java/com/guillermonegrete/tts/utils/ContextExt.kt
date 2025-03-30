package com.guillermonegrete.tts.utils

import android.content.Context
import android.content.res.Resources.getSystem
import android.graphics.Point
import android.os.Build
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import timber.log.Timber

fun Context.dpToPixel(dp: Int): Int {
    return (dp * this.resources.displayMetrics.density).toInt()
}

val Int.dpToPixel: Int get() = (this * getSystem().displayMetrics.density).toInt()

val Context.actionBarSize
    get() = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        .let { attrs -> attrs.getDimension(0, 0F).toInt().also { attrs.recycle() } }

fun Context.getScreenSizes(allBars: Boolean = false): ScreenInfo {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets = windowMetrics.windowInsets

        val bars = if (allBars) WindowInsets.Type.systemBars() else WindowInsets.Type.navigationBars()
        val insets = windowInsets.getInsets(bars or WindowInsets.Type.displayCutout())
        val b = windowMetrics.bounds
        Timber.d("Insets: $insets")

        ScreenInfo(b.width(), b.height(), Insets.toCompatInsets(insets))
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

        ScreenInfo(realSize.x, realSize.y, Insets.of(0, statusHeight, 0, realSize.y - usableSize.y))
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun Context.statusBarVisible(): Boolean {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowInsets = wm.currentWindowMetrics.windowInsets

    return windowInsets.isVisible(WindowInsets.Type.statusBars())
}

fun Context.navBarInsets(): Insets {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets = windowMetrics.windowInsets

        val insets = windowInsets.getInsets(WindowInsets.Type.navigationBars())
        Timber.d("Insets: $insets")

        Insets.toCompatInsets(insets)
    } else @Suppress("DEPRECATION", "InternalInsetResource", "DiscouragedApi") {
        val display = wm.defaultDisplay // deprecated in API 30
        val realSize = Point()
        display?.getRealSize(realSize) // deprecated in API 30
        val usableSize = Point()
        display?.getSize(usableSize)
        Timber.d("Real: $realSize, Usable: $usableSize, rotation: ${display.rotation}")

        when(display.rotation) {
            Surface.ROTATION_90 -> Insets.of(0, 0, realSize.x - usableSize.x, realSize.y - usableSize.y)
            Surface.ROTATION_270 -> Insets.of(realSize.x - usableSize.x, 0, 0, realSize.y - usableSize.y)
            else -> Insets.of(0, 0, 0, realSize.y - usableSize.y)
        }
    }
}

data class ScreenInfo(
    val width: Int,
    val height: Int,
    val insets: Insets,
)
