package com.guillermonegrete.tts.common.compose

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

@Composable
fun isDesktopOrTabletSize()
    = currentWindowAdaptiveInfo().windowSizeClass.isAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND, WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
