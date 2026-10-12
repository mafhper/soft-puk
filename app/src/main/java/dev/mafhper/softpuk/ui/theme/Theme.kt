package dev.mafhper.softpuk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SoftPukColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Ink,
    secondary = NeonCyan,
    onSecondary = Ink,
    background = Ink,
    onBackground = OnInk,
    surface = InkSurface,
    onSurface = OnInk,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = OnInkMuted,
    outline = NeonGreenDim,
    error = AlertRed,
    onError = Ink,
)

@Composable
fun SoftPukTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoftPukColors,
        typography = SoftPukTypography,
        content = content,
    )
}
