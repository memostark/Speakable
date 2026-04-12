package com.guillermonegrete.tts.common.compose

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun isDesktopOrTabletSize(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
            && (windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.EXPANDED || windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.MEDIUM)
}