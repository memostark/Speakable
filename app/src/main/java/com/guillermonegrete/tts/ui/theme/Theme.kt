package com.guillermonegrete.tts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.guillermonegrete.tts.ui.BrightnessTheme

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFF81d5cc),
    onPrimary = Color(0xFF003733),
    secondary = GreenLight,
    tertiary = BlueLight,
)

private val LightColorPalette = lightColorScheme(
    primary = GreenLight,
    secondary = GreenDark,
    tertiary = BlueLight,

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

private val BeigeColorPalette = lightColorScheme(
    primary = GreenLight,
    secondary = GreenDark,
    tertiary = BlueLight,

    background = Beige,
    surface = Beige,
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
        colorScheme = colors,
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
        colorScheme = colors,
        content = content
    )
}
