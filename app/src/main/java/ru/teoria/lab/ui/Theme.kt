package ru.teoria.lab.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF101219)
val Surface1 = Color(0xFF1A1D27)
val Surface2 = Color(0xFF262A36)
val Accent = Color(0xFFF2C14E)
val Blue = Color(0xFF7FB2FF)
val Green = Color(0xFF7ED7A7)
val Red = Color(0xFFFF7B72)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF231A00),
    secondary = Blue,
    onSecondary = Color(0xFF06172E),
    tertiary = Green,
    onTertiary = Color(0xFF06281B),
    background = Bg,
    onBackground = Color(0xFFECEDF2),
    surface = Surface1,
    onSurface = Color(0xFFECEDF2),
    surfaceVariant = Surface2,
    onSurfaceVariant = Color(0xFFB9BECE),
    error = Red
)

@Composable
fun TeoriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
