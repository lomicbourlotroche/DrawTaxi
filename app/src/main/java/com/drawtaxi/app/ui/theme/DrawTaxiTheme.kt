package com.drawtaxi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDrawTaxiColors = staticCompositionLocalOf { lightDrawTaxiColors(Indigo500) }
val LocalDrawTaxiType = staticCompositionLocalOf { DrawTaxiType() }

@Composable
fun drawTaxiColors(): DrawTaxiColors = LocalDrawTaxiColors.current

@Composable
fun drawTaxiType(): DrawTaxiType = LocalDrawTaxiType.current

@Composable
fun DrawTaxiTheme(
    brandColor: Color = Indigo500,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkDrawTaxiColors(brandColor) else lightDrawTaxiColors(brandColor)
    val typography = DrawTaxiType()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                window.statusBarColor = if (darkTheme) Slate950.toArgb() else brandColor.toArgb()
            }
        }
    }
    CompositionLocalProvider(
        LocalDrawTaxiColors provides colorScheme,
        LocalDrawTaxiType provides typography,
        content = content
    )
}
