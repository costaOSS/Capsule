package dev.capsule.ui.theme

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

val TerminalGreen = Color(0xFF00FF41)
val TerminalGreenDark = Color(0xFF00CC33)
val TerminalBackground = Color(0xFF0D1117)
val TerminalBackgroundLight = Color(0xFFF0F6FC)
val SurfaceDark = Color(0xFF161B22)
val SurfaceLight = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TerminalBackground,
    primaryContainer = TerminalGreenDark,
    secondary = Color(0xFF58A6FF),
    background = TerminalBackground,
    surface = SurfaceDark,
    onBackground = Color(0xFFC9D1D9),
    onSurface = Color(0xFFC9D1D9)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF22863A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF85E89D),
    secondary = Color(0xFF0366D6),
    background = TerminalBackgroundLight,
    surface = SurfaceLight,
    onBackground = Color(0xFF24292E),
    onSurface = Color(0xFF24292E)
)

@Composable
fun CapsuleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}