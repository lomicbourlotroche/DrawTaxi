package com.drawtaxi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo600,
    
    secondary = Slate500,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate700,
    
    tertiary = Emerald500,
    onTertiary = Color.White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Green800,
    
    background = Slate50,
    onBackground = Slate900,
    
    surface = SurfaceWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    
    outline = Slate200,
    outlineVariant = Slate300,
    
    error = Rose500,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red800,
    
    inverseSurface = Slate800,
    inverseOnSurface = Slate100,
    inversePrimary = Indigo500
)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo600,
    onPrimaryContainer = Indigo100,
    
    secondary = Slate400,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate200,
    
    tertiary = Emerald500,
    onTertiary = Color.White,
    tertiaryContainer = Green800,
    onTertiaryContainer = Emerald100,
    
    background = Slate950,
    onBackground = Slate100,
    
    surface = SurfaceDark,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    
    outline = Slate600,
    outlineVariant = Slate700,
    
    error = Rose500,
    onError = Color.White,
    errorContainer = Red800,
    onErrorContainer = Red100,
    
    inverseSurface = Slate200,
    inverseOnSurface = Slate900,
    inversePrimary = Indigo500
)

@Composable
fun DrawTaxiTheme(
    brandColor: Color = Indigo500,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = brandColor,
            onPrimary = Color.White,
            primaryContainer = brandColor.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            
            secondary = Slate400,
            onSecondary = Slate900,
            secondaryContainer = Slate700,
            onSecondaryContainer = Slate200,
            
            tertiary = Emerald500,
            onTertiary = Color.White,
            tertiaryContainer = Green800,
            onTertiaryContainer = Emerald100,
            
            background = Slate950,
            onBackground = Slate100,
            
            surface = SurfaceDark,
            onSurface = Slate100,
            surfaceVariant = Slate800,
            onSurfaceVariant = Slate300,
            
            outline = Slate600,
            outlineVariant = Slate700,
            
            error = Rose500,
            onError = Color.White,
            errorContainer = Red800,
            onErrorContainer = Red100,
            
            inverseSurface = Slate200,
            inverseOnSurface = Slate900,
            inversePrimary = brandColor
        )
    } else {
        lightColorScheme(
            primary = brandColor,
            onPrimary = Color.White,
            primaryContainer = brandColor.copy(alpha = 0.1f),
            onPrimaryContainer = brandColor,
            
            secondary = Slate500,
            onSecondary = Color.White,
            secondaryContainer = Slate100,
            onSecondaryContainer = Slate700,
            
            tertiary = Emerald600,
            onTertiary = Color.White,
            tertiaryContainer = Emerald100,
            onTertiaryContainer = Green800,
            
            background = Slate50,
            onBackground = Slate900,
            
            surface = SurfaceWhite,
            onSurface = Slate900,
            surfaceVariant = Slate100,
            onSurfaceVariant = Slate600,
            
            outline = Slate200,
            outlineVariant = Slate300,
            
            error = Rose500,
            onError = Color.White,
            errorContainer = Red100,
            onErrorContainer = Red800,
            
            inverseSurface = Slate800,
            inverseOnSurface = Slate100,
            inversePrimary = brandColor.copy(alpha = 0.8f)
        )
    }
    
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
