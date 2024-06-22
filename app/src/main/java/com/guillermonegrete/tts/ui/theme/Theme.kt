package com.guillermonegrete.tts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.guillermonegrete.tts.ui.BrightnessTheme

private val DarkColorPalette = darkColors(
    primary = GreenLight,
    primaryVariant = GreenDark,
    secondary = BlueLight,
    secondaryVariant = BlueDark,
)

private val LightColorPalette = lightColors(
    primary = GreenLight,
    primaryVariant = GreenDark,
    secondary = BlueLight,
    secondaryVariant = BlueDark,

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

private val BeigeColorPalette = lightColors(
    primary = GreenLight,
    primaryVariant = GreenDark,
    secondary = BlueLight,
    secondaryVariant = BlueDark,

    background = Color(0Xffffedbf),
    surface = Color(0Xffffedbf),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
){
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}

@Composable
fun VisualizerTheme(
    theme: BrightnessTheme,
    content: @Composable () -> Unit
) {
    val colors = when(theme) {
        BrightnessTheme.WHITE -> LightColorPalette
        BrightnessTheme.BEIGE -> BeigeColorPalette
        BrightnessTheme.BLACK -> DarkColorPalette
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}
