package com.drawtaxi.app.ui.theme

import androidx.compose.ui.graphics.Color

data class DrawTaxiColors(
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color, val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    val error: Color, val onError: Color, val errorContainer: Color, val onErrorContainer: Color,
    val inverseSurface: Color, val inverseOnSurface: Color, val inversePrimary: Color,
    val statusPending: Color, val statusPendingBg: Color,
    val statusValidated: Color, val statusValidatedBg: Color,
    val statusCancelled: Color, val statusCancelledBg: Color,
    val brandColor: Color
) {
    val profitabilityExcellent: Color get() = Emerald500
    val profitabilityExcellentBg: Color get() = Emerald100
    val profitabilityExcellentText: Color get() = Green800
    val profitabilityGood: Color get() = Amber600
    val profitabilityGoodBg: Color get() = Amber100
    val profitabilityGoodText: Color get() = Amber700
    val profitabilityWeak: Color get() = Orange500
    val profitabilityWeakBg: Color get() = Amber50
    val profitabilityWeakText: Color get() = Orange600
    val profitabilityBad: Color get() = Rose500
    val profitabilityBadBg: Color get() = Rose100
    val profitabilityBadText: Color get() = Rose700

    fun profitabilityColor(percent: Double): Color = when {
        percent >= 70 -> profitabilityExcellent; percent >= 50 -> profitabilityGood; percent >= 30 -> profitabilityWeak; else -> profitabilityBad
    }
    fun profitabilityBgColor(percent: Double): Color = when {
        percent >= 70 -> profitabilityExcellentBg; percent >= 50 -> profitabilityGoodBg; percent >= 30 -> profitabilityWeakBg; else -> profitabilityBadBg
    }
    fun profitabilityTextColor(percent: Double): Color = when {
        percent >= 70 -> profitabilityExcellentText; percent >= 50 -> profitabilityGoodText; percent >= 30 -> profitabilityWeakText; else -> profitabilityBadText
    }
}

fun lightDrawTaxiColors(brandColor: Color): DrawTaxiColors = DrawTaxiColors(
    primary = brandColor, onPrimary = Color.White, primaryContainer = brandColor.copy(alpha = 0.1f), onPrimaryContainer = brandColor,
    secondary = Slate500, onSecondary = Color.White, secondaryContainer = Slate100, onSecondaryContainer = Slate700,
    tertiary = Emerald600, onTertiary = Color.White, tertiaryContainer = Emerald100, onTertiaryContainer = Green800,
    background = Slate50, onBackground = Slate900,
    surface = SurfaceWhite, onSurface = Slate900, surfaceVariant = Slate100, onSurfaceVariant = Slate600,
    outline = Slate200, outlineVariant = Slate300,
    error = Rose500, onError = Color.White, errorContainer = Rose100, onErrorContainer = Rose700,
    inverseSurface = Slate800, inverseOnSurface = Slate100, inversePrimary = brandColor.copy(alpha = 0.8f),
    statusPending = Amber500, statusPendingBg = Amber100, statusValidated = Emerald500, statusValidatedBg = Emerald100, statusCancelled = Rose500, statusCancelledBg = Rose100,
    brandColor = brandColor
)

fun darkDrawTaxiColors(brandColor: Color): DrawTaxiColors = DrawTaxiColors(
    primary = brandColor, onPrimary = Color.White, primaryContainer = brandColor.copy(alpha = 0.3f), onPrimaryContainer = Color.White,
    secondary = Slate400, onSecondary = Slate900, secondaryContainer = Slate700, onSecondaryContainer = Slate200,
    tertiary = Emerald500, onTertiary = Color.White, tertiaryContainer = Green800, onTertiaryContainer = Emerald100,
    background = Slate950, onBackground = Slate100,
    surface = SurfaceDark, onSurface = Slate100, surfaceVariant = Slate800, onSurfaceVariant = Slate300,
    outline = Slate600, outlineVariant = Slate700,
    error = Rose500, onError = Color.White, errorContainer = Rose700, onErrorContainer = Rose100,
    inverseSurface = Slate200, inverseOnSurface = Slate900, inversePrimary = brandColor,
    statusPending = Amber500, statusPendingBg = Amber700, statusValidated = Emerald500, statusValidatedBg = Green800, statusCancelled = Rose500, statusCancelledBg = Rose700,
    brandColor = brandColor
)
